package gov.nasa.jpl.aerie.timeline.ops

import gov.nasa.jpl.aerie.timeline.*
import gov.nasa.jpl.aerie.timeline.BoundsTransformer.Companion.IDENTITY
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import gov.nasa.jpl.aerie.timeline.util.map2ParallelLists

/**
 * Operations mixin for timelines of segments.
 */
interface SegmentOps<V : Any, THIS: SegmentOps<V, THIS>>: NonZeroDurationOps<Segment<V>, THIS> {
  /**
   * [(DOC)][mapValues] Locally transforms the values of a profile without changing the intervals or profile type.
   *
   * @param f a function which takes a [Segment] and returns a new value of type [V]
   */
  fun mapValues(f: (Segment<V>) -> V) = unsafeMap(IDENTITY, false) { it.mapValue(f) }

  /**
   * [(DOC)][mapValues] Locally transforms the values of a profile without changing the intervals.
   *
   * The result can be a different profile type.
   *
   * @param R the result's payload type
   * @param RESULT the result's timeline type
   * @param ctor the constructor of the result profile
   * @param f a function which takes a [Segment] and returns a new value of any type
   */
  fun <R: Any, RESULT: SegmentOps<R, RESULT>> mapValues(ctor: (Timeline<Segment<R>, RESULT>) -> RESULT, f: (Segment<V>) -> R) =
      unsafeMap(ctor, IDENTITY, false) { it.mapValue(f) }

  /** [(DOC)][flatMapValues] A simpler version of [flatMapValues] for operations that don't change the timeline type. */
  fun <NESTED: SegmentOps<V, NESTED>> flatMapValues(f: (Segment<V>) -> NESTED) =
      unsafeFlatMap(ctor, IDENTITY, false) { it.mapValue(f) }

  /**
   * [(DOC)][flatMapValues] Maps segments into a collection of nested timelines and flattens them into their original intervals.
   *
   * Similar to [GeneralOps.unsafeFlatMap] except that the mapper function cannot change the interval the nested timeline
   * is flattened into.
   *
   * @param R the result payload type
   * @param NESTED the nested timeline type; typically the same as [RESULT]
   * @param RESULT the result timeline type
   *
   * @param ctor the constructor of the result timeline
   * @param f a mapper function that converts each timeline object to a nested timeline
   */
  fun <R: Any, NESTED: SegmentOps<R, NESTED>, RESULT: SegmentOps<R, RESULT>> flatMapValues(ctor: (Timeline<Segment<R>, RESULT>) -> RESULT, f: (Segment<V>) -> NESTED) =
     unsafeFlatMap(ctor, IDENTITY, false) { it.mapValue(f) }

  /** [(DOC)][map2Values] A simplified version of [map2Values] for operations that don't change the timeline type. */
  fun <OTHER: SegmentOps<V, OTHER>> map2Values(other: SegmentOps<V, OTHER>, op: (V, V, Interval) -> V?) = map2Values(ctor, other, op)

  /**
   * [(DOC)][map2Values] Performs a local binary operation between two segment-valued timelines.
   *
   * The operation will be evaluated on each pair of segments that overlap, with their
   * intersection supplied as the interval argument to the [operation][op]. The result of
   * the operation is inserted in the result timeline at that intersection.
   *
   * The binary operation may return `null`, which indicates that the result profile should have
   * a gap.
   *
   * The operation is "local", meaning that while the operation is allowed to know when it is
   * being evaluated, it is not allowed to change where the result segment should be placed.
   * For that, consider using [unsafeMap2] or (ideally) shifting the results in a separate operation.
   *
   * @param W the other operand's payload type
   * @param OTHER the other operand's timeline type
   * @param R the result's payload type
   * @param RESULT the result's timeline type
   *
   * @param ctor the result timeline's constructor
   * @param other the other operand timeline
   * @param op a binary operation between the two payload types that produces a maybe-null result
   *
   * @return a new timeline of segments
   */
  fun <W: Any, OTHER: SegmentOps<W, OTHER>, R: Any, RESULT: SegmentOps<R, RESULT>> map2Values(ctor: (Timeline<Segment<R>, RESULT>) -> RESULT, other: SegmentOps<W, OTHER>, op: (V, W, Interval) -> R?) =
      unsafeMap2(ctor, other) { l, r, i -> op(l.value, r.value, i)?.let { Segment(i, it) } }

  /** [(DOC)][flatMap2Values] A simpler version of [flatMap2Values] for operations that don't change the timeline type. */
  fun <OTHER: SegmentOps<V, OTHER>, NESTED: SegmentOps<V, NESTED>> flatMap2Values(other: SegmentOps<V, OTHER>, op: (V, V, Interval) -> NESTED?) =
      flatMap2Values(ctor, other, op)

  /**
   * [(DOC)][flatMap2Values] Performs a local binary operation that produces profiles, and flattens it.
   *
   * Similar to [map2Values], except it expects the operation to return a profile. Each nested profile
   * is then collected on the interval it corresponds to, and the results are concatenated into a single profile.
   *
   * This is useful for binary operations where at least one of the operand segments represents a value that
   * varies within the segment, such as [gov.nasa.jpl.aerie.timeline.collections.profiles.Real].
   *
   * @param W the other operand's payload type
   * @param OTHER the other operand's timeline type
   * @param R the result's payload type
   * @param NESTED the nested profile type returned by the operation before flattening
   * @param RESULT the result's timeline type
   *
   * @param ctor the result timeline's constructor
   * @param other the other operand timeline
   * @param op a binary operation between the two payload types that produces a maybe-null profile
   *
   * @return a coalesced flattened profile; an instance of the return type of [ctor]
   *
   * @see map2Values
   */
  fun <W: Any, OTHER: SegmentOps<W, OTHER>, R: Any, NESTED: SegmentOps<R, NESTED>, RESULT: SegmentOps<R, RESULT>> flatMap2Values(ctor: (Timeline<Segment<R>, RESULT>) -> RESULT, other: SegmentOps<W, OTHER>, op: (V, W, Interval) -> NESTED?) =
      unsafeOperate(ctor) { opts ->
        map2ParallelLists(collect(opts), other.collect(opts), isAlwaysSorted(), other.isAlwaysSorted()) { l, r, i ->
          op(l.value, r.value, i)?.let { Segment(i, it) }
        }.flatMap { it.value.collect(CollectOptions(it.interval, true)) }
      }
}
