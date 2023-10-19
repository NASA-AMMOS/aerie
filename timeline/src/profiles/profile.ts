import { Segment } from '../segment.js';
import { Inclusivity, Interval } from '../interval.js';
import {
  applyOperation,
  BoundsMap,
  EagerTimeline,
  evaluate,
  identityBoundsMap,
  isLazy,
  LazyTimeline,
  ProfileSpecialization,
  ProfileType,
  Spans,
  truncate,
  Windows
} from '../internal.js';
import { bound, coalesce, Timeline } from '../timeline.js';
import { BinaryOperation } from '../binary-operation.js';
import { fetcher } from '../data-fetcher.js';
import { Temporal } from '@js-temporal/polyfill';

export class Profile<V> {
  protected segments: Timeline<Segment<V>>;
  private readonly typeTag: ProfileType;

  constructor(segments: Timeline<Segment<V>>, typeTag: ProfileType) {
    this.segments = segments;
    this.typeTag = typeTag;
  }

  public static Empty<V>(): Profile<V> {
    return new Profile(async _ => [], ProfileType.Other);
  }

  public static Value<V>(value: V, interval?: Interval): Profile<V> {
    return new Profile<V>(
      async bounds => [new Segment(value, interval === undefined ? bounds : Interval.intersect(bounds, interval))],
      ProfileType.Other
    );
  }

  public static Resource<V>(name: string): Profile<V> {
    return new Profile<V>(
      fetcher.resource(name, $ => $ as V, ProfileType.Other),
      ProfileType.Other
    );
  }

  public timeline = () => this.segments;

  public async evaluate(bounds: Interval): Promise<this> {
    if (!isLazy(this.segments))
      throw new Error(
        `Profile has already been evaluated on bounds ${(this.segments as EagerTimeline<Segment<V>>).bounds}`
      );
    this.segments = await evaluate(this.segments as LazyTimeline<Segment<V>>, bounds);
    return this;
  }

  public async collect(bounds?: Interval): Promise<Segment<V>[]> {
    if (isLazy(this.segments)) {
      if (bounds === undefined)
        throw new Error('Collect must be provided a bounds argument if the profile has not been evaluated yet.');
      await this.evaluate(bounds);
      return (this.segments as EagerTimeline<Segment<V>>).array;
    } else {
      const s = this.segments as EagerTimeline<Segment<V>>;
      if (bounds !== undefined && !Interval.equals(bounds, s.bounds))
        throw new Error(
          `Profile has already been collected on different bounds. Requested: ${bounds}, previous: ${s.bounds}.`
        );
      return s.array;
    }
  }

  public async valueAt(time: Temporal.Duration): Promise<any | undefined> {
    const segment = await this.collect(Interval.At(time));
    if (segment.length > 0) throw new Error('multiple segments exist at the same time');
    if (segment.length === 0) return undefined;
    return segment[0].value;
  }

  public inspect(f: (segments: readonly Segment<V>[], bounds: Interval) => void): this {
    const inspectOp = ({ current: bounds }: { current: Interval }, [$]: Segment<V>[][]) => {
      f($, bounds);
      return $;
    };
    this.segments = applyOperation(inspectOp, identityBoundsMap, this.segments);
    return this;
  }

  public set(value: V, interval: Interval): ProfileSpecialization<V>;
  public set(newProfile: Profile<V>): ProfileSpecialization<V>;
  public set(valueOrProfile: Profile<V> | V, interval?: Interval): ProfileSpecialization<V> {
    let profile: Profile<V>;
    if (interval !== undefined)
      profile = new Profile<V>(bound([new Segment(valueOrProfile as V, interval)]), this.typeTag);
    else profile = valueOrProfile as Profile<V>;
    return this.map2Values(
      profile,
      BinaryOperation.combineOrIdentity((_, r) => r),
      this.typeTag
    );
  }

  public assignGaps(def: Profile<V> | V): ProfileSpecialization<V> {
    if (def! instanceof Profile) def = Profile.Value(def as V);
    return (def as Profile<V>).set(this);
  }

