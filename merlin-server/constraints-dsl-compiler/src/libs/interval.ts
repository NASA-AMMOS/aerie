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

  public static readonly Forever = new Interval(0, 1, Inclusivity.Inclusive, Inclusivity.Exclusive);

  public static intersect(left: Interval, right: Interval): Interval {

  }
}
