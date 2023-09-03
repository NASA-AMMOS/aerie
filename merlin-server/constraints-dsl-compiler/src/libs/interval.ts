export enum Inclusivity {
  Inclusive,
  Exclusive
}

export namespace Inclusivity {
  export function compareRestrictiveness(left: Inclusivity, right: Inclusivity): Temporal.ComparisonResult {
    if (left === right) return 0;
    if (left === Inclusivity.Inclusive) return -1;
    else return 1;
  }

  export function opposite(inc: Inclusivity): Inclusivity {
    if (inc === Inclusivity.Inclusive) return Inclusivity.Exclusive;
    else return Inclusivity.Inclusive;
  }
}

export class Interval {
  public start: Temporal.Duration;
  public end: Temporal.Duration;
  public startInclusivity: Inclusivity;
  public endInclusivity: Inclusivity;

  private constructor(start: Temporal.Duration, end: Temporal.Duration, startInclusivity: Inclusivity, endInclusivity: Inclusivity) {
    this.start = start;
    this.end = end;
    this.startInclusivity = startInclusivity;
    this.endInclusivity = endInclusivity;
  }

  public static between(start: Temporal.Duration, end: Temporal.Duration, startInclusivity?: Inclusivity, endInclusivity?: Inclusivity): Interval {
    if (startInclusivity === undefined) startInclusivity = Inclusivity.Inclusive;
    if (endInclusivity === undefined) endInclusivity = startInclusivity;

    return new Interval(start, end, startInclusivity, endInclusivity);
  }

  public static at(time: Temporal.Duration): Interval {
    return new Interval(time, time, Inclusivity.Inclusive, Inclusivity.Inclusive);
  }

  public isEmpty(): boolean {

  }

  public duration(): Temporal.Duration {
    return this.end.subtract(this.start);
  }

  public includesStart(): boolean {
    return this.startInclusivity === Inclusivity.Inclusive;
  }

  public includesEnd(): boolean {
    return this.endInclusivity === Inclusivity.Inclusive;
  }

  public static readonly Forever = new Interval(0, 1, Inclusivity.Inclusive, Inclusivity.Exclusive);

  public static intersect(left: Interval, right: Interval): Interval {
    let start: Temporal.Duration;
    let startInclusivity: Inclusivity;

    let startComparison = Temporal.Duration.compare(left.start, right.start);
    if (startComparison > 0) {
      start = left.start;
      startInclusivity = left.startInclusivity;
    } else if (startComparison < 0) {
      start = right.start;
      startInclusivity = right.startInclusivity;
    } else {
      start = left.start;
      startInclusivity = (left.includesStart() && right.includesStart()) ? Inclusivity.Inclusive : Inclusivity.Exclusive;
    }

    let end: Temporal.Duration;
    let endInclusivity: Inclusivity;

    let endComparison = Temporal.Duration.compare(left.end, right.end);
    if (endComparison < 0) {
      end = left.end;
      endInclusivity = left.endInclusivity;
    } else if (endComparison > 0) {
      end = right.end;
      endInclusivity = right.endInclusivity;
    } else {
      end = left.end;
      endInclusivity = (left.includesEnd() && right.includesEnd()) ? Inclusivity.Inclusive : Inclusivity.Exclusive;
    }

    return Interval.between(start, end, startInclusivity, endInclusivity);
  }

  public static compareStarts(left: Interval, right: Interval): Temporal.ComparisonResult {
    let timeComparison = Temporal.Duration.compare(left.start, right.start);
    if (timeComparison === 0) {
      return Inclusivity.compareRestrictiveness(left.startInclusivity, right.startInclusivity);
    } else return timeComparison;
  }

  public static compareEndToStart(left: Interval, right: Interval): Temporal.ComparisonResult {
    let timeComparison = Temporal.Duration.compare(left.end, right.start);
    if (timeComparison === 0) {
      let incComparison = Inclusivity.compareRestrictiveness(left.endInclusivity, right.startInclusivity);
      if (incComparison === 0) {
        if (left.endInclusivity === Inclusivity.Exclusive) return -1;
        else return 0;
      } else return <-1 | 0 | 1>-incComparison
    } else return timeComparison;
  }

  public static compareEnds(left: Interval, right: Interval): Temporal.ComparisonResult {
    let timeComparison = Temporal.Duration.compare(left.end, right.end);
    if (timeComparison === 0) {
      return <-1 | 0 | 1>-Inclusivity.compareRestrictiveness(left.startInclusivity, right.startInclusivity);
    } else return timeComparison;
  }
}

export interface HasInterval {
  readonly interval: Interval
}
