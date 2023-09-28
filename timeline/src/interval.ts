import { Temporal } from '@js-temporal/polyfill';

/**
 * An interface for objects that have an interval-like extent on the timeline.
 *
 * Segments, activity instances, violations, and intervals themselves are the main examples.
 */
export interface IntervalLike {
  get interval(): Interval;
  bound(bounds: Interval): this | undefined;
  mapInterval(map: (i: this) => Interval): this;
}

/** Represents whether the ends of an interval contain their extremes. */
export enum Inclusivity {
  Inclusive,
  Exclusive
}

export namespace Inclusivity {
  /**
   * Compares the restrictiveness of two inclusivities, where exclusive is considered more restrictive than inclusive.
   * @param left
   * @param right
   */
  export function compareRestrictiveness(left: Inclusivity, right: Inclusivity): Temporal.ComparisonResult {
    if (left === right) return 0;
    if (left === Inclusivity.Inclusive) return -1;
    else return 1;
  }

  /**
   * Inverts an inclusivity to the other value.
   * @param inc
   */
  export function opposite(inc: Inclusivity): Inclusivity {
    if (inc === Inclusivity.Inclusive) return Inclusivity.Exclusive;
    else return Inclusivity.Inclusive;
  }
}

/** A contiguous range of time. */
export class Interval implements IntervalLike {
  public start: Temporal.Duration;
  public end: Temporal.Duration;
  public startInclusivity: Inclusivity;
  public endInclusivity: Inclusivity;

  /**
   * Self-reference required to satisfy the IntervalLike interface.
   *
   * Usage is unnecessary unless operating on polymorphic IntervalLike objects.
   */
  public get interval(): Interval {
    return this;
  }

  private constructor(
    start: Temporal.Duration,
    end: Temporal.Duration,
    startInclusivity: Inclusivity,
    endInclusivity: Inclusivity
  ) {
    this.start = start;
    this.end = end;
    this.startInclusivity = startInclusivity;
    this.endInclusivity = endInclusivity;
  }

  /**
   * Creates a new interval between two times, with optional inclusivity.
   *
   * If no inclusivity is provided, the interval will be closed on both ends.
   * If only one inclusivity is provided, the interval will have that inclusivity on both ends.
   * If two inclusivities are provided, they correspond to the start and end.
   *
   * @param start start time of new interval
   * @param end end time of new interval
   * @param startInclusivity inclusivity of interval start; default is Inclusive.
   * @param endInclusivity inclusivity of interval end; default is equal to startInclusivity.
   * @constructor
   */
  public static Between(
    start: Temporal.Duration,
    end: Temporal.Duration,
    startInclusivity?: Inclusivity,
    endInclusivity?: Inclusivity
  ): Interval {
    if (startInclusivity === undefined) startInclusivity = Inclusivity.Inclusive;
    if (endInclusivity === undefined) endInclusivity = startInclusivity;

    return new Interval(start, end, startInclusivity, endInclusivity);
  }

  /** Create a singleton interval at a single time. */
  public static At(time: Temporal.Duration): Interval {
    return new Interval(time, time, Inclusivity.Inclusive, Inclusivity.Inclusive);
  }

  /** Create an empty interval at time zero. */
  public static Empty(): Interval {
    return new Interval(new Temporal.Duration(), new Temporal.Duration(), Inclusivity.Exclusive, Inclusivity.Exclusive);
  }

  /**
   * Returns true if the interval contains no points.
   *
   * This can happen if the end is before the start, or if the start and end are equal but
   * at least one of the ends is exclusive.
   */
  public isEmpty(): boolean {
    const comparison = Temporal.Duration.compare(this.start, this.end);
    if (comparison === 1) return true;
    return (
      comparison === 0 &&
      (this.startInclusivity === Inclusivity.Exclusive || this.endInclusivity === Inclusivity.Exclusive)
    );
  }

  /** Returns true if the interval contains only a single time. */
  public isSingleton(): boolean {
    return Temporal.Duration.compare(this.start, this.end) === 0;
  }

  /** Calculates the duration of the interval. */
  public duration(): Temporal.Duration {
    return this.end.subtract(this.start);
  }

  /** Whether the interval includes its start point. */
  public includesStart(): boolean {
    return this.startInclusivity === Inclusivity.Inclusive;
  }

  /** Whether the interval includes its start end point. */
  public includesEnd(): boolean {
    return this.endInclusivity === Inclusivity.Inclusive;
  }

