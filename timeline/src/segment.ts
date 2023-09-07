import { Interval, IntervalLike } from './interval.js';

export class Segment<V> implements IntervalLike {
  public readonly interval: Interval;
  public readonly value: V;

  constructor(value: V, interval: Interval) {
    this.interval = interval;
    this.value = value;
  }

  public static Of<V>(value: V, interval: Interval): Segment<V> {
    return new Segment(value, interval);
  }

  public transpose(): Segment<NonNullable<V>> | undefined {
    if (this.value === undefined || this.value === null) return undefined;
    else return new Segment<NonNullable<V>>(this.value, this.interval);
  }

  public mapValue<W>(f: (s: Segment<V>) => W): Segment<W> {
    return new Segment<W>(f(this), this.interval);
  }

  // @ts-ignore
  public mapInterval(f: (s: Segment<V>) => Interval): Segment<V> {
    return new Segment<V>(this.value, f(this));
  }

  bound(bounds: Interval): this | undefined {
    const intersection = Interval.intersect(bounds, this.interval);
    if (intersection.isEmpty()) return undefined;
    else {
      // @ts-ignore
      return new Segment<V>(this.value, intersection);
    }
  }
}
