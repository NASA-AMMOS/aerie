import type {HasInterval, Interval} from "./interval";

export class Segment<V> implements HasInterval {
  public readonly interval: Interval;
  public readonly value: V;

  constructor(interval: Interval, value: V) {
    this.interval = interval;
    this.value = value;
  }

  public transpose(): Segment<NonNullable<V>> | undefined {
    if (this.value === undefined || this.value === null) return undefined;
    else return new Segment<NonNullable<V>>(this.interval, this.value);
  }

  public mapValue<W>(f: (v: V) => W): Segment<W> {
    return new Segment<W>(this.interval, f(this.value));
  }

  public mapInterval(f: (i: Interval) => Interval): Segment<V> {
    return new Segment<V>(f(this.interval), this.value);
  }
}
