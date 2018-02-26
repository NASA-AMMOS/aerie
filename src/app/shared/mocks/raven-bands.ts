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

export const activityBand: RavenActivityBand = {
  activityStyle: 1,
  alignLabel: 3,
  baselineLabel: 3,
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
  sourceId: '',
  sourceName: '',
  trimLabel: true,
  type: 'activity',
};

export const compositeBand: RavenCompositeBand = {
  containerId: '0',
  height: 50,
  heightPadding: 0,
  id: '200',
  name: 'test-composite-band',
  showTooltip: true,
  sortOrder: 0,
  subBands: [],
  type: 'composite',
};

export const resourceBand: RavenResourceBand = {
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
  sourceId: '',
  sourceName: '',
  type: 'resource',
};

export const stateBand: RavenStateBand = {
  alignLabel: 3,
  baselineLabel: 3,
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
  sourceId: '',
  sourceName: '',
  type: 'state',
};
