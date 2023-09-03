import type {HasInterval, Interval} from "./interval";

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

export function makeIterable<V>(iter: Iterator<V>): IterableIterator<V> {
  return {
    [Symbol.iterator]() {return this},
    ...iter
  };
}
