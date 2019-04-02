/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenState, RavenSubBand } from '../models';
import { exportState, importState } from './state';

export const preExportBands: any[] = [
  {
    backgroundColor: '#FFFFFF',
    containerId: '0',
    id: '100',
    name: 'test-composite-band-0',
    sortOrder: 0,
    subBands: [
      {
        id: '0',
        name: 'test-activity-sub-band-0',
        parentUniqueId: '100',
      },
    ],
  },
  {
    backgroundColor: '#DDDDDD',
    containerId: '0',
    id: '101',
    name: 'test-composite-band-1',
    sortOrder: 1,
    subBands: [
      {
        id: '0',
        name: 'test-resource-sub-band-0',
        parentUniqueId: '101',
      },
      {
        id: '1',
        name: 'test-resource-sub-band-1',
        parentUniqueId: '101',
      },
    ],
  },
  {
    backgroundColor: '#222222',
    containerId: '1',
    id: '102',
    name: 'test-composite-band-2',
    sortOrder: 0,
    subBands: [
      {
        id: '0',
        name: 'test-divider-sub-band-0',
        parentUniqueId: '102',
      },
    ],
  },
];

export const postExportBands: any[] = [
  {
    backgroundColor: '#FFFFFF',
    containerId: '0',
    name: 'test-activity-sub-band-0',
    sortOrder: 0,
  },
  {
    backgroundColor: '#DDDDDD',
    containerId: '0',
    name: 'test-composite-band-1',
    sortOrder: 1,
    subBands: [
      {
        name: 'test-resource-sub-band-0',
      },
      {
        name: 'test-resource-sub-band-1',
      },
    ],
  },
  {
    backgroundColor: '#222222',
    containerId: '1',
    name: 'test-divider-sub-band-0',
    sortOrder: 0,
  },
];

describe('state.ts', () => {
  describe('exportState', () => {
    it('should properly export a state', () => {
      expect(exportState({ bands: preExportBands } as RavenState)).toEqual({
        bands: postExportBands,
      });
    });
  });

  describe('importState', () => {
    it('should properly import a state', () => {
      const { bands } = importState({ bands: postExportBands });

      // Make sure all bands are composite bands.
      bands.forEach(band => {
        expect(band.type).toEqual('composite');
      });

      // Make sure container id and sort order were properly restored.
      bands.forEach((band, i) => {
        expect(band.containerId).toBeDefined();
        expect(band.sortOrder).toBeDefined();
        expect(band.containerId).toEqual(preExportBands[i].containerId);
        expect(band.sortOrder).toEqual(preExportBands[i].sortOrder);
      });

      // Check that ids are all accounted for.
      bands.forEach(band => {
        const parentUniqueId = band.id;
        expect(parentUniqueId).toBeDefined();

        band.subBands.forEach((subBand: RavenSubBand) => {
          expect(subBand.id).toBeDefined();
          expect(subBand.parentUniqueId).toEqual(parentUniqueId);
        });
      });
    });
  });
});
