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
  getParentSourceIds,
  getSourceIds,
  getSourceType,
} from './source';

import {
  bands,
  treeBySourceId,
} from './../mocks/';

describe('source.ts', () => {
  describe('getAllChildIds', () => {
    it(`should return a list of all child ids (recursively) for a given source id`, () => {
      expect(getAllChildIds(treeBySourceId, '/')).toEqual([
        '/child/0/',
        '/child/1/',
        '/child/child/0/',
      ]);

      expect(getAllChildIds(treeBySourceId, '/child/1/')).toEqual([
        '/child/child/0/',
      ]);
    });

    it(`should return an empty list for a source id that does not exist in the tree`, () => {
      expect(getAllChildIds(treeBySourceId, 'nonExistentId')).toEqual([]);
    });
  });

  describe('getParentSourceIds', () => {
    it(`should split a source correctly into it's parent source ids, in the correct order`, () => {
      expect(getParentSourceIds('/hello/world/goodbye/what/is/going/on/')).toEqual([
        '/hello/',
        '/hello/world/',
        '/hello/world/goodbye/',
        '/hello/world/goodbye/what/',
        '/hello/world/goodbye/what/is/',
        '/hello/world/goodbye/what/is/going/',
      ]);
    });
  });

  describe('getSourceIds', () => {
    it(`should split sources from a list of bands into parent source ids and leaf source ids`, () => {
      expect(getSourceIds(bands)).toEqual({
        parentSourceIds: [
          '/a/',
          '/a/b/',
          '/a/b/c/',
          '/a/b/c/d/',
          '/a/b/c/d/e/',
          '/a/b/c/d/e/x/',
        ],
        sourceIds: [
          '/a/b/c/d/e/w/',
          '/a/b/c/d/e/x/y/',
          '/a/b/c/d/e/x/z/',
          '/a/b/c/d/e/u/',
          '/a/b/c/d/e/v/',
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
});