  public unset(unsetInterval: Interval): ProfileSpecialization<V> {
    const unsetOp = (_: any, [$]: Segment<V>[][]) =>
      $.flatMap((seg: Segment<V>) => {
        const currentInterval = seg.interval;
        const currentValue = seg.value;
        return Interval.subtract(currentInterval, unsetInterval).map(i => new Segment(currentValue, i));
      });
    const timeline = applyOperation(unsetOp, identityBoundsMap, this.segments);
    return new Profile<V>(timeline, this.typeTag).specialize();
  }

  public mapValues(f: (v: V, i: Interval) => V): ProfileSpecialization<V>;
  public mapValues<W>(f: (v: V, i: Interval) => W, typeTag: ProfileType): ProfileSpecialization<W>;
  public mapValues<W>(f: (v: V, i: Interval) => W, typeTag?: ProfileType): ProfileSpecialization<W> {
    return this.unsafe.map<W>(
      (v, i) => new Segment(f(v, i), i),
      identityBoundsMap,
      typeTag !== undefined ? typeTag : this.typeTag
    );
  }

  public map2Values<W>(rightProfile: Profile<W>, op: BinaryOperation<V, W, V>): ProfileSpecialization<V>;
  public map2Values<W, Result>(
    rightProfile: Profile<W>,
    op: BinaryOperation<V, W, Result>,
    typeTag: ProfileType
  ): ProfileSpecialization<Result>;
  public map2Values<W, Result>(
    rightProfile: Profile<W>,
    op: BinaryOperation<V, W, Result>,
    typeTag?: ProfileType
  ): ProfileSpecialization<Result> {
    if (typeTag === undefined) typeTag = this.typeTag;
    const timeline = applyOperation(
      (_, [l, r]) => map2Arrays(l, r, op),
      identityBoundsMap,
      this.segments,
      rightProfile.segments
    );

    return new Profile(timeline, typeTag).specialize();
  }

  public filter(predicate: (v: V, i: Interval) => boolean): ProfileSpecialization<V> {
    const filterOp: (_: any, a: Segment<V>[][]) => Segment<V>[] = (_, [$]) =>
      $.filter((s: Segment<V>) => predicate(s.value, s.interval));
    const timeline = applyOperation<Segment<V>>(filterOp, identityBoundsMap, this.segments);
    return new Profile<V>(timeline, this.typeTag).specialize();
  }

  public equalTo(other: Profile<V>): Windows {
    return this.map2Values(
      other,
      BinaryOperation.combineOrUndefined((l, r) => l === r),
      ProfileType.Windows
    );
  }

  public notEqualTo(other: Profile<V>): Windows {
    return this.map2Values(
      other,
      BinaryOperation.combineOrUndefined((l, r) => l !== r),
      ProfileType.Windows
    );
  }

  public edges(edgeFilter: BinaryOperation<V, V, boolean>): Windows {
    const edgesOp = ({ current: bounds }: { current: Interval }, [arr]: Segment<V>[][]) => {
      let buffer: Segment<V> | undefined = undefined;
      return coalesce(
        arr
          .flatMap(currentSegment => {
            let leftEdge: boolean | undefined;
            let rightEdge: boolean | undefined;

            const previous = buffer;
            buffer = currentSegment;
            const currentInterval = currentSegment.interval;

            const leftEdgeInterval = Interval.At(currentInterval.start);
            const rightEdgeInterval = Interval.At(currentInterval.end);

            if (currentInterval.end === bounds.end && currentInterval.endInclusivity === bounds.endInclusivity) {
              if (bounds.includesEnd()) rightEdge = false;
              else rightEdge = undefined;
            } else {
              rightEdge = edgeFilter.left(currentSegment.value, rightEdgeInterval);
            }

            if (previous !== undefined) {
              if (Interval.compareEndToStart(previous.interval, currentInterval) === 0) {
                leftEdge = edgeFilter.combine(previous.value, currentSegment.value, leftEdgeInterval);
              } else {
                leftEdge = edgeFilter.right(currentSegment.value, leftEdgeInterval);
              }
            } else {
              if (
                currentInterval.start == bounds.start &&
                currentInterval.startInclusivity == bounds.startInclusivity
              ) {
                if (bounds.includesStart()) leftEdge = false;
                else leftEdge = undefined;
              } else {
                leftEdge = edgeFilter.right(currentSegment.value, leftEdgeInterval);
              }
            }

            return [
              new Segment(leftEdge, leftEdgeInterval).transpose(),
              new Segment(
                false,
                Interval.Between(currentInterval.start, currentInterval.end, Inclusivity.Exclusive)
              ).transpose(),
              new Segment(rightEdge, rightEdgeInterval).transpose()
            ];
          })
          .filter($ => $ !== undefined) as Segment<boolean>[],
        ProfileType.Windows
      );
    };
    const timeline = applyOperation(edgesOp, identityBoundsMap, this.segments);
    return new Windows(timeline);
  }

