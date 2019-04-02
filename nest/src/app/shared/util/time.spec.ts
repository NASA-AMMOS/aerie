/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { getMaxTimeRange } from './time';

describe('time.ts', () => {
  describe('getMaxTimeRange', () => {
    it(`should properly calculate the max time range for a list of values with no duration`, () => {
      const arr = [
        {
          start: 30,
        },
        {
          start: 10,
        },
        {
          start: 5,
        },
        {
          start: 6,
        },
      ];
      expect(getMaxTimeRange(arr)).toEqual({
        end: 30,
        start: 5,
      });
    });

    it(`should properly calculate the max time range for a list of values with a duration`, () => {
      const arr = [
        {
          duration: 70,
          end: 100,
          start: 30,
        },
        {
          duration: 190,
          end: 200,
          start: 10,
        },
        {
          duration: 45,
          end: 50,
          start: 5,
        },
        {
          duration: 394,
          end: 400,
          start: 6,
        },
      ];
      expect(getMaxTimeRange(arr)).toEqual({
        end: 400,
        start: 5,
      });
    });
  });
});
