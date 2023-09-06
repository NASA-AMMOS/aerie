import { Inclusivity, Interval, Intervallic } from './interval.js';
import { Segment } from './segment.js';
import { ProfileType } from './profiles/profile-type.js';

export type Timeline<V extends Intervallic> = (bounds: Interval) => Promise<V[]>;

export function bound<V extends Intervallic>(data: V[]): Timeline<V>;
export function bound<V extends Intervallic>(data: Iterator<V>): Timeline<V>;
export function bound<V extends Intervallic>(data: Iterable<V>): Timeline<V>;
export function bound<V extends Intervallic>(data: IterableIterator<V>): Timeline<V>;
export function bound<V extends Intervallic>(data: any): Timeline<V> {
  let array: V[];
  if (Array.isArray(data)) array = data;
  else if ('next' in data) {
    const iterable = makeIterable<V>(data);
    array = [];
    for (const v of iterable) {
      array.push(v);
    }
  } else array = [data];

  if (array.length > 0) {
    if (array[0] instanceof Segment) {
      const guessedType = ProfileType.guessType(array[0].value);
      sortSegments(array as unknown as Segment<any>[], guessedType);
      coalesce(array as unknown as Segment<any>[], guessedType);
    }
  }

  return async bounds => (data as V[]).map($ => $.bound(bounds)).filter($ => $ !== undefined) as V[];
}

export function sortSegments<V>(segments: Segment<V>[], profileType: ProfileType): Segment<V>[] {
  const valueComparator = ProfileType.getSegmentComparator(profileType);
  return segments.sort((l: Segment<any>, r: Segment<any>) => {
    const startComparison = Interval.compareStarts(l.interval, r.interval);
    const endComparison = Interval.compareEnds(l.interval, r.interval);
    if (startComparison === endComparison && startComparison !== 0) {
      return startComparison;
    } else {
      if (valueComparator(l.value, r.value)) return startComparison;
      throw new Error(
        'Segments should be sortable into an order in which both start and end times are strictly increasing, unless segment values are equal.'
      );
    }
  });
}

/**
 * input condition: segments must be sorted such that between each pair of consecutive elements, one of the following is true:
 * - if the values are unequal, the start and end times (including inclusivity) must be strictly increasing
 * - if the values are equal, the start time must be non-decreasing.
 * @param segments
 * @param typeTag
 */
export function coalesce<V>(segments: Segment<V>[], typeTag: ProfileType): Segment<V>[] {
  const equals = ProfileType.getSegmentComparator(typeTag);
  if (segments.length === 0) return segments;
  let shortIndex = 0;
  let buffer = segments[0]!;
  for (const segment of segments.slice(1)) {
    const comparison = Interval.compareEndToStart(buffer.interval, segment.interval);
    if (comparison < 1) {
      segments[shortIndex++] = buffer;
      buffer = segment;
    } else {
      if (equals(buffer.value, segment.value)) {
        if (Interval.compareEnds(buffer.interval, segment.interval) < 0) {
          buffer.interval.end = segment.interval.end;
          buffer.interval.endInclusivity = segment.interval.endInclusivity;
        }
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

export function cache<V extends Intervallic>(t: Timeline<V>): Timeline<V> {
  // Stored as a list of tuples that we must search through because Intervals contain Durations,
  // which have an inaccurate equality check.
  let history: [Interval, V[]][] = [];
  return async bounds => {
    for (const [i, t] of history) {
      if (Interval.equals(i, bounds)) return [...t];
    }
    const result = await t(bounds);
    history.push([bounds, result]);
    return result;
  };
}

export function merge<V extends Intervallic, W extends Intervallic>(
  left: Timeline<V>,
  right: Timeline<W>
): Timeline<V | W> {
  return async bounds => (await Promise.all([left(bounds), right(bounds)])).flat();
}

export function makeIterable<V>(iter: Iterator<V>): IterableIterator<V> {
  return {
    [Symbol.iterator]() {
      return this;
    },
    ...iter
  };
}
