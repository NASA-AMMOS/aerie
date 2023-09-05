import { expect } from '@jest/globals';
import { Temporal } from '@js-temporal/polyfill';
import Duration = Temporal.Duration;

function areDurationsEqual(a: unknown, b: unknown): boolean | undefined {
  const isAVolume = a instanceof Duration;
  const isBVolume = b instanceof Duration;

  if (isAVolume && isBVolume) {
    return Duration.compare(a, b) === 0;
  } else if (isAVolume !== isBVolume) {
    return false;
  } else {
    return undefined;
  }
}

expect.addEqualityTesters([areDurationsEqual]);