  public changes(): Windows {
    return this.edges(BinaryOperation.combineOrUndefined((l, r) => l !== r));
  }

  public transitions(from: V, to: V): Windows {
    return this.edges(
      BinaryOperation.cases(
        l => (l === from ? undefined : false),
        r => (r === to ? undefined : false),
        (l, r) => l === from && r === to
      )
    );
  }

  public shiftBy(shift: Temporal.Duration): ProfileSpecialization<V> {
    return this.unsafe.mapIntervals((_, i) => i.shiftBy(shift), {
      eager: b => b.shiftBy(shift),
      lazy: b => b.shiftBy(shift.negated())
    });
  }

  public select(selection: Interval): ProfileSpecialization<V> {
    const selectOp = (bounds: { current: Interval; next: Interval }, [$]: Segment<V>[][]) => {
      // Black magic: if the current and next bounds are unequal, we are in lazy mode.
      // The lazy branch of the bounds map below guaranteed that the input was already evaluated on the
      // selection interval. Thus lazy select is a no-op.
      if (!Interval.equals(bounds.current, bounds.next)) return $;
      else return truncate($, selection);
    };
    const selectBoundsMap: BoundsMap = {
      eager: $ => $,
      lazy: $ => {
        const intersection = Interval.intersect($, selection);

        // Evaluating an operation on inverted bounds is UB. Replace any empty bounds with the standard empty interval.
        if (intersection.isEmpty()) return Interval.Empty();
        else return intersection;
      }
    };
    const timeline = applyOperation(selectOp, selectBoundsMap, this.segments);
    return new Profile(timeline, this.typeTag).specialize();
  }

  public intoSpans(): Spans<Segment<V>> {
    return new Spans(this.segments);
  }

  public specialize(): ProfileSpecialization<V> {
    return ProfileType.specialize(this, this.typeTag);
  }

  public async any(predicate: (v: V, i: Interval) => boolean, bounds?: Interval): Promise<boolean> {
    const segments = await this.collect(bounds);
    return segments.some((s: Segment<V>) => predicate(s.value, s.interval));
  }

  public async all(predicate: (v: V, i: Interval) => boolean, bounds?: Interval): Promise<boolean> {
    const segments = await this.collect(bounds);
    return segments.every((s: Segment<V>) => predicate(s.value, s.interval));
  }

