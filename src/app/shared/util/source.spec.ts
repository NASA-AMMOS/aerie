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
  getCustomFilterForLabel,
  getFormattedSourceUrl,
  getParentSourceIds,
  getPin,
  getPinLabel,
  getQueryOptionsForGraphableFilter,
  getSourceIds,
  getTargetFilters,
  toRavenCustomMetadata,
  toRavenFileMetadata,
  updateSourceId,
} from './source';

import {
  bands,
  customGraphableSource,
  filterSourceLocation,
  filterSourceStatus,
  graphableFilterKickoff,
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

  describe('getCustomFilterForLabel', () => {
    const label = 'ips';
    const customFilters = [
      {
        filter: 'AC.*',
        label: 'ac',
        subBandId: '',
      },
      {
        filter: '.*IPS.*',
        label: 'ips',
        subBandId: '',
      },
    ];

    it(`should return the filter for a label for a custom source`, () => {
      expect(getCustomFilterForLabel(label, customFilters)).toEqual({
        filter: '.*IPS.*',
        label: 'ips',
        subBandId: '',
      });
    });

    it(`should return null for a label with no filter`, () => {
      expect(getCustomFilterForLabel('badLabel', customFilters)).toEqual(null);
    });
  });

  describe('getFormattedSourceUrl', () => {
    const customFilter = {
      filter: '.*IPS.*',
        label: 'ips',
        subBandId: '',
    };
    const filtersByTarget = {
      '/SequenceTracker': {
        events: [filterSourceLocation, filterSourceStatus],
      },
    };

    it(`should return sourceUrl with custom filter args`, () => {
      expect(getFormattedSourceUrl(customGraphableSource, customFilter, filtersByTarget)).toEqual('https://a/b/c?format=TMS&legend=ips&filter=(command=[.*IPS.*])');
    });

    it(`should return sourceUrl with filter`, () => {
      // TODO.
      // expect(getFormattedSourceUrl(graphableFilterKickoff, customFilter, filtersByTarget)).toEqual('https://a/b/c?events=kickoff&');
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

  describe('getPin', () => {
    const pins = [
      { name: 'pin1', sourceId: '/a/b/c' },
      { name: 'pin2', sourceId: '/e/f' },
      { name: 'pin3', sourceId: '/a/b/c/d' },
      { name: 'pin4', sourceId: '/a' },
      { name: 'pin5', sourceId: '/a/b' },
    ];

    it(`should return a pin if a source id is in the pin set`, () => {
      expect(getPin('/e/f/g/h/i', pins)).toEqual({ name: 'pin2', sourceId: '/e/f' });
    });

    it(`should return the pin who's sourceId is the longest sub-string of the given sourceId (i.e. the nearest parent)`, () => {
      expect(getPin('/a/b/c/d/e', pins)).toEqual({ name: 'pin3', sourceId: '/a/b/c/d' });
    });

    it('should return null if the source id is not in the pin set', () => {
      expect(getPin('/e/x', pins)).toEqual(null);
    });
  });

  describe('getPinLabel', () => {
    it(`should get a pin label for a given list of source ids and pins`, () => {
      const sourceIds = ['/a/b/c', '/d/e/f'];
      const pins = [
        { name: 'pin1', sourceId: '/a/b/c' },
      ];

      expect(getPinLabel(sourceIds[0], pins)).toEqual('pin1');
    });

    it(`should return no pin label if there are no pins for the given source ids`, () => {
      const sourceIds = ['/a/b/c', '/d/e/f'];
      const pins = [
        { name: 'pin1', sourceId: '/x/y/z' },
      ];

      expect(getPinLabel(sourceIds[0], pins)).toEqual('');
    });
  });

  describe('getQueryOptionsForGraphableFilter', () => {
    const sourceIdOrUrl = 'leucadia/taifunTest/abc.pef/DKF';
    const groupFilters = {
      events: [graphableFilterKickoff],
    };
    it('should include filters as query option in the sourceId or url', () => {
      expect(getQueryOptionsForGraphableFilter(sourceIdOrUrl, groupFilters)).toEqual('leucadia/taifunTest/abc.pef/DKF?events=Kickoff&');
    });
  });

  describe('getQueryOptionsForGraphableFilter', () => {
    const sourceIdOrUrl = 'leucadia/taifunTest/abc.pef/DKF';
    const parentFilters = {
      collection: [],
    };
    it(`should include filters as query option in the sourceId or url`, () => {
      expect(getQueryOptionsForGraphableFilter(sourceIdOrUrl, parentFilters)).toEqual('leucadia/taifunTest/abc.pef/DKF?collection=&');
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

  describe('getSourceIdsForSubBand', () => {
    it(`should return all related filter sourceIds for subBand`, () => {
      // TODO
    });
  });

  describe('getTargetFilters', () => {
    const filtersByTarget = {
      '/SequenceTracker': {
        collection: [filterSourceStatus],
        meeting: [filterSourceLocation],
      },
    };

    it(`should return filter related to sourceId`, () => {
      expect(getTargetFilters(filtersByTarget, '/SequenceTracker')).toEqual({ collection: [filterSourceStatus], meeting: [ filterSourceLocation] });
    });
  });

  describe('getTargetFilterSourceIds', () => {

    it(`should return target filter source ids`, () => {
      // TODO
    });
  });

  describe('toRavenCustomMetadata', () => {
    it(`should convert to RavenCustomMetadata`, () => {
      expect(toRavenCustomMetadata([
        {
          Key: 'release',
          Value: 'A',
        },
        {
          Key: 'version',
          Value: 'seq 34.7',
        },
      ])).toEqual({
        release: 'A',
        version: 'seq 34.7',
      });
    });
  });

  describe('toRavenFileMetadata', () => {
    it(`should convert to folder RavenFileMetadata`, () => {
      expect(toRavenFileMetadata({
        __db_type: '',
        __kind: 'fs_dir',
        __kind_sub: '',
        contents_url: '',
        created: '2017-10-05 15:26:58-0700',
        createdBy: 'userA',
        customMeta: [
          {
            Key: 'release',
            Value: 'A',
          },
          {
            Key: 'version',
            Value: 'seq 34.7',
          },
        ],
        file_data_url: '',
        hasCollectionType: '',
        importJobStatus: '',
        label: '',
        modified: '2017-10-05 15:26:58-0700',
        name: '',
        permissions: 'rw-rw-r-- all us',
      })).toEqual({
        createdBy: 'userA',
        createdOn: '2017-10-05 15:26:58-0700',
        customMetadata: {
          release: 'A',
          version: 'seq 34.7',
        },
        fileType: 'folder',
        lastModified: '2017-10-05 15:26:58-0700',
        permissions: 'rw-rw-r--',
      });
    });

    it(`should convert to generic csv RavenFileMetadata`, () => {
      expect(toRavenFileMetadata({
        __db_type: '',
        __kind: 'fs_file',
        __kind_sub: 'file_maros',
        contents_url: '',
        created: '2017-10-05 15:26:58-0700',
        createdBy: 'userB',
        customMeta: [
          {
            Key: 'release',
            Value: 'B',
          },
          {
            Key: 'version',
            Value: 'seq 34.8',
          },
        ],
        file_data_url: '',
        hasCollectionType: '',
        importJobStatus: '',
        label: '',
        modified: '2017-10-05 15:26:58-0700',
        name: '',
        permissions: 'rw-rw-r-- all us',
      })).toEqual({
        createdBy: 'userB',
        createdOn: '2017-10-05 15:26:58-0700',
        customMetadata: {
          release: 'B',
          version: 'seq 34.8',
        },
        fileType: 'Generic CSV',
        lastModified: '2017-10-05 15:26:58-0700',
        permissions: 'rw-rw-r--',
      });
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
