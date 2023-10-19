import { Interval, IntervalLike } from './interval.js';

/**
 * A generic container for associating a value with an interval on the timeline.
 */
export class Segment<V> implements IntervalLike {

  constructor(
      /** The interval on the timeline over which this segment applies. */
      public readonly value: V,
      /** The value contained in this segment */
      public readonly interval: Interval
  ) {}

  /** Static version of the constructor */
  public static Of<V>(value: V, interval: Interval): Segment<V> {
    return new Segment(value, interval);
  }

  /**
   * Returns `undefined` if the segment contains `undefined` or `null`. Otherwise, returns this unchanged.
   *
   * Useful for extracting nulls/undefined into gaps.
   */
  public transpose(): Segment<NonNullable<V>> | undefined {
    if (this.value === undefined || this.value === null) return undefined;
    else return new Segment<NonNullable<V>>(this.value, this.interval);
  }

  /**
   * Applies the provided map to this segment's value and returns a segment with the mapped
   * value on the same interval.
   *
   * @param f
   */
  public mapValue<W>(f: (s: Segment<V>) => W): Segment<W> {
    return new Segment<W>(f(this), this.interval);
  }

  /**
   * Applies the provided map to this segment's interval and returns a segment on the mapped
   * interval with the same value.
   *
   * @param f
   */
  public mapInterval(f: (s: this) => Interval): this {
    // @ts-ignore
    return new Segment<V>(this.value, f(this));
  }

  /**
   * Returns a new segment with a truncated interval to ensure that it is within the provided bounds.
   *
   * Returns `undefined` if this segment's interval is fully outside the bounds.
   *
   * @param bounds
   */
  public bound(bounds: Interval): this | undefined {
    const intersection = Interval.intersect(bounds, this.interval);
    if (intersection.isEmpty()) return undefined;
    else {
      // @ts-ignore
      return new Segment<V>(this.value, intersection);
    }
  }

  public toString(): string {
    return `${this.interval}: ${this.value}`;
  }
}
