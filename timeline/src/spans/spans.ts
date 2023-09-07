import type { Timeline } from '../timeline.js';
import { bound, coalesce, merge, sortSegments } from '../timeline.js';
import { Segment } from '../segment.js';
import { IntervalLike, Inclusivity, Interval } from '../interval.js';
import {ProfileSpecialization, ProfileType} from '../profiles/profile-type.js';
import { LinearEquation, Real, map2Arrays, Windows, Profile} from '../internal.js';
import { BinaryOperation } from '../binary-operation.js';
import { Temporal } from '@js-temporal/polyfill';

export class Spans<S extends IntervalLike> {
  private spans: Timeline<S>;

  constructor(spans: Timeline<S>) {
    this.spans = spans;
  }

  public async collect(bounds: Interval): Promise<S[]> {
    return this.spans(bounds);
  }

  public inspect(f: (spans: readonly S[]) => void) {
    const innerSpans = this.spans;
    this.spans = async bounds => {
      const spans = await innerSpans(bounds);
      f(spans);
      return spans;
    };
  }

  public add<T extends IntervalLike>(span: T): Spans<S | T> {
    const timeline = merge(this.spans, bound([span]));
    return new Spans(timeline);
  }

  public addAll<T extends IntervalLike>(other: Spans<T>): Spans<S | T> {
    const timeline = merge(this.spans, other.spans);
    return new Spans(timeline);
  }

  public filter(predicate: (s: S) => boolean): Spans<S> {
    const timeline = async (bounds: Interval) => (await this.spans(bounds)).filter(predicate);
    return new Spans(timeline);
  }

  public flattenIntoProfile<Result>(map: (v: S) => Result, profileType: ProfileType): ProfileSpecialization<Result> {
    const segments = async (bounds: Interval) => {
      const result = (await this.spans(bounds)).map($ => new Segment(map($), $.interval));
      sortSegments(result, profileType);
      coalesce(result, profileType);
      return result;
    };
    return new Profile(segments, profileType).specialize();
  }

  public combineIntoProfile<Result>(
    op: BinaryOperation<S, Result, Result>,
    profileType: ProfileType
  ): ProfileSpecialization<Result> {
    const segments = async (bounds: Interval) => {
      let acc: Segment<Result>[] = [];
      const remaining = await this.spans(bounds);
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
    return new Profile(segments, profileType).specialize();
  }

  public intoWindows(): Windows {
    return this.flattenIntoProfile(() => true, ProfileType.Windows).assignGaps(false);
  }

  public shiftBy(shiftRising: Temporal.Duration, shiftFalling?: Temporal.Duration): Spans<S> {
    const boundsMap = (bounds: Interval) => {
      let start: Temporal.Duration;
      let end: Temporal.Duration;
      if (Temporal.Duration.compare(shiftRising, shiftFalling!) === 1) {
        start = bounds.start.subtract(shiftRising);
        end = bounds.end.subtract(shiftFalling!);
      } else {
        start = bounds.start.subtract(shiftFalling!);
        end = bounds.end.subtract(shiftRising);
      }
      return Interval.Between(start, end, bounds.startInclusivity, bounds.endInclusivity);
    };
    return this.unsafe.map(s => s.mapInterval(i => i.interval.shiftBy(shiftRising, shiftFalling)), boundsMap);
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
    const timeline = async (bounds: Interval) => {
      const spans = (await this.spans(bounds)).map(i => i.mapInterval(s => Interval.At(s.interval.start)));
      return spans.filter(s => bounds.contains(s.interval));
    };
    return new Spans(timeline);
  }

  public ends(): Spans<S> {
    const timeline = async (bounds: Interval) => {
      const spans = (await this.spans(bounds)).map(i => i.mapInterval(s => Interval.At(s.interval.end)));
      return spans.filter(s => bounds.contains(s.interval));
    };
    return new Spans(timeline);
  }

  public split(
    numberOfSubSpans: number,
    internalStartInclusivity: Inclusivity,
    internalEndInclusivity: Inclusivity,
    strict: boolean = true
  ): Spans<S> {
    if (numberOfSubSpans === 1) return this;
    const timeline = async (bounds: Interval) =>
      (await this.spans(bounds)).flatMap(s => {
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

    return new Spans(timeline);
  }

  // @ts-ignore
  public unsafe = new (class {
    constructor(public outerThis: Spans<S>) {}

    public map<T extends IntervalLike>(f: (span: S) => T, boundsMap: (b: Interval) => Interval): Spans<T> {
      return new Spans<T>(async bounds => (await this.outerThis.spans(boundsMap(bounds))).map(s => f(s)));
    }

    public flatMap<T extends IntervalLike>(f: (span: S) => T[], boundsMap: (b: Interval) => Interval): Spans<T> {
      return new Spans<T>(async bounds => (await this.outerThis.spans(boundsMap(bounds))).flatMap(s => f(s)));
    }
  })(this);
}
