import type {Timeline} from "../timeline";
import {bound, coalesce, merge, sortSegments} from "../timeline";
import {Segment} from "../segment";
import type {Windows} from "../profiles/windows";
import {Intervallic, Inclusivity, Interval} from "../interval";
import {ProfileType} from "../profiles/profile-type";
import {map2Arrays, Profile, ProfileSpecialization} from "../profiles/profile";
import {LinearEquation, Real} from "../profiles/real";
import {BinaryOperation} from "../binary-operation";

export class Spans<S extends Intervallic> {
  private spans: Timeline<S>;

  constructor(spans: Timeline<S>) {
    this.spans = spans;
  }

  public async collect(bounds: Interval): Promise<S[]> {
    return this.spans(bounds);
  }

  public inspect(f: (spans: readonly S[]) => void) {
    const innerSpans = this.spans;
    this.spans = bounds => {
      const spans = innerSpans(bounds);
      f(spans);
      return spans;
    }
  }

  public add<T extends Intervallic>(span: T): Spans<S | T> {
    const timeline = merge(this.spans, bound([span]));
    return new Spans(timeline);
  }

  public addAll<T extends Intervallic>(other: Spans<T>): Spans<S | T> {
    const timeline = merge(this.spans, other.spans);
    return new Spans(timeline);
  }

  public filter(predicate: (s: S) => boolean): Spans<S> {
    const timeline = (bounds: Interval) => this.spans(bounds).filter(predicate);
    return new Spans(timeline);
  }

  public flattenIntoProfile<Result>(map: (v: S) => Result, profileType: ProfileType): ProfileSpecialization<Result> {
    const segments = (bounds: Interval) => {
      const result = this.spans(bounds).map($ => new Segment(map($), $.interval));
      sortSegments(result, profileType);
      coalesce(result, profileType);
      return result;
    }
    return (new Profile(segments, profileType)).specialize();
  }

  public combineIntoProfile<Result>(op: BinaryOperation<S, Result, Result>, profileType: ProfileType): ProfileSpecialization<Result> {
    const segments = (bounds: Interval) => {
      let acc: Segment<Result>[] = [];
      const remaining = this.spans(bounds);
      while (remaining.length > 0) {
        const batch: Segment<S>[] = [];
        let previousTime = bounds.start;
        let previousInclusivity = Inclusivity.opposite(bounds.startInclusivity);
        for (const span of remaining) {
          const startComparison = Temporal.Duration.compare(span.interval.start, previousTime);
          if (startComparison > 0 || (startComparison === 0 && previousInclusivity !== span.interval.startInclusivity)) {
            batch.push(new Segment(span, span.interval));
            previousTime = span.interval.end;
            previousInclusivity = span.interval.endInclusivity;
          }
        }
        acc = map2Arrays(batch, acc, op);
      }
      return coalesce(acc, profileType);
    }
    return (new Profile(segments, profileType)).specialize();
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
      return Interval.between(
          start,
          end,
          bounds.startInclusivity,
          bounds.endInclusivity
      );
    };
    return this.unsafe.map(
        s => s.mapInterval(i => i.interval.shiftBy(shiftRising, shiftFalling)),
        boundsMap
    );
  }

  public countActive(): Real {
    return this.combineIntoProfile<LinearEquation>(BinaryOperation.cases(
        () => LinearEquation.Constant(1),
        r => r,
        (l, r) => r.plus(1)
    ), ProfileType.Real).assignGaps(0);
  }

  public accumulatedDuration(unit: Temporal.Duration): Real {
    return this.countActive().integrate(unit);
  }

  public starts(): Spans<S> {
    const timeline = (bounds: Interval) => {
      const spans = this.spans(bounds).map(i => i.mapInterval(s => Interval.at(s.interval.start)));
      return spans.filter(s => bounds.contains(s.interval));
    }
    return new Spans(timeline);
  }

  public ends(): Spans<S> {
    const timeline = (bounds: Interval) => {
      const spans = this.spans(bounds).map(i => i.mapInterval(s => Interval.at(s.interval.end)));
      return spans.filter(s => bounds.contains(s.interval));
    }
    return new Spans(timeline);
  }

  public split(numberOfSubSpans: number, internalStartInclusivity: Inclusivity, internalEndInclusivity: Inclusivity, strict: boolean = true): Spans<S> {
    if (numberOfSubSpans === 1) return this;
    const timeline = (bounds: Interval) => this.spans(bounds).flatMap(s => {
      const i = s.interval;

      const fullWidth = i.duration().total('microsecond');
      const subWidth = Math.floor(fullWidth / numberOfSubSpans);

      if (i.isSingleton()) throw new Error("Cannot split an instantaneous span into sub-spans.");
      else if (subWidth === 0) throw new Error(`Cannot split a span only ${subWidth} microseconds long in to ${numberOfSubSpans} sub-spans.`);

      if (strict) {
        if (Interval.compareStarts(i, bounds) === 0)
          throw new Error("Cannot split a span that starts at or before the bounds start. Consider setting the `strict` arg to `false`.");
        if (Interval.compareEnds(i, bounds) === 0)
          throw new Error("Cannot split a span that ends at or before the bounds end. Consider setting the `strict` arg to `false`.");
      }

      let acc = i.start.add({microseconds: subWidth});
      let result: S[] = [];
      result.push(s.mapInterval(() => Interval.between(i.start, acc, i.startInclusivity, internalEndInclusivity)));
      for (let index = 0; index < numberOfSubSpans - 1; index++) {
        let nextAcc = acc.add({microseconds: subWidth});
        result.push(s.mapInterval(() => Interval.between(acc, nextAcc, internalStartInclusivity, internalEndInclusivity)));
        acc = nextAcc;
      }
      result.push(s.mapInterval(() => Interval.between(acc, i.end, internalStartInclusivity, i.endInclusivity)));

      return result;
    });

    return new Spans(timeline);
  }

  public unsafe = new class {
    constructor(private outerThis: Spans<S>) {}

    public map<T extends Intervallic>(f: (span: S) => T, boundsMap: (b: Interval) => Interval): Spans<T> {
      return new Spans<T>(bounds => this.outerThis.spans(boundsMap(bounds)).map(s => f(s)));
    }

    public flatMap<T extends Intervallic>(f: (span: S) => T[], boundsMap: (b: Interval) => Interval): Spans<T> {
      return new Spans<T>(bounds => this.outerThis.spans(boundsMap(bounds)).flatMap(s => f(s)));
    }
  }(this);
}
