import {Segment} from "./Segment";
import {Inclusivity, Interval} from "./interval";
import {Windows} from "./windows";
import {
  CachedAsyncGenerator,
  cacheGenerator,
  collect,
  GeneratorType,
  getGeneratorType,
  preparePlainGenerator,
  UncachedAsyncGenerator
} from "./generators";
import {BinaryOperation, OpMode} from "./BinaryOperation";

export enum ProfileType {
  Real,
  Discrete,
  Windows,
  Other
}

export class Profile<V> {
  private segments: UncachedAsyncGenerator<Segment<V>> | CachedAsyncGenerator<Segment<V>>;
  private readonly type_tag: ProfileType;

  constructor(segments: AsyncGenerator<Segment<V>>, type_tag: ProfileType) {
    this.segments = preparePlainGenerator(segments);
    this.type_tag = type_tag;
  }

  public static from<V>(data: Segment<V>[], type_tag: ProfileType): ProfileSpecialization<V>;
  public static from<V>(data: Segment<V>, type_tag: ProfileType): ProfileSpecialization<V>;
  public static from<V>(data: V, type_tag: ProfileType): ProfileSpecialization<V>;
  public static from<V>(data: any, type_tag: ProfileType): ProfileSpecialization<V> {
    let segments: AsyncGenerator<Segment<V>>;
    if (Array.isArray(data)) {
      segments = (async function* () {
        for (const segment of data) {
          yield segment;
        }
      })();
    } else if (data instanceof Segment) {
      segments = (async function* () {
        yield data;
      })();
    } else {
      segments = (async function* () {
        yield new Segment(Interval.Forever, data);
      })();
    }
    return (new Profile(segments, type_tag)).specialize();
  }

  public map(f: (v: Segment<V>) => Segment<V>): ProfileSpecialization<V>;
  public map<W>(f: (v: Segment<V>) => Segment<W>, type_tag: ProfileType): ProfileSpecialization<W>;
  public map<W>(f: (v: Segment<V>) => Segment<W>, type_tag?: ProfileType): ProfileSpecialization<W> {
    let outer_segments = this.segments;
    if (type_tag === undefined) {
      type_tag = this.type_tag;
    }
    let result = new Profile<W>((async function*()  {
      for await (const s of outer_segments) {
        yield f(s);
      }
    })(), type_tag);

    return result.specialize();
  }

  public map_values(f: (v: Segment<V>) => V): ProfileSpecialization<V>;
  public map_values<W>(f: (v: Segment<V>) => W, type_tag: ProfileType): ProfileSpecialization<W>;
  public map_values<W>(f: (v: Segment<V>) => W, type_tag?: ProfileType): ProfileSpecialization<W> {
    return this.map<W>(
        s => new Segment(s.interval, f(s)),
        type_tag !== undefined ? type_tag : this.type_tag
    );
  }

  public map_intervals(f: (s: Segment<V>) => Interval): ProfileSpecialization<V> {
    return this.map<V>(
        s => new Segment<V>(f(s), s.value),
        this.type_tag
    );
  }

