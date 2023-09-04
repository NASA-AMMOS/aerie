export interface Intervallic {
  readonly interval: Interval;
  bound(bounds: Interval): this | undefined;
  mapInterval(map: (i: this) => Interval): this;
}

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

export class Interval implements Intervallic {
  public start: Temporal.Duration;
  public end: Temporal.Duration;
  public startInclusivity: Inclusivity;
  public endInclusivity: Inclusivity;

  public readonly interval = this;

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
    const comparison = Temporal.Duration.compare(this.start, this.end);
    if (comparison === 1) return true;
    return comparison === 0 && (this.startInclusivity === Inclusivity.Exclusive || this.endInclusivity === Inclusivity.Exclusive);
  }

  public isSingleton(): boolean {
    return Temporal.Duration.compare(this.start, this.end) === 0;
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

  public static intersect(left: Interval, right: Interval): Interval {
    let start: Temporal.Duration;
    let startInclusivity: Inclusivity;

    const startComparison = Temporal.Duration.compare(left.start, right.start);
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

    const endComparison = Temporal.Duration.compare(left.end, right.end);
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

  public static union(left: Interval, right: Interval): Interval | undefined {
    if (Interval.intersect(left, right).isEmpty()) return undefined;

    const startComparison = Interval.compareStarts(left, right);
    const endComparison = Interval.compareEnds(left, right);

    let start: Temporal.Duration;
    let startInclusivity: Inclusivity;
    let end: Temporal.Duration;
    let endInclusivity: Inclusivity;

    if (startComparison <= 0) {
      start = left.start;
      startInclusivity = left.startInclusivity;
    } else {
      start = right.start;
      startInclusivity = right.startInclusivity;
    }

    if (endComparison >= 0) {
      end = left.end;
      endInclusivity = left.endInclusivity;
    } else {
      end = right.end;
      endInclusivity = right.endInclusivity;
    }

    return Interval.between(
        start,
        end,
        startInclusivity,
        endInclusivity
    );
  }

  public static subtract(left: Interval, right: Interval) {
    const intersection = Interval.intersect(left, right);
    if (intersection.isEmpty()) return [left];
    else if (Interval.equals(intersection, left)) return [];
    else {
      const result = [
        Interval.between(left.start, right.start, left.startInclusivity, Inclusivity.opposite(right.startInclusivity)),
        Interval.between(right.end, left.end, Inclusivity.opposite(right.startInclusivity), left.endInclusivity)
      ];
      return result.filter($ => !$.isEmpty());
    }
  }

  public static compareStarts(left: Interval, right: Interval): Temporal.ComparisonResult {
    const timeComparison = Temporal.Duration.compare(left.start, right.start);
    if (timeComparison === 0) {
      return Inclusivity.compareRestrictiveness(left.startInclusivity, right.startInclusivity);
    } else return timeComparison;
  }

  public static compareEndToStart(left: Interval, right: Interval): Temporal.ComparisonResult {
    const timeComparison = Temporal.Duration.compare(left.end, right.start);
    if (timeComparison === 0) {
      const incComparison = Inclusivity.compareRestrictiveness(left.endInclusivity, right.startInclusivity);
      if (incComparison !== 0) return 0;
      else {
        if (left.endInclusivity === Inclusivity.Exclusive) return -1;
        else return 1;
      }
    } else return timeComparison;
  }

  public static compareEnds(left: Interval, right: Interval): Temporal.ComparisonResult {
    const timeComparison = Temporal.Duration.compare(left.end, right.end);
    if (timeComparison === 0) {
      return <-1 | 0 | 1>-Inclusivity.compareRestrictiveness(left.startInclusivity, right.startInclusivity);
    } else return timeComparison;
  }

  public static equals(left: Interval, right: Interval): boolean {
    return Temporal.Duration.compare(left.start, right.start) === 0 &&
        Temporal.Duration.compare(left.end, right.end) === 0 &&
        left.startInclusivity === right.startInclusivity &&
        left.endInclusivity === right.endInclusivity;
  }

  public contains(time: Temporal.Duration): boolean;
  public contains(time: Interval): boolean;
  public contains(other: Interval | Temporal.Duration): boolean {
    if (other instanceof Temporal.Duration) other = Interval.at(other);
    return Interval.compareStarts(this, other) <= 0 && Interval.compareEnds(this, other) >= 0;
  }

  public shiftBy(fromStart: Temporal.Duration, fromEnd?: Temporal.Duration): Interval {
    if (fromEnd === undefined) fromEnd = fromStart;
    return Interval.between(
        this.start.add(fromStart),
        this.end.add(fromEnd),
        this.startInclusivity,
        this.endInclusivity
    );
  }

  // @ts-ignore
  public bound(bounds: Interval): Interval | undefined {
    const intersection = Interval.intersect(bounds, this);
    if (intersection.isEmpty()) return undefined;
    return intersection;
  }

  // @ts-ignore
  public mapInterval(map: (i: this) => Interval): Interval {
    return map(this);
  }
}
