import { Segment } from '../src/segment.js';
import { Interval } from '../src/interval.js';
import { Temporal } from '@js-temporal/polyfill';
import Duration = Temporal.Duration;

const seg = Segment.Of;
const dur = (m: number) => Duration.from({ minutes: m });
const at = Interval.At;

test(Segment.prototype.transpose.name, () => {
  expect(seg(5 as number | undefined, at(dur(2))).transpose()).toEqual(seg(5, at(dur(2))));
  expect(seg(undefined as number | undefined, at(dur(2))).transpose()).toBeUndefined();
});