  /**
   * Calculates the intersection between two intervals.
   *
   * If there is no intersection, the result will be an empty interval.
   *
   * @param left
   * @param right
   */
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
      startInclusivity = left.includesStart() && right.includesStart() ? Inclusivity.Inclusive : Inclusivity.Exclusive;
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
      endInclusivity = left.includesEnd() && right.includesEnd() ? Inclusivity.Inclusive : Inclusivity.Exclusive;
    }

    return Interval.Between(start, end, startInclusivity, endInclusivity);
  }

  /**
   * Calculates the interval union of two intervals.
   *
   * If the intervals don't overlap, the result cannot be represented as an interval, and the result is `undefined`.
   *
   * @param left
   * @param right
   */
  public static union(left: Interval, right: Interval): Interval | undefined {
    if (Interval.compareStarts(left, right) > 0) {
      const hold = right;
      right = left;
      left = hold;
    }
    if (Interval.intersect(left, right).isEmpty() && this.compareEndToStart(left, right) !== 0) return undefined;

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

    return Interval.Between(start, end, startInclusivity, endInclusivity);
  }

  /**
   * Remove the intersection of the two arguments from the left argument, splitting it if necessary.
   *
   * @param left interval to subtract from
   * @param right interval to remove from `left`
   */
  public static subtract(left: Interval, right: Interval): Interval[] {
    const intersection = Interval.intersect(left, right);
    if (intersection.isEmpty()) return [left];
    else if (Interval.equals(intersection, left)) return [];
    else {
      const result = [
        Interval.Between(left.start, right.start, left.startInclusivity, Inclusivity.opposite(right.startInclusivity)),
        Interval.Between(right.end, left.end, Inclusivity.opposite(right.startInclusivity), left.endInclusivity)
      ];
      return result.filter($ => !$.isEmpty());
    }
  }

  /**
   * Compare the start bounds of the intervals, accounting for inclusivity.
   *
   * @returns `-1` if `left` starts first, `1` if `right` starts first, `0` if they are equal
   */
  public static compareStarts(left: Interval, right: Interval): Temporal.ComparisonResult {
    const timeComparison = Temporal.Duration.compare(left.start, right.start);
    if (timeComparison === 0) {
      return Inclusivity.compareRestrictiveness(left.startInclusivity, right.startInclusivity);
    } else return timeComparison;
  }

  /**
   * Compare the end bounds of the intervals, accounting for inclusivity.
   *
   * @returns `-1` if `left` ends first, `1` if `right` ends first, `0` if they are equal
   */
  public static compareEnds(left: Interval, right: Interval): Temporal.ComparisonResult {
    const timeComparison = Temporal.Duration.compare(left.end, right.end);
    if (timeComparison === 0) {
      return Inclusivity.compareRestrictiveness(right.startInclusivity, left.startInclusivity);
    } else return timeComparison;
  }

  /**
   * Compare the end of the left interval to the start of the right interval, accounting for inclusivity.
   *
   * A result of `0` means the two intervals meet with no gap and no intersection. This means that
   * `compareEndToStart([x, y), (y, z])` will return `-1`, and
   * `compareEndToStart([x, y], [y, z])` will return `1`.
   *
   * @returns `-1` if `left` ends before `right` starts, `1` if `left` ends after `right` starts, and `0`
   *          if they meet with no overlap.
   */
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

  /** Tests if two intervals are equal. */
  public static equals(left: Interval, right: Interval): boolean {
    return (
      Temporal.Duration.compare(left.start, right.start) === 0 &&
      Temporal.Duration.compare(left.end, right.end) === 0 &&
      left.startInclusivity === right.startInclusivity &&
      left.endInclusivity === right.endInclusivity
    );
  }

  /** Test if this interval contains a specific time. */
  public contains(time: Temporal.Duration): boolean;
  /** Test if this interval contains all of another interval. */
  public contains(time: Interval): boolean;
  public contains(other: Interval | Temporal.Duration): boolean {
    if (other instanceof Temporal.Duration) other = Interval.At(other);
    return Interval.compareStarts(this, other) <= 0 && Interval.compareEnds(this, other) >= 0;
  }

  /**
   * Shifts the start and end of this interval by relative durations.
   *
   * If the bounds cross, the interval is considered empty. If this is a possibility,
   * it is your responsibility to check via {@link isEmpty}.
   *
   * @param fromStart duration to shift start of interval.
   * @param fromEnd duration to shift end of interval; defaults to `fromStart` if omitted.
   */
  public shiftBy(fromStart: Temporal.Duration, fromEnd?: Temporal.Duration): Interval {
    if (fromEnd === undefined) fromEnd = fromStart;
    return Interval.Between(
      this.start.add(fromStart),
      this.end.add(fromEnd),
      this.startInclusivity,
      this.endInclusivity
    );
  }

  /**
   * Restrict this interval to within a bounding interval via intersection.
   *
   * Returns `undefined` if empty.
   *
   * @param bounds bounding interval
   */
  // @ts-ignore
  public bound(bounds: Interval): Interval | undefined {
    const intersection = Interval.intersect(bounds, this);
    if (intersection.isEmpty()) return undefined;
    return intersection;
  }

  /**
   * Required to satisfy the {@link IntervalLike} interface.
   *
   * Usage on plain intervals is unnecessary unless operating on polymorphic IntervalLikes.
   */
  // @ts-ignore
  public mapInterval(map: (i: this) => Interval): Interval {
    return map(this);
  }

  public toString(): string {
    let result = '';
    if (this.startInclusivity === Inclusivity.Inclusive) {
      result += '[';
    } else {
      result += '(';
    }
    result += this.start.toString() + ',' + this.end.toString();
    if (this.endInclusivity === Inclusivity.Inclusive) {
      result += ']';
    } else {
      result += ')';
    }
    return result;
  }

  public toJSON(): any {
    return this.toString();
  }
}