  public unsafe = new (class {
    constructor(public outerThis: Profile<V>) {}

    public map(f: (v: V, i: Interval) => Segment<V>, boundsMap: BoundsMap): ProfileSpecialization<V>;
    public map<W>(
      f: (v: V, i: Interval) => Segment<W>,
      boundsMap: BoundsMap,
      typeTag: ProfileType
    ): ProfileSpecialization<W>;
    public map<W>(
      f: (v: V, i: Interval) => Segment<W>,
      boundsMap: BoundsMap,
      typeTag?: ProfileType
    ): ProfileSpecialization<W> {
      if (typeTag === undefined) {
        typeTag = this.outerThis.typeTag;
      }
      const mapOp = (_: any, [$]: Segment<V>[][]) =>
        coalesce(
          $.map(s => f(s.value, s.interval)),
          typeTag!
        );
      const timeline = applyOperation(mapOp, boundsMap, this.outerThis.segments);
      return new Profile<W>(timeline, typeTag).specialize();
    }

    public mapIntervals(map: (v: V, i: Interval) => Interval, boundsMap: BoundsMap): ProfileSpecialization<V> {
      return this.map<V>((v, i) => new Segment<V>(v, map(v, i)), boundsMap, this.outerThis.typeTag);
    }

    public map2<W>(
      rightProfile: Profile<W>,
      op: BinaryOperation<V, W, Segment<V>>,
      boundsMap: BoundsMap
    ): ProfileSpecialization<V>;
    public map2<W, Result>(
      rightProfile: Profile<W>,
      op: BinaryOperation<V, W, Segment<Result>>,
      boundsMap: BoundsMap,
      typeTag: ProfileType
    ): ProfileSpecialization<Result>;
    public map2<W, Result>(
      rightProfile: Profile<W>,
      op: BinaryOperation<V, W, Segment<Result>>,
      boundsMap: BoundsMap,
      typeTag?: ProfileType
    ): ProfileSpecialization<Result> {
      if (typeTag === undefined) typeTag = this.outerThis.typeTag;
      const map2Op = (_: any, [l, r]: Segment<any>[][]) =>
        coalesce(
          map2Arrays(l, r, op).map($ => $.value),
          typeTag!
        );
      const timeline = applyOperation(map2Op, boundsMap, this.outerThis.segments, rightProfile.segments);

      return new Profile(timeline, typeTag).specialize();
    }

    public flatMap(f: (v: V, i: Interval) => Segment<V>[], boundsMap: BoundsMap): ProfileSpecialization<V>;
    public flatMap<W>(
      f: (v: V, i: Interval) => Segment<W>[],
      boundsMap: BoundsMap,
      typeTag: ProfileType
    ): ProfileSpecialization<W>;
    public flatMap<W>(
      f: (v: V, i: Interval) => Segment<W>[],
      boundsMap: BoundsMap,
      typeTag?: ProfileType
    ): ProfileSpecialization<W> {
      if (typeTag === undefined) typeTag = this.outerThis.typeTag;
      const flatMapOp = (_: any, [$]: Segment<V>[][]) =>
        coalesce(
          $.flatMap(s => f(s.value, s.interval)),
          typeTag!
        );
      const timeline = applyOperation(flatMapOp, boundsMap, this.outerThis.segments);
      return new Profile<W>(timeline, typeTag).specialize();
    }

    public flatMap2<W>(
      rightProfile: Profile<W>,
      op: BinaryOperation<V, W, Segment<V>[]>,
      boundsMap: BoundsMap
    ): ProfileSpecialization<V>;
    public flatMap2<W, Result>(
      rightProfile: Profile<W>,
      op: BinaryOperation<V, W, Segment<Result>[]>,
      boundsMap: BoundsMap,
      typeTag: ProfileType
    ): ProfileSpecialization<Result>;
    public flatMap2<W, Result>(
      rightProfile: Profile<W>,
      op: BinaryOperation<V, W, Segment<Result>[]>,
      boundsMap: BoundsMap,
      typeTag?: ProfileType
    ): ProfileSpecialization<Result> {
      if (typeTag === undefined) typeTag = this.outerThis.typeTag;
      const flatMap2Op = (_: any, [l, r]: Segment<any>[][]) =>
        coalesce(
          map2Arrays(l, r, op).flatMap($ => $.value),
          typeTag!
        );
      const timeline = applyOperation(flatMap2Op, boundsMap, this.outerThis.segments, rightProfile.segments);
      return new Profile(timeline, typeTag).specialize();
    }
  })(this);
}

