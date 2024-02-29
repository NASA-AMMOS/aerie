package gov.nasa.jpl.aerie.timeline.ops

import gov.nasa.jpl.aerie.timeline.*
import gov.nasa.jpl.aerie.timeline.BoundsTransformer.Companion.IDENTITY
import gov.nasa.jpl.aerie.timeline.payloads.Segment

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
}
