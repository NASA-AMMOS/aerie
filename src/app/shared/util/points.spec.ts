/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  getMaxTimeRange,
  getPoint,
  updateSelectedPoint,
} from './points';

import {
  activityPoint,
  bands,
} from './../mocks';

describe('points.ts', () => {
  describe('getMaxTimeRange', () => {
    it(`should properly calculate the max time range for a list of points`, () => {
      const points = [
        {
          ...activityPoint,
          end: 100,
          start: 30,
        },
        {
          ...activityPoint,
          end: 200,
          start: 10,
        },
        {
          ...activityPoint,
          end: 50,
          start: 5,
        },
        {
          ...activityPoint,
          end: 400,
          start: 6,
        },
      ];
      expect(getMaxTimeRange(points)).toEqual({
        end: 400,
        start: 5,
      });
    });
  });

  describe('getPoint', () => {
    it(`should return null given an unknown band id and an unknown unique point id`, () => {
      expect(getPoint(bands, '42', '42')).toEqual(null);
    });

    it(`should return null given an unknown band id`, () => {
      expect(getPoint(bands, '42', '400')).toEqual(null);
    });

    it(`should return null given an unknown unique point id`, () => {
      expect(getPoint(bands, '104', '42')).toEqual(null);
    });

    it(`should get the correct point given the band id and unique point id`, () => {
      expect(getPoint(bands, '104', '400')).toEqual({
        ...bands[4].subBands[0].points[0],
      });
    });
  });

  describe('updateSelectedPoint', () => {
    it(`should return null if the point has the source id`, () => {
      expect(updateSelectedPoint(activityPoint, '/a/b/c/', '')).toEqual({
        selectedPoint: null,
      });
    });

    it(`should return null if the point has the sub-band id`, () => {
      expect(updateSelectedPoint(activityPoint, '', '4')).toEqual({
        selectedPoint: null,
      });
    });

    it(`should return the given point if the given source id or sub-band id are not associated with the point`, () => {
      expect(updateSelectedPoint(activityPoint, '', '')).toEqual({
        selectedPoint: activityPoint,
      });
    });
  });
});
