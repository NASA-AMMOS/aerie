package gov.nasa.jpl.aerie.timeline.util

import gov.nasa.jpl.aerie.timeline.NullBinaryOperation
import gov.nasa.jpl.aerie.timeline.Duration.Companion.seconds
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.Interval.Companion.between
import gov.nasa.jpl.aerie.timeline.Interval.Inclusivity.*
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested

class Map2Test {

  @Test
  fun basicCombineOrIdentity() {
    val left = listOf(Segment(seconds(0) .. seconds(2), 2))
    val right = listOf(Segment(seconds(1) .. seconds(3), 3))

    val result = map2SegmentLists(
        left, right,
        NullBinaryOperation.combineOrIdentity { l, r, _ -> l + r}
    )

    val expected = listOf(
        Segment(seconds(0) ..< seconds(1), 2),
        Segment(seconds(1) .. seconds(2), 5),
        Segment(between(seconds(2), seconds(3), Exclusive, Inclusive), 3),
    )

    assertIterableEquals(expected, result)
  }

  @Test
  fun basicCombineOrUndefined() {
    val left = listOf(Segment(seconds(0) .. seconds(2), 2))
    val right = listOf(Segment(seconds(1) .. seconds(3), 3))

    val result = map2SegmentLists(
        left, right,
        NullBinaryOperation.combineOrNull { l, r, _ -> l + r}
    )

    val expected = listOf(
        Segment(seconds(1) .. seconds(2), 5)
    )

    assertIterableEquals(expected, result)
  }

  @Nested
  inner class SegmentAlignment {

    // Helper functions for below
    val op = NullBinaryOperation.combineOrIdentity<Int> { l, r, _ -> l + r }
    fun makeLeft(s: Long, e: Long, si: Interval.Inclusivity = Inclusive, ei: Interval.Inclusivity = Inclusive) =
        listOf(Segment(between(seconds(s), seconds(e), si, ei), -1))

    fun makeRight(s: Long, e: Long, si: Interval.Inclusivity = Inclusive, ei: Interval.Inclusivity = Inclusive) =
        listOf(Segment(between(seconds(s), seconds(e), si, ei), 1))

    @Test
    fun identical() {
      assertIterableEquals(
          listOf(Segment(seconds(1) .. seconds(2), 0)),
          map2SegmentLists(makeLeft(1, 2), makeRight(1, 2), op)
      )
    }

    @Test
    fun identicalExclusive() {
      assertIterableEquals(
          listOf(Segment(between(seconds(1), seconds(2), Exclusive, Exclusive), 0)),
          map2SegmentLists(makeLeft(1, 2, Exclusive, Exclusive), makeRight(1, 2, Exclusive, Exclusive), op)
      )
    }

    @Test
    fun entireLeftSegmentFirst() {
      assertIterableEquals(
          listOf(
              Segment(seconds(1) .. seconds(2), -1),
              Segment(seconds(3) .. seconds(4), 1)
          ),
          map2SegmentLists(makeLeft(1, 2), makeRight(3, 4), op)
      )
    }

    @Test
    fun entireRightSegmentFirst() {
      assertIterableEquals(
          listOf(
              Segment(seconds(1) .. seconds(2), 1),
              Segment(seconds(3) .. seconds(4), -1)
          ),
          map2SegmentLists(makeLeft(3, 4), makeRight(1, 2), op)
      )
    }

    @Test
    fun leftFirstMomentOfOverlap() {
      assertIterableEquals(
          listOf(
              Segment(between(seconds(1), seconds(2), endInclusivity = Exclusive), -1),
              Segment(Interval.at(seconds(2)), 0),
              Segment(between(seconds(2), seconds(3), Exclusive, Inclusive), 1)
          ),
          map2SegmentLists(makeLeft(1, 2), makeRight(2, 3), op)
      )
    }

    @Test
    fun rightFirstMomentOfOverlap() {
      assertIterableEquals(
          listOf(
              Segment(between(seconds(1), seconds(2), endInclusivity = Exclusive), 1),
              Segment(Interval.at(seconds(2)), 0),
              Segment(between(seconds(2), seconds(3), Exclusive, Inclusive), -1)
          ),
          map2SegmentLists(makeLeft(2, 3), makeRight(1, 2), op)
      )
    }

    @Test
    fun leftFirstMomentOfNonOverlap() {
      assertIterableEquals(
          listOf(
              Segment(Interval.at(seconds(1)), -1),
              Segment(between(seconds(1), seconds(2), Exclusive, Inclusive), 0)
          ),
          map2SegmentLists(makeLeft(1, 2), makeRight(1, 2, Exclusive, Inclusive), op)
      )
    }

    @Test
    fun rightFirstMomentOfNonOverlap() {
      assertIterableEquals(
          listOf(
              Segment(Interval.at(seconds(1)), 1),
              Segment(between(seconds(1), seconds(2), Exclusive, Inclusive), 0)
          ),
          map2SegmentLists(makeLeft(1, 2, Exclusive, Inclusive), makeRight(1, 2), op)
      )
    }

    @Test
    fun leftFirstHalfNonOverlap() {
      assertIterableEquals(
          listOf(
              Segment(seconds(1) ..< seconds(2), -1),
              Segment(seconds(2) .. seconds(3), 0),
              Segment(between(seconds(3), seconds(4), Exclusive, Inclusive), 1),
          ),
          map2SegmentLists(makeLeft(1, 3), makeRight(2, 4), op)
      )
    }

    @Test
    fun rightFirstHalfNonOverlap() {
      assertIterableEquals(
          listOf(
              Segment(seconds(1) ..< seconds(2), 1),
              Segment(seconds(2) .. seconds(3), 0),
              Segment(between(seconds(3), seconds(4), Exclusive, Inclusive), -1),
          ),
          map2SegmentLists(makeLeft(2, 4), makeRight(1, 3), op)
      )
    }

    @Test
    fun leftContainsRight() {
      assertIterableEquals(
          listOf(
              Segment(seconds(1) ..< seconds(2), -1),
              Segment(seconds(2) .. seconds(3), 0),
              Segment(between(seconds(3), seconds(4), Exclusive, Inclusive), -1),
          ),
          map2SegmentLists(makeLeft(1, 4), makeRight(2, 3), op)
      )
    }

    @Test
    fun rightContainsLeft() {
      assertIterableEquals(
          listOf(
              Segment(seconds(1) ..< seconds(2), 1),
              Segment(seconds(2) .. seconds(3), 0),
              Segment(between(seconds(3), seconds(4), Exclusive, Inclusive), 1),
          ),
          map2SegmentLists(makeLeft(2, 3), makeRight(1, 4), op)
      )
    }
  }
}
