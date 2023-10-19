import type { BoundsMap, Timeline } from '../timeline.js';
import {
  applyOperation,
  bound,
  coalesce,
  EagerTimeline,
  identityBoundsMap,
  isLazy,
  LazyTimeline,
  merge,
  sortSegments,
  evaluate
} from '../timeline.js';
import { Segment } from '../segment.js';
import { IntervalLike, Inclusivity, Interval } from '../interval.js';
import { ProfileSpecialization, ProfileType } from '../profiles/profile-type.js';
import { LinearEquation, Real, map2Arrays, Windows, Profile, ActivityInstance, fetcher } from '../internal.js';
import { BinaryOperation } from '../binary-operation.js';
import { Temporal } from '@js-temporal/polyfill';
import { AnyActivityType, ActivityTypeName } from '../dynamic/activity-type.js';
import { shiftEdgesBoundsMap } from '../bounds-utils.js';

export class Spans<S extends IntervalLike> {
  private spans: Timeline<S>;

  constructor(spans: Timeline<S>) {
    this.spans = spans;
  }

  public static ActivityInstances<A extends ActivityTypeName = typeof AnyActivityType>(
    type?: A
  ): Spans<ActivityInstance<A>> {
    if (type === undefined) return new Spans(fetcher.allActivityInstances());
    else return new Spans(fetcher.activityInstanceByType(type));
  }

  public async evaluate(bounds: Interval): Promise<this> {
    if (!isLazy(this.spans)) throw new Error('Spans has already been evaluated');
    this.spans = await evaluate(this.spans as LazyTimeline<S>, bounds);
    return this;
  }

  public collect(): S[];
  public collect(bounds: Interval): Promise<S[]>;
  public collect(bounds?: Interval): S[] | Promise<S[]> {
    if (isLazy(this.spans)) {
      if (bounds === undefined)
        throw new Error('Collect must be provided a bounds argument if the profile has not been evaluated yet.');
      return this.evaluate(bounds).then(p => (p.spans as EagerTimeline<S>).array as S[]);
    } else {
      return (this.spans as EagerTimeline<S>).array;
    }
  }

  public inspect(f: (spans: readonly S[], bounds: Interval) => void) {
    const inspectOp = ({ current: bounds }: { current: Interval }, [$]: S[][]) => {
      f($, bounds);
      return $;
    };
    this.spans = applyOperation(inspectOp, identityBoundsMap, this.spans);
    return this;
  }

  public add<T extends IntervalLike>(span: T): Spans<S | T> {
    const timeline = merge(this.spans, bound([span]));
    return new Spans(timeline);
  }

  public addAll<T extends IntervalLike>(other: Spans<T>): Spans<S | T> {
    const timeline = merge(this.spans, other.spans);
    return new Spans(timeline);
  }

  public mapToSegments<T>(f: (span: S) => T, boundsMap: BoundsMap): Spans<Segment<T>> {
    const mapToSegmentsOp = (_: any, [$]: S[][]) =>
      $.map(s => {
        let value = f(s);
        return new Segment(value, s.interval);
      });
    const timeline = applyOperation(mapToSegmentsOp, boundsMap, this.spans);
    return new Spans<Segment<T>>(timeline);
  }

  public filter(predicate: (s: S) => boolean): Spans<S> {
    const filterOp = (_: any, [$]: S[][]) => $.filter(predicate);
    const timeline = applyOperation(filterOp, identityBoundsMap, this.spans);
    return new Spans(timeline);
  }

  public flattenIntoProfile<Result>(map: (v: S) => Result, profileType: ProfileType): ProfileSpecialization<Result> {
    const flattenIntoProfileOp = (_: any, [$]: S[][]) => {
      const result = $.map(s => new Segment(map(s), s.interval));
      sortSegments(result, profileType);
      coalesce(result, profileType);
      return result;
    };
    const timeline = applyOperation(flattenIntoProfileOp, identityBoundsMap, this.spans);
    return new Profile(timeline, profileType).specialize();
  }

  public combineIntoProfile<Result>(
    op: BinaryOperation<S, Result, Result>,
    profileType: ProfileType
  ): ProfileSpecialization<Result> {
    const combineIntoProfileOp = ({ current: bounds }: { current: Interval }, [$]: S[][]) => {
      let acc: Segment<Result>[] = [];
      const remaining = $;
      while (remaining.length > 0) {
        const batch: Segment<S>[] = [];
        let previousTime = bounds.start;
        let previousInclusivity = Inclusivity.opposite(bounds.startInclusivity);
        for (const span of remaining) {
          const startComparison = Temporal.Duration.compare(span.interval.start, previousTime);
          if (
            startComparison > 0 ||
            (startComparison === 0 && previousInclusivity !== span.interval.startInclusivity)
          ) {
            batch.push(new Segment(span, span.interval));
            previousTime = span.interval.end;
            previousInclusivity = span.interval.endInclusivity;
          }
        }
        acc = map2Arrays(batch, acc, op);
      }
      return coalesce(acc, profileType);
    };
    const timeline = applyOperation(combineIntoProfileOp, identityBoundsMap, this.spans);
    return new Profile(timeline, profileType).specialize();
  }

