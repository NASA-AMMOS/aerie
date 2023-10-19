import { Inclusivity, Interval, IntervalLike } from './interval.js';
import { Segment } from './segment.js';
import { ProfileType } from './profiles/profile-type.js';

export type Timeline<V extends IntervalLike> = EagerTimeline<V> | LazyTimeline<V>;

export type LazyTimeline<V> = (bounds: Interval) => Promise<V[]>;
export type EagerTimeline<V> = { array: V[]; bounds: Interval };

export function isLazy<V extends IntervalLike, T extends Timeline<V>>(t: T): boolean {
  return typeof t === 'function';
}

export function isEager<V extends IntervalLike, T extends Timeline<V>>(t: T): boolean {
  return t.hasOwnProperty('bounds') && t.hasOwnProperty('array');
}

export type BoundsMap = {
  /** Transforms the bounds produced from the previous operations into the bounds to produce from this operation. */
  eager: (i: Interval) => Interval;

  /** Transforms the bounds requested of this operation into the bounds to request of the previous operations. */
  lazy: (i: Interval) => Interval;
};

export const identityBoundsMap = { eager: ($: Interval) => $, lazy: ($: Interval) => $ };

/**
 * Applies an arbitrary operation to an arbitrary list of timelines.
 *
 * The operation is written agnostic-ly regarding lazy or eager evaluation. The input timelines can be a mix of
 * eager and lazy.
 *
 * @param op
 * @param boundsMap
 * @param timelines
 */
export function applyOperation<O extends IntervalLike>(
  op: (bounds: { current: Interval; next: Interval }, ...arrays: any[][]) => O[],
  boundsMap: BoundsMap,
  ...timelines: Timeline<any>[]
): Timeline<O> {
  if (timelines.every($ => isEager($))) {
    // If all timelines are eagerly evaluated, apply the operation eagerly.
    const oldBounds = (timelines[0] as EagerTimeline<any>).bounds;
    const bounds = { current: oldBounds, next: boundsMap.eager(oldBounds) };
    for (const t of timelines.slice(1)) {
      if (!Interval.equals((t as EagerTimeline<any>).bounds, oldBounds))
        throw new Error(
          `All operand timelines must be evaluated on the same bounds. Requested: ${oldBounds}, found: ${
            (t as EagerTimeline<any>).bounds
          }`
        );
    }
    const arrays = timelines.map($ => ($ as EagerTimeline<any>).array);
    return { array: op(bounds, arrays), bounds: bounds.next };
  } else {
    // If any timeline is lazy, apply return a lazy timeline.
    // Variable is NOT inlined to give the function a name in the stack trace.
    const evaluateOperation = async (newBounds: Interval) => {
      const oldBounds = boundsMap.lazy(newBounds);
      const arrays = await Promise.all(
        timelines.map($ => {
          if (isLazy($)) return ($ as LazyTimeline<any>)(oldBounds);
          else {
            if (!Interval.equals(($ as EagerTimeline<any>).bounds, oldBounds))
              throw new Error(
                `All operand timelines must be evaluated on the same bounds. Requested: ${newBounds}, found: ${
                  ($ as EagerTimeline<any>).bounds
                }`
              );
            return $;
          }
        })
      );
      return op({ current: oldBounds, next: newBounds }, arrays);
    };
    return evaluateOperation;
  }
}

export async function evaluate<V extends IntervalLike>(
  timeline: LazyTimeline<V>,
  bounds: Interval
): Promise<EagerTimeline<V>> {
  return { array: await (timeline as LazyTimeline<any>)(bounds), bounds };
}

export function bound<V extends IntervalLike>(data: V[]): Timeline<V>;
export function bound<V extends IntervalLike>(data: Iterator<V>): Timeline<V>;
export function bound<V extends IntervalLike>(data: Iterable<V>): Timeline<V>;
export function bound<V extends IntervalLike>(data: IterableIterator<V>): Timeline<V>;
export function bound<V extends IntervalLike>(data: any): Timeline<V> {
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

  return async bounds => truncate(data as V[], bounds);
}

export function truncate<V extends IntervalLike>(data: V[], bounds: Interval): V[] {
  return data.map($ => $.bound(bounds)).filter($ => $ !== undefined) as V[];
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
 * In-place flattens an array of overlapping segments into non-overlapping segments with unequal consecutive values.
 *
 * *Input condition*: segments must be sorted such that between each pair of consecutive elements, one of the following is true:
 * - if the values are unequal, the start and end times (including inclusivity) must be strictly increasing
 * - if the values are equal, the start time must be non-decreasing.
 *
 * This input condition is not checked, and violating it is undefined behavior.
 *
 * Empty intervals are removed, and their values are not considered for the purposes of the sorted input condition.
 *
 * @param segments
 * @param typeTag
 */
export function coalesce<V>(segments: Segment<V>[], typeTag: ProfileType): Segment<V>[] {
  const equals = ProfileType.getSegmentComparator(typeTag);
  if (segments.length === 0) return segments;
  let shortIndex = 0;
  let startIndex = 0;
  while (segments[startIndex].interval.isEmpty()) startIndex++;
  let buffer = segments[startIndex];
  for (const segment of segments.slice(startIndex + 1)) {
    if (segment.interval.isEmpty()) continue;
    const comparison = Interval.compareEndToStart(buffer.interval, segment.interval);
    if (comparison === -1) {
      segments[shortIndex++] = buffer;
      buffer = segment;
    } else if (comparison === 0) {
      if (equals(buffer.value, segment.value)) {
        if (Interval.compareEnds(buffer.interval, segment.interval) < 0) {
          buffer.interval.end = segment.interval.end;
          buffer.interval.endInclusivity = segment.interval.endInclusivity;
        }
      } else {
        segments[shortIndex++] = buffer;
        buffer = segment;
      }
    } else {
      if (equals(buffer.value, segment.value)) {
        if (Interval.compareEnds(buffer.interval, segment.interval) < 0) {
          buffer.interval.end = segment.interval.end;
          buffer.interval.endInclusivity = segment.interval.endInclusivity;
        }
      } else {
        buffer.interval.end = segment.interval.start;
        buffer.interval.endInclusivity = Inclusivity.opposite(segment.interval.startInclusivity);
        segments[shortIndex++] = buffer;
        buffer = segment;
      }
    }
  }
  segments[shortIndex++] = buffer;
  segments.splice(shortIndex);
  return segments;
}

export function cache<V extends IntervalLike>(t: Timeline<V>): Timeline<V> {
  // Stored as a list of tuples that we must search through because Intervals contain Durations,
  // which have an inaccurate equality check.
  if (isEager(t)) return t;
  let history: [Interval, V[]][] = [];
  return async bounds => {
    for (const [i, t] of history) {
      if (Interval.equals(i, bounds)) return [...t];
    }
    const result = await (t as LazyTimeline<any>)(bounds);
    history.push([bounds, result]);
    return result;
  };
}

export function zip<V extends IntervalLike, W extends IntervalLike>(
  left: Timeline<V>,
  right: Timeline<W>
): Timeline<V | W> {
  return applyOperation((_: any, a) => a.flat(), identityBoundsMap, left, right);
}

export function makeIterable<V>(iter: Iterator<V>): IterableIterator<V> {
  return {
    [Symbol.iterator]() {
      return this;
    },
    ...iter
  };
}
