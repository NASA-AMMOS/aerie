import {Inclusivity, Interval} from "./interval";
import type {Segment} from "./segment";

export type Timeline<V extends Boundable<V>> = (bounds: Interval) => V[];

export interface Boundable<This> {
  bound(bounds: Interval): This | undefined;
}

export function bound<V extends Boundable<V>>(data: V[]): Timeline<V>;
export function bound<V extends Boundable<V>>(data: Iterator<V>): Timeline<V>;
export function bound<V extends Boundable<V>>(data: Iterable<V>): Timeline<V>;
export function bound<V extends Boundable<V>>(data: IterableIterator<V>): Timeline<V>;
export function bound<V extends Boundable<V>>(data: any): Timeline<V> {
  if (Array.isArray(data)) {}
  else if ('next' in data) {
    const iterable = makeIterable(data);
    data = [];
    for (const v in iterable) {
      data.push(v);
    }
  }
  else data = [data];

  return bounds => (data as V[]).map($ => $.bound(bounds)).filter($ => $ !== undefined) as V[];
}

export function coalesce<V>(segments: Segment<V>[], equals?: (l: V, r: V) => boolean): Segment<V>[] {
  if (segments.length === 0) return segments;
  if (equals === undefined) {
    equals = (l, r) => l === r;
  }
  let shortIndex = 0;
  let buffer = segments[0]!;
  for (const segment of segments.slice(1)) {
    const comparison = Interval.compareEndToStart(buffer.interval, segment.interval);
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

function cache<V extends Boundable<V>>(t: Timeline<V>): Timeline<V> {
  // Stored as a list of tuples that we must search through because Intervals contain Durations,
  // which have an inaccurate equality check.
  let history: [Interval, V[]][] = [];
  return bounds => {
    for (const [i, t] of history) {
      if (Interval.equals(i, bounds)) return [...t];
    }
    const result = t(bounds);
    history.push([bounds, result]);
    return result;
  }
}

export function makeIterable<V>(iter: Iterator<V>): IterableIterator<V> {
  return {
    [Symbol.iterator]() {return this},
    ...iter
  };
}
