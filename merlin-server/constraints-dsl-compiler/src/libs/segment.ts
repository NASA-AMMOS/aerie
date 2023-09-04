import {Interval} from "./interval";
import type {Boundable} from "./timeline";

export class Segment<V> implements Boundable<Segment<V>> {
  public readonly interval: Interval;
  public readonly value: V;

  constructor(value: V, interval: Interval) {
    this.interval = interval;
    this.value = value;
  }

  public transpose(): Segment<NonNullable<V>> | undefined {
    if (this.value === undefined || this.value === null) return undefined;
    else return new Segment<NonNullable<V>>(this.value, this.interval);
  }

  public mapValue<W>(f: (v: V, i: Interval) => W): Segment<W> {
    return new Segment<W>(f(this.value, this.interval), this.interval);
  }

  public mapInterval(f: (v: V, i: Interval) => Interval): Segment<V> {
    return new Segment<V>(this.value, f(this.value, this.interval));
  }

  bound(bounds: Interval): Segment<V> | undefined {
    const intersection = Interval.intersect(bounds, this.interval);
    if (intersection.isEmpty()) return undefined;
    else return new Segment(this.value, intersection);
  }


}
