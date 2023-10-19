import { Inclusivity, map2Arrays } from '../../src/internal.js';
import { Interval, Segment } from '../../src/index.js';
import { Temporal } from '@js-temporal/polyfill';
import { BinaryOperation } from '../../src/binary-operation.js';
import Duration = Temporal.Duration;

const dur = (m: number) => Duration.from({ minutes: m });
const between = Interval.Between;
const at = Interval.At;

test('basic combine or identity', () => {
  const left = [new Segment(2, between(dur(0), dur(2)))];
  const right = [new Segment(3, between(dur(1), dur(3)))];

  const result = map2Arrays(
    left,
    right,
    BinaryOperation.combineOrIdentity((l, r) => l + r)
  );

  expect(result).toEqual([
    new Segment(2, between(dur(0), dur(1), Inclusivity.Inclusive, Inclusivity.Exclusive)),
    new Segment(5, between(dur(1), dur(2))),
    new Segment(3, between(dur(2), dur(3), Inclusivity.Exclusive, Inclusivity.Inclusive))
  ]);
});

test('basic combine or undefined', () => {
  const left = [new Segment(2, between(dur(0), dur(2)))];
  const right = [new Segment(3, between(dur(1), dur(3)))];

  const result = map2Arrays(
    left,
    right,
    BinaryOperation.combineOrUndefined((l, r) => l + r)
  );

  expect(result).toEqual([new Segment(5, between(dur(1), dur(2)))]);
});

describe('map2Arrays segment alignments', () => {
  const op = BinaryOperation.combineOrIdentity<number, number, number>((l, r) => l + r);
  const makeLeft = (
    s: number,
    e: number,
    si: Inclusivity = Inclusivity.Inclusive,
    ei: Inclusivity = Inclusivity.Inclusive
  ) => [new Segment(-1, between(dur(s), dur(e), si, ei))];
  const makeRight = (
    s: number,
    e: number,
    si: Inclusivity = Inclusivity.Inclusive,
    ei: Inclusivity = Inclusivity.Inclusive
  ) => [new Segment(1, between(dur(s), dur(e), si, ei))];

  test('identical', () => {
    expect(map2Arrays(makeLeft(1, 2), makeRight(1, 2), op)).toEqual([new Segment(0, between(dur(1), dur(2)))]);
  });

  test('identical exclusive', () => {
    expect(
      map2Arrays(
        makeLeft(1, 2, Inclusivity.Exclusive, Inclusivity.Exclusive),
        makeRight(1, 2, Inclusivity.Exclusive, Inclusivity.Exclusive),
        op
      )
    ).toEqual([new Segment(0, between(dur(1), dur(2), Inclusivity.Exclusive))]);
  });

  test('entire left segment first', () => {
    expect(map2Arrays(makeLeft(1, 2), makeRight(3, 4), op)).toEqual([
      new Segment(-1, between(dur(1), dur(2))),
      new Segment(1, between(dur(3), dur(4)))
    ]);
  });

  test('entire right segment first', () => {
    expect(map2Arrays(makeLeft(3, 4), makeRight(1, 2), op)).toEqual([
      new Segment(1, between(dur(1), dur(2))),
      new Segment(-1, between(dur(3), dur(4)))
    ]);
  });

  test('left first, moment of overlap', () => {
    expect(map2Arrays(makeLeft(1, 2), makeRight(2, 3), op)).toEqual([
      new Segment(-1, between(dur(1), dur(2), Inclusivity.Inclusive, Inclusivity.Exclusive)),
      new Segment(0, at(dur(2))),
      new Segment(1, between(dur(2), dur(3), Inclusivity.Exclusive, Inclusivity.Inclusive))
    ]);
  });

  test('right first, moment of overlap', () => {
    expect(map2Arrays(makeLeft(2, 3), makeRight(1, 2), op)).toEqual([
      new Segment(1, between(dur(1), dur(2), Inclusivity.Inclusive, Inclusivity.Exclusive)),
      new Segment(0, at(dur(2))),
      new Segment(-1, between(dur(2), dur(3), Inclusivity.Exclusive, Inclusivity.Inclusive))
    ]);
  });

  test('left first, moment of non-overlap', () => {
    expect(map2Arrays(makeLeft(1, 2), makeRight(1, 2, Inclusivity.Exclusive, Inclusivity.Inclusive), op)).toEqual([
      new Segment(-1, at(dur(1))),
      new Segment(0, between(dur(1), dur(2), Inclusivity.Exclusive, Inclusivity.Inclusive))
    ]);
  });

  test('right first, moment of non-overlap', () => {
    expect(map2Arrays(makeLeft(1, 2, Inclusivity.Exclusive, Inclusivity.Inclusive), makeRight(1, 2), op)).toEqual([
      new Segment(1, at(dur(1))),
      new Segment(0, between(dur(1), dur(2), Inclusivity.Exclusive, Inclusivity.Inclusive))
    ]);
  });

  test('left first, half overlap', () => {
    expect(map2Arrays(makeLeft(1, 3), makeRight(2, 4), op)).toEqual([
      new Segment(-1, between(dur(1), dur(2), Inclusivity.Inclusive, Inclusivity.Exclusive)),
      new Segment(0, between(dur(2), dur(3))),
      new Segment(1, between(dur(3), dur(4), Inclusivity.Exclusive, Inclusivity.Inclusive))
    ]);
  });

  test('right first, half overlap', () => {
    expect(map2Arrays(makeLeft(2, 4), makeRight(1, 3), op)).toEqual([
      new Segment(1, between(dur(1), dur(2), Inclusivity.Inclusive, Inclusivity.Exclusive)),
      new Segment(0, between(dur(2), dur(3))),
      new Segment(-1, between(dur(3), dur(4), Inclusivity.Exclusive, Inclusivity.Inclusive))
    ]);
  });

  test('left contains right', () => {
    expect(map2Arrays(makeLeft(1, 4), makeRight(2, 3), op)).toEqual([
      new Segment(-1, between(dur(1), dur(2), Inclusivity.Inclusive, Inclusivity.Exclusive)),
      new Segment(0, between(dur(2), dur(3))),
      new Segment(-1, between(dur(3), dur(4), Inclusivity.Exclusive, Inclusivity.Inclusive))
    ]);
  });

  test('right contains left', () => {
    expect(map2Arrays(makeLeft(2, 3), makeRight(1, 4), op)).toEqual([
      new Segment(1, between(dur(1), dur(2), Inclusivity.Inclusive, Inclusivity.Exclusive)),
      new Segment(0, between(dur(2), dur(3))),
      new Segment(1, between(dur(3), dur(4), Inclusivity.Exclusive, Inclusivity.Inclusive))
    ]);
  });
});
