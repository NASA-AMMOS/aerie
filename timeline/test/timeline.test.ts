import { Inclusivity, Interval } from '../src/interval.js';
import { Temporal } from '@js-temporal/polyfill';
import { Segment } from '../src/segment.js';
import { bound, coalesce, isLazy, LazyTimeline } from '../src/timeline.js';
import { ProfileType } from '../src/profiles/profile-type.js';
import Duration = Temporal.Duration;

const dur = (m: number) => Duration.from({ minutes: m });
const between = Interval.Between;
const at = Interval.At;

describe('bound', () => {
  const bounds = between(dur(0), dur(10));
  test('bound array', async () => {
    const input = [new Segment(true, between(dur(4), dur(5)))];
    const bounded = bound(input);
    expect(isLazy(bounded)).toBeTruthy();
    const result = await (bounded as LazyTimeline<Segment<boolean>>)(bounds);

    expect(result).toEqual(input);
  });
});

describe('coalesce', () => {
  test('does nothing on coalesced array', () => {
    const supplier = () => [
      new Segment(false, between(dur(0), dur(1), Inclusivity.Inclusive, Inclusivity.Exclusive)),
      new Segment(true, between(dur(1), dur(2), Inclusivity.Inclusive, Inclusivity.Inclusive)),
      new Segment(false, between(dur(2), dur(3), Inclusivity.Exclusive, Inclusivity.Inclusive))
    ];

    expect(coalesce(supplier(), ProfileType.Windows)).toEqual(supplier());
  });
});
