import { Temporal } from '@js-temporal/polyfill';
import Duration = Temporal.Duration;

test('duration equality jest helper', () => {
  expect(Duration.from({ seconds: 5 })).toEqual(Duration.from({ milliseconds: 5000 }));
  expect(Duration.from({ seconds: 5 })).not.toEqual(Duration.from({ seconds: 10 }));
});