  public static map2Values<Left, Right, Result>(
      leftProfile: Profile<Left> ,
      rightProfile: Profile<Right>,
      op: BinaryOperation<Left, Right, Result>,
      typeTag: ProfileType
  ): ProfileSpecialization<Result> {
    const segments = (async function* () {
      let leftIterator = leftProfile.segments;
      let rightIterator = rightProfile.segments;

      let remainingLeftSegment: Segment<Left> | undefined = undefined;
      let remainingRightSegment: Segment<Right> | undefined = undefined;

      let left: Segment<Left> | undefined;
      let right: Segment<Right> | undefined;

      while (true) {
        if (remainingLeftSegment !== undefined) {
          left = remainingLeftSegment;
          remainingLeftSegment = undefined;
        } else {
          let nextLeftResult = await leftIterator.next();
          if (!nextLeftResult.done) left = nextLeftResult.value;
          else left = undefined;
        }
        if (remainingRightSegment !== undefined) {
          right = remainingRightSegment;
          remainingRightSegment = undefined;
        } else {
          let nextRightResult = await rightIterator.next();
          if (!nextRightResult.done) right = nextRightResult.value;
          else right = undefined;
        }

        if (left === undefined) {
          if (right === undefined) break;
          let newSegment = right.mapValue(op.right).transpose();
          if (newSegment !== undefined) yield newSegment;
        } else if (right === undefined) {
          let newSegment = left.mapValue(op.left).transpose();
          if (newSegment !== undefined) yield newSegment;
        } else {
          let leftInterval = left.interval;
          let rightInterval = right.interval;

          let opMode: OpMode;

          let comparison = Temporal.Duration.compare(leftInterval.start, rightInterval.start);
          if (comparison < 0) opMode = OpMode.Left;
          else if (comparison > 0) opMode = OpMode.Right;
          else {
            comparison = Inclusivity.compareRestrictiveness(leftInterval.startInclusivity, rightInterval.startInclusivity);
            if (comparison < 0) opMode = OpMode.Left;
            else if (comparison > 0) opMode = OpMode.Right;
            else opMode = OpMode.Combine;
          }

          let intersection = Interval.intersect(leftInterval, rightInterval);

          switch (opMode) {
            case OpMode.Left: {
              remainingRightSegment = right;
              if (!intersection.isEmpty()) {
                remainingLeftSegment =
                    left.mapInterval($ => Interval.between(
                        intersection.start,
                        $.end,
                        intersection.startInclusivity,
                        $.endInclusivity));
                let newSegment = left.mapInterval($ => Interval.between(
                    $.start,
                    intersection.start,
                    $.startInclusivity,
                    Inclusivity.opposite(intersection.startInclusivity)
                )).mapValue(op.left).transpose();
                if (newSegment !== undefined) yield newSegment;
              } else {
                let newSegment = left.mapValue(op.left).transpose();
                if (newSegment !== undefined) yield newSegment;
              }
              break;
            }
            case OpMode.Right: {
              remainingLeftSegment = left;
              if (!intersection.isEmpty()) {
                remainingRightSegment =
                    right.mapInterval($ => Interval.between(
                        intersection.start,
                        $.end,
                        intersection.startInclusivity,
                        $.endInclusivity));
                let newSegment = right.mapInterval($ => Interval.between(
                    $.start,
                    intersection.start,
                    $.startInclusivity,
                    Inclusivity.opposite(intersection.startInclusivity)
                )).mapValue(op.right).transpose();
                if (newSegment !== undefined) yield newSegment;
              } else {
                let newSegment = right.mapValue(op.right).transpose();
                if (newSegment !== undefined) yield newSegment;
              }
              break;
            }
            default: {
              if (Temporal.Duration.compare(leftInterval.end, intersection.end) > 0
                  || Inclusivity.compareRestrictiveness(intersection.endInclusivity, leftInterval.endInclusivity) > 0) {
                remainingLeftSegment =
                    left.mapInterval($ => Interval.between(
                        intersection.end,
                        $.end,
                        Inclusivity.opposite(intersection.endInclusivity),
                        $.endInclusivity));
              } else if (Temporal.Duration.compare(rightInterval.end, intersection.end) > 0
                  || Inclusivity.compareRestrictiveness(intersection.endInclusivity, rightInterval.endInclusivity) > 0) {
                remainingRightSegment =
                    right.mapInterval($ => Interval.between(
                        intersection.end,
                        $.end,
                        Inclusivity.opposite(intersection.endInclusivity),
                        $.endInclusivity));
              }

              let newSegment = new Segment(intersection, op.combine(left.value, right.value)).transpose();
              if (newSegment !== undefined) yield newSegment;
            }
          }
        }
      }
    })();
    return (new Profile(segments, typeTag)).specialize();
  }

  public filter(f: (s: Segment<V>) => boolean): ProfileSpecialization<V> {
    let outer_segments = this.segments;
    let result = new Profile<V>((async function*()  {
      for await (const s of outer_segments) {
        if (f(s)) {
          yield s;
        }
      }
    })(), this.type_tag);

    return result.specialize();
  }

  public clone(n: number): ProfileSpecialization<V> {
    if (getGeneratorType(this.segments) === GeneratorType.Uncached) {
      this.segments = cacheGenerator(this.segments as UncachedAsyncGenerator<Segment<V>>);
    }

    // @ts-ignore
    return (new Profile<V>(this.segments.clone(), this.type_tag)).specialize();
  }

  public async collect(): Promise<Segment<V>[]> {
    return await collect(this.segments);
  }

  public select(interval: Interval): ProfileSpecialization<V> {
    return this.specialize();
  }

  public specialize(): ProfileSpecialization<V> {
    if (this.type_tag === ProfileType.Windows) {
      // @ts-ignore
      return new Windows(this);
    } else if (this.type_tag === ProfileType.Real) {
      // @ts-ignore
      return new Real(this);
    }

    // @ts-ignore
    return this;
  }
}

type ProfileSpecialization<V> =
  V extends boolean ? Windows
  : Profile<V>;
