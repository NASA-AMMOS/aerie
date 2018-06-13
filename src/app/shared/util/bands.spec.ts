/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  bandById,
  getCustomFiltersBySourceId,
  hasActivityBand,
  hasSourceId,
  isAddTo,
  isMessageTypeActivity,
  isOverlay,
  subBandById,
  updateSelectedBandIds,
  updateSortOrder,
  updateTimeRanges,
} from './bands';

import {
  activityPoint,
  bands,
  bandsWithCustomFiltersInSourceId,
  keywordLineActivityPoint,
  messageTypeActivityPoint,
  treeBySourceId,
} from './../mocks';

describe('bands.ts', () => {
  describe('bandById', () => {
    it(`should return null if band id does not exist`, () => {
      expect(bandById(bands, '42')).toBe(null);
    });

    it(`should return the band given the correct band id`, () => {
      expect(bandById(bands, '100')).toEqual({
        ...bands[0],
      });
    });
  });

  describe('getCustomFiltersBySourceId', () => {
    it(`should return custom filters from sourceIds in bands`, () => {
      expect(getCustomFiltersBySourceId(bandsWithCustomFiltersInSourceId, treeBySourceId)).toEqual({
        '/DKF/command': [{
          filter: '.*',
          label: 'ips',
        }],
      });
    });
  });

  describe('hasActivityBand', () => {
    it(`should return null if the given band is not an activity band`, () => {
      const stateBand = bands[1].subBands[0];
      expect(hasActivityBand(bands, stateBand)).toBe(null);
    });

    it(`should return the first found sub-band if it is an activity by-type sub-band with the same legend as the given band`, () => {
      const byTypeActivityBand = bands[4].subBands[0];
      expect(hasActivityBand(bands, byTypeActivityBand)).toEqual({
        bandId: '100',
        subBandId: '0',
      });
    });
  });

  describe('hasSourceId', () => {
    it(`should return null if a given source id does not exist`, () => {
      expect(hasSourceId(bands, '/wtf')).toBe(null);
    });

    it(`should return a sub-band locator for the first sub-band that has the given source id`, () => {
      expect(hasSourceId(bands, '/a/b/c/d/e/w')).toEqual({
        bandId: '100',
        subBandId: '0',
      });
    });
  });

  describe('isAddTo', () => {
    it(`should return false if band id does not exist`, () => {
      expect(isAddTo(bands, '42', '0', 'activity')).toBe(false);
    });

    it(`should return false if the sub-band id does not exist`, () => {
      expect(isAddTo(bands, '100', '42', 'activity')).toBe(false);
    });

    it(`should return false if the type is wrong`, () => {
      expect(isAddTo(bands, '100', '0', 'state')).toBe(false);
    });

    it(`should return false if the ids and type are correct and the band is NOT in addTo mode`, () => {
      expect(isAddTo(bands, '101', '1', 'state')).toBe(false);
    });

    it(`should return true if the ids and type are correct and the band is in addTo mode`, () => {
      expect(isAddTo(bands, '100', '0', 'activity')).toBe(true);
    });
  });

  describe('isMessageTypeActivity', () => {
    it(`should return true for activityPoint with message and no keywordLine`, () => {
      expect(isMessageTypeActivity(messageTypeActivityPoint)).toBe(true);
    });

    it(`should return false for activityPoint with message and keywordLine`, () => {
      expect(isMessageTypeActivity(keywordLineActivityPoint)).toBe(false);
    });

    it(`should return false for activityPoint with message and keywordLine`, () => {
      expect(isMessageTypeActivity(activityPoint)).toBe(false);
    });
  });

  describe('isOverlay', () => {
    it(`should return false if band id does not exist`, () => {
      expect(isOverlay(bands, '42')).toBe(false);
    });

    it(`should return false if band is not in overlay mode`, () => {
      expect(isOverlay(bands, '101')).toBe(false);
    });

    it(`should return true if band is in overlay mode`, () => {
      expect(isOverlay(bands, '102')).toBe(true);
    });
  });

  describe('subBandById', () => {
    it(`should return null if band id does not exist`, () => {
      expect(subBandById(bands, '42', '0')).toBe(null);
    });

    it(`should return null if sub-band id does not exist`, () => {
      expect(subBandById(bands, '100', '42')).toBe(null);
    });

    it(`should return null if band-id and sub-band id does not exist`, () => {
      expect(subBandById(bands, '42', '42')).toBe(null);
    });

    it(`should return the sub-band given the correct band id and sub-band id`, () => {
      expect(subBandById(bands, '100', '0')).toEqual({
        ...bands[0].subBands[0],
      });
    });
  });

  describe('updateSelectedBandIds', () => {
    it(`should return the original selectedBandId and selectedSubBandId if the band and sub-band exist in the given bands`, () => {
      expect(updateSelectedBandIds(bands, '102', '2')).toEqual({
        selectedBandId: '102',
        selectedSubBandId: '2',
      });
    });

    it(`should return an empty selectedBandId and an empty selectedSubBandId if the band id does not exist in the given bands`, () => {
      expect(updateSelectedBandIds(bands, '42', '2')).toEqual({
        selectedBandId: '',
        selectedSubBandId: '',
      });
    });

    it(`should return the original selectedBandId and new selectedSubBandId if the sub-band id does not exist in the given bands`, () => {
      expect(updateSelectedBandIds(bands, '102', '42')).toEqual({
        selectedBandId: '102',
        selectedSubBandId: '2',
      });
    });
  });

  describe('updateSortOrder', () => {
    it(`should maintain the correct sort order of the given bands`, () => {
      expect(updateSortOrder(bands)).toEqual(bands);
    });
  });

  describe('updateTimeRanges', () => {
    it(`should return empty time ranges when an empty bands array is given`, () => {
      expect(updateTimeRanges([], { end: 0, start: 0 })).toEqual({
        maxTimeRange: { end: 0, start: 0 },
        viewTimeRange: { end: 0, start: 0 },
      });
    });

    it(`should set the viewTimeRange to the maxTimeRange if an empty currentViewTimeRange is given`, () => {
      expect(updateTimeRanges(bands, { end: 0, start: 0 })).toEqual({
        maxTimeRange: { end: 300, start: 10 },
        viewTimeRange: { end: 300, start: 10 },
      });
    });

    it(`should clamp the viewTimeRange.start to the maxTimeRange.start`, () => {
      expect(updateTimeRanges(bands, { end: 100, start: -100 })).toEqual({
        maxTimeRange: { end: 300, start: 10 },
        viewTimeRange: { end: 100, start: 10 },
      });
    });

    it(`should clamp the viewTimeRange.end to the maxTimeRange.end`, () => {
      expect(updateTimeRanges(bands, { end: 600, start: 0 })).toEqual({
        maxTimeRange: { end: 300, start: 10 },
        viewTimeRange: { end: 300, start: 10 },
      });
    });

    it(`should clamp start and end of viewTimeRange to the start and end of maxTimeRange`, () => {
      expect(updateTimeRanges(bands, { end: 600, start: -100 })).toEqual({
        maxTimeRange: { end: 300, start: 10 },
        viewTimeRange: { end: 300, start: 10 },
      });
    });
  });
});