  public intoWindows(): Windows {
    return this.flattenIntoProfile(() => true, ProfileType.Windows).assignGaps(false);
  }

  public shiftBy(shiftRising: Temporal.Duration, shiftFalling?: Temporal.Duration): Spans<S> {
    if (shiftFalling === undefined) shiftFalling = shiftRising;
    return this.unsafe.map(
      s => s.mapInterval(i => i.interval.shiftBy(shiftRising, shiftFalling)),
      shiftEdgesBoundsMap(shiftRising, shiftFalling)
    );
  }

  public countActive(): Real {
    return this.combineIntoProfile<LinearEquation>(
      BinaryOperation.cases(
        () => LinearEquation.Constant(1),
        r => r,
        (_, r) => r.plus(1)
      ),
      ProfileType.Real
    ).assignGaps(0);
  }

  public accumulatedDuration(unit: Temporal.Duration): Real {
    return this.countActive().integrate(unit);
  }

  public starts(): Spans<S> {
    const startsOp = ({ current: bounds }: { current: Interval }, [$]: S[][]) => {
      const spans = $.map(i => i.mapInterval(s => Interval.At(s.interval.start)));
      return spans.filter(s => bounds.contains(s.interval));
    };
    const timeline = applyOperation(startsOp, identityBoundsMap, this.spans);
    return new Spans(timeline);
  }

  public ends(): Spans<S> {
    const endsOp = ({ current: bounds }: { current: Interval }, [$]: S[][]) => {
      const spans = $.map(i => i.mapInterval(s => Interval.At(s.interval.end)));
      return spans.filter(s => bounds.contains(s.interval));
    };
    const timeline = applyOperation(endsOp, identityBoundsMap, this.spans);
    return new Spans(timeline);
  }

  public split(
    numberOfSubSpans: number,
    internalStartInclusivity: Inclusivity,
    internalEndInclusivity: Inclusivity,
    strict: boolean = true
  ): Spans<S> {
    if (numberOfSubSpans === 1) return this;
    const splitOp = ({ current: bounds }: { current: Interval }, [$]: S[][]) => {
      return $.flatMap(s => {
        const i = s.interval;

        const fullWidth = i.duration().total('microsecond');
        const subWidth = Math.floor(fullWidth / numberOfSubSpans);

        if (i.isSingleton()) throw new Error('Cannot split an instantaneous span into sub-spans.');
        else if (subWidth === 0)
          throw new Error(
            `Cannot split a span only ${subWidth} microseconds long in to ${numberOfSubSpans} sub-spans.`
          );

        if (strict) {
          if (Interval.compareStarts(i, bounds) === 0)
            throw new Error(
              'Cannot split a span that starts at or before the bounds start. Consider setting the `strict` arg to `false`.'
            );
          if (Interval.compareEnds(i, bounds) === 0)
            throw new Error(
              'Cannot split a span that ends at or before the bounds end. Consider setting the `strict` arg to `false`.'
            );
        }

        let acc = i.start.add({ microseconds: subWidth });
        let result: S[] = [];
        result.push(s.mapInterval(() => Interval.Between(i.start, acc, i.startInclusivity, internalEndInclusivity)));
        for (let index = 0; index < numberOfSubSpans - 1; index++) {
          let nextAcc = acc.add({ microseconds: subWidth });
          result.push(
            s.mapInterval(() => Interval.Between(acc, nextAcc, internalStartInclusivity, internalEndInclusivity))
          );
          acc = nextAcc;
        }
        result.push(s.mapInterval(() => Interval.Between(acc, i.end, internalStartInclusivity, i.endInclusivity)));

        return result;
      });
    };
    const timeline = applyOperation(splitOp, identityBoundsMap, this.spans);
    return new Spans(timeline);
  }

  // @ts-ignore
  public unsafe = new (class {
    constructor(public outerThis: Spans<S>) {}

    public map<T extends IntervalLike>(f: (span: S) => T, boundsMap: BoundsMap): Spans<T> {
      const mapOp = (_: any, [$]: S[][]) => $.map(s => f(s));
      const timeline = applyOperation(mapOp, boundsMap, this.outerThis.spans);
      return new Spans<T>(timeline);
    }

    public flatMap<T extends IntervalLike>(f: (span: S) => T[], boundsMap: BoundsMap): Spans<T> {
      const flatMapOp = (_: any, [$]: S[][]) => $.flatMap(s => f(s));
      const timeline = applyOperation(flatMapOp, boundsMap, this.outerThis.spans);
      return new Spans<T>(timeline);
    }
  })(this);
}
