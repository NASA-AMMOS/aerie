/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenCompositeBand, RavenSubBand } from '../models';
import { RavenExportBand } from '../models/export-state';
import { exportBand, importBand } from './state';

const commonCompositeProperties = {
  compositeAutoScale: false,
  compositeLogTicks: false,
  compositeScientificNotation: false,
  compositeYAxisLabel: false,
  height: 50,
  heightPadding: 0,
  overlay: false,
  showTooltip: false,
  type: 'composite',
};

const commonResourceProperties = {
  addTo: false,
  autoScale: false,
  color: '#FFEEDD',
  decimate: false,
  fill: false,
  fillColor: '#DDEEFF',
  height: 50,
  heightPadding: 0,
  icon: 'circle',
  interpolation: 'linear',
  isDuration: false,
  isTime: false,
  label: 'label',
  labelColor: '#AABBCC',
  labelFont: 'Helvetica',
  labelPin: '',
  labelUnit: 'mibibits',
  logTicks: false,
  maxLimit: undefined,
  maxTimeRange: { end: 0, start: 0 },
  minLimit: undefined,
  points: [],
  scientificNotation: false,
  showIcon: false,
  showLabelPin: false,
  showLabelUnit: false,
  showTooltip: false,
  sourceIds: [],
  tableColumns: [],
  type: 'resource',
};

export const preExportBands: RavenCompositeBand[] = [
  {
    ...commonCompositeProperties,
    backgroundColor: '#FFFFFF',
    containerId: '0',
    id: '100',
    name: 'test-composite-band-0',
    sortOrder: 0,
    subBands: [
      {
        ...commonResourceProperties,
        id: '0',
        name: 'test-resource-sub-band-0',
        parentUniqueId: '100',
      },
    ],
  },
  {
    ...commonCompositeProperties,
    backgroundColor: '#DDDDDD',
    containerId: '0',
    id: '101',
    name: 'test-composite-band-1',
    sortOrder: 1,
    subBands: [
      {
        ...commonResourceProperties,
        id: '0',
        name: 'test-resource-sub-band-1',
        parentUniqueId: '101',
      },
      {
        ...commonResourceProperties,
        id: '1',
        name: 'test-resource-sub-band-2',
        parentUniqueId: '101',
      },
    ],
  },
  {
    ...commonCompositeProperties,
    backgroundColor: '#222222',
    containerId: '1',
    id: '102',
    name: 'test-composite-band-2',
    sortOrder: 0,
    subBands: [
      {
        ...commonResourceProperties,
        id: '0',
        name: 'test-resource-sub-band-3',
        parentUniqueId: '102',
      },
    ],
  },
];

export const postExportBands: RavenExportBand[] = [
  {
    ...commonResourceProperties,
    backgroundColor: '#FFFFFF',
    containerId: '0',
    name: 'test-resource-sub-band-0',
    sortOrder: 0,
  },
  {
    ...commonCompositeProperties,
    backgroundColor: '#DDDDDD',
    containerId: '0',
    name: 'test-composite-band-1',
    sortOrder: 1,
    subBands: [
      {
        ...commonResourceProperties,
        name: 'test-resource-sub-band-1',
      },
      {
        ...commonResourceProperties,
        name: 'test-resource-sub-band-2',
      },
    ],
  },
  {
    ...commonResourceProperties,
    backgroundColor: '#222222',
    containerId: '1',
    name: 'test-resource-sub-band-3',
    sortOrder: 0,
  },
];

describe('state.ts', () => {
  describe('exportBand', () => {
    it('should properly export bands', () => {
      expect(preExportBands.map(band => exportBand(band))).toEqual(
        postExportBands,
      );
    });
  });

  describe('importBand', () => {
    it('should properly import bands', () => {
      const bands = postExportBands.map(band => importBand(band));

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
