import {HasInterval, Inclusivity, Interval} from "./interval";
import type {Segment} from "./segment";

export type Timeline<V extends HasInterval> = (bounds: Interval) => V[];

export function bound<V extends HasInterval>(data: V[]): Timeline<V>;
export function bound<V extends HasInterval>(data: Iterator<V>): Timeline<V>;
export function bound<V extends HasInterval>(data: Iterable<V>): Timeline<V>;
export function bound<V extends HasInterval>(data: IterableIterator<V>): Timeline<V>;
export function bound<V extends HasInterval>(data: any): Timeline<V> {
  if (Array.isArray(data)) {}
  else if ('next' in data) {
    let iterable = makeIterable(data);
    data = [];
    for (const v in iterable) {
      data.push(v);
    }
  }
  else data = [data];

  // TODO
  return bounds => data;
}

export function coalesce<V>(segments: Segment<V>[], equals?: (l: V, r: V) => boolean): Segment<V>[] {
  if (segments.length === 0) return segments;
  if (equals === undefined) {
    equals = (l, r) => l === r;
  }
  let shortIndex = 0;
  let buffer = segments[0]!;
  for (const segment of segments.slice(1)) {
    let comparison = Interval.compareEndToStart(buffer.interval, segment.interval);
    if (comparison === -1) {
      segments[shortIndex++] = buffer;
      buffer = segment;
    } else {
      if (equals(buffer.value, segment.value)) {
        buffer.interval.end = segment.interval.end;
        buffer.interval.endInclusivity = segment.interval.endInclusivity;
      } else {
        buffer.interval.end = segment.interval.end;
        buffer.interval.endInclusivity = Inclusivity.opposite(segment.interval.endInclusivity);
        segments[shortIndex++] = buffer;
        buffer = segment;
      }
    }
  }
  segments[shortIndex++] = buffer;
  segments.splice(shortIndex);
  return segments;
}

export function makeIterable<V>(iter: Iterator<V>): IterableIterator<V> {
  return {
    [Symbol.iterator]() {return this},
    ...iter
  };
}
