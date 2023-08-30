import type {Interval} from "./interval";

export class Segment<V> {
  public readonly interval: Interval;
  public readonly value: V;

  constructor(interval: Interval, value: V) {
    this.interval = interval;
    this.value = value;
  }
}
