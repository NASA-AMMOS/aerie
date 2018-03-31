/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  getParentPaths,
  getSourceType,
} from './source';

describe('source.ts', () => {
  describe('getParentPaths', () => {
    it(`should split a source correctly into it's parent paths`, () => {
      expect(getParentPaths('/hello/world/goodbye/')).toEqual(['/hello/', '/hello/world/']);
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
