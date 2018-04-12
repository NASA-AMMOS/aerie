/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  RavenActivityBand,
  RavenCompositeBand,
  RavenResourceBand,
  RavenStateBand,
} from './../models';

import {
  activityPoint,
} from './raven-points';

export const activityBand: RavenActivityBand = {
  activityHeight: 20,
  activityStyle: 1,
  addTo: false,
  alignLabel: 3,
  baselineLabel: 3,
  borderWidth: 1,
  height: 50,
  heightPadding: 10,
  id: '100',
  label: 'test-activity-band',
  labelColor: [0, 0, 0],
  layout: 1,
  legend: '',
  maxTimeRange: { end: 200, start: 50 },
  minorLabels: [],
  name: 'test-activity-band',
  parentUniqueId: null,
  points: [],
  showLabel: true,
  showTooltip: true,
  sourceIds: {},
  sourceType: '',
  trimLabel: true,
  type: 'activity',
};

export const compositeBand: RavenCompositeBand = {
  containerId: '0',
  height: 50,
  heightPadding: 0,
  id: '200',
  name: 'test-composite-band',
  overlay: false,
  showTooltip: true,
  sortOrder: 0,
  subBands: [],
  type: 'composite',
};

export const resourceBand: RavenResourceBand = {
  addTo: false,
  autoTickValues: true,
  color: [0, 0, 0],
  fill: false,
  fillColor: [0, 0, 0],
  height: 50,
  heightPadding: 10,
  id: '101',
  interpolation: 'linear',
  label: 'test-resource-band',
  labelColor: [0, 0, 0],
  maxTimeRange: { end: 300, start: 100 },
  minorLabels: [],
  name: 'test-resource-band',
  parentUniqueId: null,
  points: [],
  rescale: true,
  showIcon: false,
  showTooltip: true,
  sourceIds: {},
  type: 'resource',
};

export const stateBand: RavenStateBand = {
  addTo: false,
  alignLabel: 3,
  baselineLabel: 3,
  borderWidth: 1,
  height: 50,
  heightPadding: 0,
  id: '102',
  label: 'test-state-band',
  labelColor: [0, 0, 0],
  maxTimeRange: { end: 100, start: 0 },
  minorLabels: [],
  name: 'test-state-band',
  parentUniqueId: null,
  points: [],
  showTooltip: true,
  sourceIds: {},
  type: 'state',
};

/**
 * This is a list of mock bands built from bands above for use in unit tests.
 */
export const bands: RavenCompositeBand[] = [
  {
    ...compositeBand,
    id: '100',
    name: 'test-composite-band-0',
    sortOrder: 0,
    subBands: [
      {
        ...activityBand,
        addTo: true,
        id: '0',
        legend: 'a',
        name: 'test-activity-sub-band-0',
        parentUniqueId: '100',
        sourceIds: {
          '/a/b/c/d/e/w/': '/a/b/c/d/e/w/',
        },
        sourceType: 'byType',
      },
    ],
  },
  {
    ...compositeBand,
    id: '101',
    name: 'test-composite-band-1',
    overlay: false,
    sortOrder: 1,
    subBands: [
      {
        ...stateBand,
        id: '1',
        name: 'test-state-sub-band-0',
        parentUniqueId: '101',
        sourceIds: {
          '/a/b/c/d/e/x/y/': '/a/b/c/d/e/x/y/',
        },
      },
    ],
  },
  {
    ...compositeBand,
    id: '102',
    name: 'test-composite-band-2',
    overlay: true,
    sortOrder: 2,
    subBands: [
      {
        ...resourceBand,
        id: '2',
        name: 'test-resource-sub-band-0',
        parentUniqueId: '102',
        sourceIds: {
          '/a/b/c/d/e/x/z/': '/a/b/c/d/e/x/z/',
        },
      },
    ],
  },
  {
    ...compositeBand,
    id: '103',
    name: 'test-composite-band-3',
    sortOrder: 3,
    subBands: [
      {
        ...activityBand,
        addTo: true,
        id: '3',
        name: 'test-activity-sub-band-3',
        parentUniqueId: '103',
        sourceIds: {
          '/a/b/c/d/e/u/': '/a/b/c/d/e/u/',
        },
        sourceType: 'byLegend',
      },
    ],
  },
  {
    ...compositeBand,
    id: '104',
    name: 'test-composite-band-4',
    sortOrder: 4,
    subBands: [
      {
        ...activityBand,
        addTo: true,
        id: '4',
        legend: 'a',
        name: 'test-activity-sub-band-4',
        parentUniqueId: '104',
        points: [{
          ...activityPoint,
          sourceId: '/a/b/c/d/e/v/',
          subBandId: '4',
          uniqueId: '400',
        }],
        sourceIds: {
          '/a/b/c/d/e/v/': '/a/b/c/d/e/v/',
        },
        sourceType: 'byType',
      },
    ],
  },
];
