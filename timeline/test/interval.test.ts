import { Inclusivity, Interval } from '../src/interval';
import { Temporal } from '@js-temporal/polyfill';
import './jest.setup';
import Duration = Temporal.Duration;

const dur = (m: number) => Duration.from({ minutes: m });
const between = Interval.Between;
const at = Interval.At;

test(Inclusivity.opposite.name, () => {
  expect(Inclusivity.opposite(Inclusivity.Inclusive)).toEqual(Inclusivity.Exclusive);
});

test(Inclusivity.compareRestrictiveness.name, () => {
  expect(Inclusivity.compareRestrictiveness(Inclusivity.Inclusive, Inclusivity.Exclusive)).toEqual(-1);
  expect(Inclusivity.compareRestrictiveness(Inclusivity.Exclusive, Inclusivity.Exclusive)).toEqual(0);
});

test(Interval.At.name, () => {
  const time = dur(45);

  const i = at(time);
  expect(i.start).toEqual(time);
  expect(i.end).toEqual(time);
  expect(i.startInclusivity).toEqual(Inclusivity.Inclusive);
  expect(i.endInclusivity).toEqual(Inclusivity.Inclusive);
});

test(Interval.Between.name, () => {
  const t1 = dur(1);
  const t2 = dur(5);

  let i = between(t1, t2);
  expect(i.start).toEqual(t1);
  expect(i.end).toEqual(t2);
  expect(i.startInclusivity).toEqual(Inclusivity.Inclusive);
  expect(i.endInclusivity).toEqual(Inclusivity.Inclusive);

  i = between(t1, t2, Inclusivity.Exclusive);
  expect(i.start).toEqual(t1);
  expect(i.end).toEqual(t2);
  expect(i.startInclusivity).toEqual(Inclusivity.Exclusive);
  expect(i.endInclusivity).toEqual(Inclusivity.Exclusive);

  i = between(t1, t2, Inclusivity.Inclusive, Inclusivity.Exclusive);
  expect(i.start).toEqual(t1);
  expect(i.end).toEqual(t2);
  expect(i.startInclusivity).toEqual(Inclusivity.Inclusive);
  expect(i.endInclusivity).toEqual(Inclusivity.Exclusive);
});

test(Interval.prototype.isEmpty.name, () => {
  expect(at(dur(5)).isEmpty()).toBeFalsy();
  expect(between(dur(1), dur(2)).isEmpty()).toBeFalsy();
  expect(between(dur(2), dur(1)).isEmpty()).toBeTruthy();
  expect(between(dur(1), dur(1), Inclusivity.Inclusive, Inclusivity.Exclusive).isEmpty()).toBeTruthy();
});

test(Interval.prototype.isSingleton.name, () => {
  expect(at(dur(1)).isSingleton()).toBeTruthy();
  expect(between(dur(1), dur(2)).isSingleton()).toBeFalsy();
  expect(between(dur(2), dur(1)).isSingleton()).toBeFalsy();
});

test(Interval.prototype.duration.name, () => {
  expect(between(dur(1), dur(2)).duration()).toEqual(dur(1));
});

test(Interval.intersect.name, () => {
  expect(Interval.intersect(between(dur(1), dur(2)), between(dur(0), dur(4)))).toEqual(between(dur(1), dur(2)));
  expect(Interval.intersect(between(dur(0), dur(2)), between(dur(1), dur(3)))).toEqual(between(dur(1), dur(2)));
  expect(Interval.intersect(between(dur(0), dur(2), Inclusivity.Exclusive), between(dur(1), dur(3)))).toEqual(
    between(dur(1), dur(2), Inclusivity.Inclusive, Inclusivity.Exclusive)
  );
  expect(Interval.intersect(between(dur(0), dur(1)), between(dur(3), dur(4))).isEmpty()).toBeTruthy();
});

test(Interval.union.name, () => {
  expect(Interval.union(between(dur(0), dur(2)), at(dur(1)))).toEqual(between(dur(0), dur(2)));
  expect(Interval.union(between(dur(0), dur(1)), between(dur(1), dur(2)))).toEqual(between(dur(0), dur(2)));
  expect(Interval.union(between(dur(0), dur(1), Inclusivity.Exclusive), between(dur(1), dur(2)))).toEqual(
    between(dur(0), dur(2), Inclusivity.Exclusive, Inclusivity.Inclusive)
  );
  expect(Interval.union(between(dur(0), dur(1)), at(dur(4)))).toBeUndefined();
});

test(Interval.subtract.name, () => {
  expect(Interval.subtract(between(dur(0), dur(2)), at(dur(1)))).toEqual([
    between(dur(0), dur(1), Inclusivity.Inclusive, Inclusivity.Exclusive),
    between(dur(1), dur(2), Inclusivity.Exclusive, Inclusivity.Inclusive)
  ]);
  expect(Interval.subtract(between(dur(0), dur(1)), between(dur(2), dur(3)))).toEqual([between(dur(0), dur(1))]);
});

test(Interval.compareStarts.name, () => {
  expect(Interval.compareStarts(between(dur(0), dur(1)), at(dur(1)))).toEqual(-1);
  expect(Interval.compareStarts(between(dur(0), dur(1), Inclusivity.Exclusive), at(dur(0)))).toEqual(1);
  expect(Interval.compareStarts(between(dur(0), dur(1)), at(dur(0)))).toEqual(0);
});

test(Interval.compareEnds.name, () => {
  expect(Interval.compareEnds(between(dur(0), dur(1)), at(dur(0)))).toEqual(1);
  expect(Interval.compareEnds(between(dur(0), dur(1), Inclusivity.Exclusive), at(dur(1)))).toEqual(-1);
  expect(Interval.compareEnds(between(dur(0), dur(1)), at(dur(1)))).toEqual(0);
});

test(Interval.compareEndToStart.name, () => {
  expect(Interval.compareEndToStart(at(dur(0)), at(dur(1)))).toEqual(-1);
  expect(Interval.compareEndToStart(at(dur(0)), between(dur(0), dur(1), Inclusivity.Exclusive))).toEqual(0);
  expect(Interval.compareEndToStart(between(dur(0), dur(2)), between(dur(1), dur(4)))).toEqual(1);
});

test(Interval.prototype.contains.name, () => {
  expect(between(dur(0), dur(2)).contains(dur(1))).toBeTruthy();
  expect(between(dur(0), dur(2)).contains(dur(3))).toBeFalsy();
  expect(between(dur(0), dur(2)).contains(at(dur(1)))).toBeTruthy();
  expect(between(dur(0), dur(2)).contains(at(dur(3)))).toBeFalsy();
  expect(between(dur(0), dur(2)).contains(between(dur(1), dur(3)))).toBeFalsy();
  expect(between(dur(0), dur(2)).contains(between(dur(1), dur(2)))).toBeTruthy();
});

test(Interval.prototype.shiftBy.name, () => {
  expect(between(dur(0), dur(1)).shiftBy(dur(1))).toEqual(between(dur(1), dur(2)));
  expect(at(dur(2)).shiftBy(dur(-1))).toEqual(at(dur(1)));
  expect(between(dur(0), dur(4)).shiftBy(dur(1), dur(-1))).toEqual(between(dur(1), dur(3)));
  expect(between(dur(0), dur(2)).shiftBy(dur(1), dur(-1))).toEqual(at(dur(1)));
  expect(between(dur(0), dur(1)).shiftBy(dur(2), dur(0)).isEmpty()).toBeTruthy();
});
