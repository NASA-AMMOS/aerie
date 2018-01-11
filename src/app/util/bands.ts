/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { groupBy } from 'lodash';
import { v4 } from 'uuid';

import {
  MpsServerActivityPoint,
  MpsServerResourcePointMetadata,
  MpsServerResourcePoint,
  RavenActivityBand,
  RavenActivityPoint,
  RavenBand,
  RavenCompositeBand,
  RavenDividerBand,
  RavenResourceBand,
} from '../models/index';

import {
  getMaxTimeRangeForPoints,
  mpsServerToRavenActivityPoints,
  mpsServerToRavenResourcePoints,
} from './points';

/**
 * Returns a list of bands based on timelineData and point legends.
 */
export function toActivityBands(sourceId: string, timelineData: MpsServerActivityPoint[]) {
  const bands: RavenActivityBand[] = [];
  const points: RavenActivityPoint[] = mpsServerToRavenActivityPoints(sourceId, timelineData);
  const legends = groupBy(points, 'legend');

  // Map each legend to a band.
  Object.keys(legends).forEach((legend) => {
    bands.push(toActivityBand(legend, legends[legend]));
  });

  return bands;
}

/**
 * Returns an activity band given a name and points.
 */
export function toActivityBand(name: string, points: RavenActivityPoint[]): RavenActivityBand {
  // Activity band schema.
  const activityBand = {
    activityStyle: 1,
    alignLabel: 3,
    baselineLabel: 3,
    height: 50,
    heightPadding: 10,
    id: v4(),
    label: name,
    labelColor: [0, 0, 0],
    layout: 1,
    maxTimeRange: getMaxTimeRangeForPoints(points),
    minorLabels: [],
    name,
    parentUniqueId: null,
    points,
    showLabel: true,
    showTooltip: true,
    sourceIds: {}, // Map of source ids this band has data from.
    trimLabel: true,
    type: 'activity',
  };

  return activityBand;
}

/**
 * Returns a composite band given a list of bands.
 * Bands should already be preformatted to their correct schema by the time they arrive here.
 */
export function toCompositeBand(bands: RavenBand[]): RavenCompositeBand {
  const compositeBandUniqueId = v4();
  let compositeBandName = '';

  bands = bands.map((band, index) => {
    band.parentUniqueId = compositeBandUniqueId;

    // Make the initial composite band name just an '&' separated list of band names.
    compositeBandName += `${band.name}`;
    if (bands[index + 1]) {
      compositeBandName += ' & ';
    }

    return band;
  });

  const compositeBand = {
    bands,
    height: 100,
    heightPadding: 10,
    id: compositeBandUniqueId,
    name: compositeBandName,
    parentUniqueId: null,
    showTooltip: true,
    type: 'composite',
  };

  return compositeBand;
}

/**
 * Returns a default divider band.
 */
export function toDividerBand(): RavenDividerBand {
  const id = v4();

  const dividerBand = {
    color: [255, 255, 255],
    height: 7,
    id,
    label: `Divider ${id}`,
    labelColor: [0, 0, 0],
    minorLabels: [],
    name: `Divider ${id}`,
    parentUniqueId: null,
    showTooltip: true,
    type: 'divider',
  };

  return dividerBand;
}

/**
 * Returns a resource band given metadata and timelineData.
 */
export function toResourceBand(sourceId: string, metadata: MpsServerResourcePointMetadata, timelineData: MpsServerResourcePoint[]): RavenResourceBand {
  // Map raw resource timeline data to points for a band.
  const points = mpsServerToRavenResourcePoints(sourceId, timelineData);

  // Resource band schema.
  const resourceBand = {
    autoTickValues: true,
    color: [0, 0, 0],
    fill: false,
    fillColor: [0, 0, 0],
    height: 100,
    heightPadding: 10,
    id: v4(),
    interpolation: 'linear',
    label: metadata.hasObjectName,
    labelColor: [0, 0, 0],
    maxTimeRange: getMaxTimeRangeForPoints(points),
    minorLabels: [],
    name: metadata.hasObjectName,
    parentUniqueId: null,
    points,
    rescale: true,
    showIcon: false,
    showTooltip: true,
    sourceIds: {}, // Map of source ids this band has data from.
    type: 'resource',
  };

  return resourceBand;
}