export function map2Arrays<V, W, Result>(
  left: Segment<V>[],
  right: Segment<W>[],
  op: BinaryOperation<V, W, Result>
): Segment<Result>[] {
  const result: Segment<Result>[] = [];

  let leftIndex = 0;
  let rightIndex = 0;

  let leftSegment: Segment<V> | undefined = undefined;
  let rightSegment: Segment<W> | undefined = undefined;
  let remainingLeftSegment: Segment<V> | undefined = undefined;
  let remainingRightSegment: Segment<W> | undefined = undefined;

  while (
    leftIndex < left.length ||
    rightIndex < right.length ||
    remainingLeftSegment !== undefined ||
    remainingRightSegment !== undefined
  ) {
    if (remainingLeftSegment !== undefined) {
      leftSegment = remainingLeftSegment;
      remainingLeftSegment = undefined;
    } else if (leftIndex < left.length) {
      leftSegment = left[leftIndex++];
    } else {
      leftSegment = undefined;
    }
    if (remainingRightSegment !== undefined) {
      rightSegment = remainingRightSegment;
      remainingRightSegment = undefined;
    } else if (rightIndex < right.length) {
      rightSegment = right[rightIndex++];
    } else {
      rightSegment = undefined;
    }

    if (leftSegment === undefined) {
      const resultingSegment = rightSegment!.mapValue(s => op.right(s.value, s.interval)).transpose();
      if (resultingSegment !== undefined) result.push(resultingSegment);
    } else if (rightSegment === undefined) {
      const resultingSegment = leftSegment!.mapValue(s => op.left(s.value, s.interval)).transpose();
      if (resultingSegment !== undefined) result.push(resultingSegment);
    } else {
      const startComparison = Interval.compareStarts(leftSegment.interval, rightSegment.interval);
      if (startComparison === -1) {
        remainingRightSegment = rightSegment;
        const endComparison = Interval.compareEndToStart(leftSegment.interval, rightSegment.interval);
        if (endComparison < 1) {
          const resultingSegment = leftSegment.mapValue(s => op.left(s.value, s.interval)).transpose();
          if (resultingSegment !== undefined) result.push(resultingSegment);
        } else {
          remainingLeftSegment = leftSegment.mapInterval(s =>
            Interval.Between(
              rightSegment!.interval.start,
              s.interval.end,
              rightSegment!.interval.startInclusivity,
              s.interval.endInclusivity
            )
          );
          const resultingSegment = new Segment(
            leftSegment.value,
            Interval.Between(
              leftSegment.interval.start,
              rightSegment!.interval.start,
              leftSegment.interval.startInclusivity,
              Inclusivity.opposite(rightSegment!.interval.startInclusivity)
            )
          )
            .mapValue(s => op.left(s.value, s.interval))
            .transpose();
          if (resultingSegment !== undefined) result.push(resultingSegment);
        }
      } else if (startComparison === 1) {
        remainingLeftSegment = leftSegment;
        const endComparison = Interval.compareEndToStart(rightSegment.interval, leftSegment.interval);
        if (endComparison < 1) {
          const resultingSegment = rightSegment.mapValue(s => op.right(s.value, s.interval)).transpose();
          if (resultingSegment !== undefined) result.push(resultingSegment);
        } else {
          remainingRightSegment = rightSegment.mapInterval(s =>
            Interval.Between(
              leftSegment!.interval.start,
              s.interval.end,
              leftSegment!.interval.startInclusivity,
              s.interval.endInclusivity
            )
          );
          const resultingSegment = new Segment(
            rightSegment.value,
            Interval.Between(
              rightSegment.interval.start,
              leftSegment!.interval.start,
              rightSegment.interval.startInclusivity,
              Inclusivity.opposite(leftSegment!.interval.startInclusivity)
            )
          )
            .mapValue(s => op.right(s.value, s.interval))
            .transpose();
          if (resultingSegment !== undefined) result.push(resultingSegment);
        }
      } else {
        const endComparison = Interval.compareEnds(leftSegment.interval, rightSegment.interval);
        if (endComparison === -1) {
          remainingRightSegment = rightSegment.mapInterval(r =>
            Interval.Between(
              leftSegment!.interval.end,
              r.interval.end,
              Inclusivity.opposite(leftSegment!.interval.endInclusivity),
              r.interval.endInclusivity
            )
          );
          const resultingSegment = leftSegment
            .mapValue(l => op.combine(l.value, rightSegment!.value, l.interval))
            .transpose();
          if (resultingSegment !== undefined) result.push(resultingSegment);
        } else if (endComparison === 1) {
          remainingLeftSegment = leftSegment.mapInterval(l =>
            Interval.Between(
              rightSegment!.interval.end,
              l.interval.end,
              Inclusivity.opposite(rightSegment!.interval.endInclusivity),
              l.interval.endInclusivity
            )
          );
          const resultingSegment = rightSegment
            .mapValue(r => op.combine(leftSegment!.value, r.value, r.interval))
            .transpose();
          if (resultingSegment !== undefined) result.push(resultingSegment);
        } else {
          const resultingSegment = leftSegment
            .mapValue(l => op.combine(l.value, rightSegment!.value, l.interval))
            .transpose();
          if (resultingSegment !== undefined) result.push(resultingSegment);
        }
      }
    }
  }

  return result;
}
