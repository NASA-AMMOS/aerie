/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  getAllChildIds,
  getAllSourcesByKind,
  getParentSourceIds,
  getSourceIds,
  getSourceType,
  updateSourceId,
} from './source';

import {
  bands,
  treeBySourceId,
} from './../mocks/';

describe('source.ts', () => {
  describe('getAllChildIds', () => {
    it(`should return a list of all child ids (recursively) for a given source id`, () => {
      expect(getAllChildIds(treeBySourceId, '/')).toEqual([
        '/child/0',
        '/child/1',
        '/child/child/0',
      ]);

      expect(getAllChildIds(treeBySourceId, '/child/1')).toEqual([
        '/child/child/0',
      ]);
    });

    it(`should return an empty list for a source id that does not exist in the tree`, () => {
      expect(getAllChildIds(treeBySourceId, 'nonExistentId')).toEqual([]);
    });
  });

  describe('getAllSourcesByKind', () => {
    it(`should return no sources for no kind`, () => {
      expect(getAllSourcesByKind(treeBySourceId, '/', '42')).toEqual([]);
    });

    it(`should get all sources for an existing kind`, () => {
      expect(getAllSourcesByKind(treeBySourceId, '/', 'file')).toEqual([
        treeBySourceId['/child/0'],
        treeBySourceId['/child/1'],
      ]);
    });

    it(`should get a single source for an existing kind`, () => {
      expect(getAllSourcesByKind(treeBySourceId, '/', 'data')).toEqual([
        treeBySourceId['/child/child/0'],
      ]);
    });

    it(`should get a single source for an existing kind when we dont start at the root source`, () => {
      expect(getAllSourcesByKind(treeBySourceId, '/child/1', 'data')).toEqual([
        treeBySourceId['/child/child/0'],
      ]);
    });
  });

  describe('getParentSourceIds', () => {
    it(`should split a source correctly into it's parent source ids, in the correct order`, () => {
      expect(getParentSourceIds('/hello/world/goodbye/what/is/going/on')).toEqual([
        '/hello',
        '/hello/world',
        '/hello/world/goodbye',
        '/hello/world/goodbye/what',
        '/hello/world/goodbye/what/is',
        '/hello/world/goodbye/what/is/going',
      ]);
    });
  });

  describe('getSourceIds', () => {
    it(`should split sources from a list of bands into parent source ids and leaf source ids`, () => {
      expect(getSourceIds(bands)).toEqual({
        parentSourceIds: [
          '/a',
          '/a/b',
          '/a/b/c',
          '/a/b/c/d',
          '/a/b/c/d/e',
          '/a/b/c/d/e/x',
        ],
        sourceIds: [
          '/a/b/c/d/e/w',
          '/a/b/c/d/e/x/y',
          '/a/b/c/d/e/x/z',
          '/a/b/c/d/e/u',
          '/a/b/c/d/e/v',
        ],
      });
    });
  });

  describe('getSourceType', () => {
    it(`should properly identify an 'Activities by Legend'`, () => {
      expect(getSourceType('/hello/Activities by Legend/goodbye/')).toEqual('byLegend');
    });

    it(`should properly identify an 'Activities by Type'`, () => {
      expect(getSourceType('/hello/Activities by Type/goodbye/')).toEqual('byType');
    });

    it(`should return empty string for an unknown source type`, () => {
      expect(getSourceType('hello, world!')).toEqual('');
    });
  });

  describe('updateSourceId', () => {
    it(`should update a simple source id with a simple base id`, () => {
      expect(updateSourceId('/a/b/c/d', '/x/y')).toEqual('/x/y/c/d');
    });

    it(`should update an actual source id with an actual base id`, () => {
      const sourceId = updateSourceId(
        '/leucadia/EuropaSimulations/15F10_Cruise_Simulation_CheckoutActivities/Resources/Array/ArrayTrackingMode',
        '/leucadia/EuropaSimulations/15F10_Tour_Simulation_2016_265T23_32_16',
      );
      const expected = '/leucadia/EuropaSimulations/15F10_Tour_Simulation_2016_265T23_32_16/Resources/Array/ArrayTrackingMode';
      expect(sourceId).toEqual(expected);
    });

    it(`if the base id is as long as the source id, then the entire source id should be replaced`, () => {
      const sourceId = updateSourceId(
        '/leucadia/EuropaSimulations/15F10_Cruise_Simulation_CheckoutActivities/Resources/Array/ArrayTrackingMode',
        '/leucadia/EuropaSimulations/15F10_Tour_Simulation_2016_265T23_32_16/Resources/Array/SolarArrayFlopCount',
      );
      const expected = '/leucadia/EuropaSimulations/15F10_Tour_Simulation_2016_265T23_32_16/Resources/Array/SolarArrayFlopCount';
      expect(sourceId).toEqual(expected);
    });

    it(`if the base id is longer than the source id, then source id should be replaced but not more than it's original length`, () => {
      const sourceId = updateSourceId(
        '/leucadia/EuropaSimulations/15F10_Cruise_Simulation_CheckoutActivities/Resources/Array/ArrayTrackingMode',
        '/leucadia/EuropaSimulations/15F10_Tour_Simulation_2016_265T23_32_16/Resources/Array/SolarArrayFlopCount/some/more/stuff',
      );
      const expected = '/leucadia/EuropaSimulations/15F10_Tour_Simulation_2016_265T23_32_16/Resources/Array/SolarArrayFlopCount';
      expect(sourceId).toEqual(expected);
    });
  });
});
