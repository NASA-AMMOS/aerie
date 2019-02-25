/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  getActivityPointInBand,
  getColorFromActivityMetadata,
  getMaxTimeRange,
  getPoint,
  getUniqueActivityId,
  updateSelectedPoint,
} from './points';

import { activityPoint, bands } from '../mocks';

describe('points.ts', () => {
  describe('getActivityPointInBand', () => {
    it(`should return a correct an activity point with a given activityId`, () => {
      expect(getActivityPointInBand(bands, 'test-activity-point')).toEqual({
        activityPoint,
        bandId: '104',
        subBandId: '4',
      });
    });
  });
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
    it(`should return null given an unknown band id, sub-band id and an unknown unique point id`, () => {
      expect(getPoint(bands, '42', '42', '42')).toEqual(null);
    });

    it(`should return null given an unknown band id`, () => {
      expect(getPoint(bands, '42', '4', '400')).toEqual(null);
    });

    it(`should return null given an unknown sub-band id`, () => {
      expect(getPoint(bands, '104', '42', '400')).toEqual(null);
    });

    it(`should return null given an unknown unique point id`, () => {
      expect(getPoint(bands, '104', '4', '42')).toEqual(null);
    });

    it(`should get the correct point given the band id and unique point id`, () => {
      expect(getPoint(bands, '104', '4', '400')).toEqual({
        ...bands[4].subBands[0].points[0],
      });
    });
  });

  describe('getColorFromActivityMetadata', () => {
    it(`should return [66, 130, 198 ] for Dodger Blue`, () => {
      expect(
        getColorFromActivityMetadata([{ Name: 'color', Value: 'Dodger Blue' }]),
      ).toEqual([66, 130, 198]);
    });

    it(`should return [255, 0, 0 ] for #ff0000`, () => {
      expect(
        getColorFromActivityMetadata([{ Name: 'color', Value: '#ff0000' }]),
      ).toEqual([255, 0, 0]);
    });

    it(`should return [255, 255, 198 ] for [255, 255, 198]`, () => {
      expect(
        getColorFromActivityMetadata([
          { Name: 'color', Value: [255, 255, 198] },
        ]),
      ).toEqual([255, 255, 198]);
    });
  });

  describe('getUniqueActivityId', () => {
    it(`should return a correct unique activity id`, () => {
      expect(getUniqueActivityId(activityPoint)).toEqual(
        'test-activity-point-0-500',
      );
    });
  });

  describe('updateSelectedPoint', () => {
    it(`should return null if no bands are given`, () => {
      expect(updateSelectedPoint([], activityPoint)).toEqual({
        selectedPoint: null,
      });
    });

    it(`should return null if no selected point is given`, () => {
      expect(updateSelectedPoint([], null)).toEqual({
        selectedPoint: null,
      });
    });

    it(`should return the given point if the given selected point is in the given bands array`, () => {
      const selectedPoint = {
        ...activityPoint,
        sourceId: '/a/b/c/d/e/v',
        subBandId: '4',
        uniqueId: '400',
      };

      expect(updateSelectedPoint(bands, selectedPoint)).toEqual({
        selectedPoint,
      });
    });
  });
});
