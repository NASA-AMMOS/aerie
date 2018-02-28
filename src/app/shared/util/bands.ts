/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { sortBy } from 'lodash';
import { v4 } from 'uuid';

import {
  MpsServerActivityPoint,
  MpsServerGraphData,
  MpsServerResourceMetadata,
  MpsServerResourcePoint,
  MpsServerStateMetadata,
  MpsServerStatePoint,
  RavenActivityBand,
  RavenCompositeBand,
  RavenDividerBand,
  RavenResourceBand,
  RavenSource,
  RavenStateBand,
  RavenSubBand,
  RavenTimeRange,
} from './../models';

import {
  getActivityPointsByLegend,
  getResourcePoints,
  getStatePoints,
} from './points';

/**
 * This is a helper function that takes a list of current bands and their associated sourceId,
 * and returns a list of band ids that we want to remove.
 */
export function removeBands(sourceId: string, bands: RavenCompositeBand[]): string[] {
  const bandIds: string[] = [];

  for (let i = 0, l = bands.length; i < l; ++i) {
    const band = bands[i];

    for (let j = 0, ll = band.subBands.length; j < ll; ++j) {
      const subBand = band.subBands[j];

      if (subBand.sourceId === sourceId) {
        bandIds.push(subBand.id);
      }
    }
  }

  return bandIds;
}

/**
 * Returns a data structure that transforms MpsServerGraphData to bands displayed in Raven.
 * Note that we do not worry about how these bands are displayed here. That is the job of the reducer.
 */
export function toRavenBandData(source: RavenSource, graphData: MpsServerGraphData): RavenSubBand[] {
  const metadata = graphData['Timeline Metadata'];
  const timelineData = graphData['Timeline Data'];

  if (metadata.hasTimelineType === 'measurement' && (metadata as MpsServerStateMetadata).hasValueType === 'string_xdr' ||
      metadata.hasTimelineType === 'state') {
    // State.
    const stateBand = toStateBand(source, metadata as MpsServerStateMetadata, timelineData as MpsServerStatePoint[]);
    return [stateBand];
  } else if (metadata.hasTimelineType === 'measurement') {
    // Resource.
    const resourceBand = toResourceBand(source, metadata as MpsServerResourceMetadata, timelineData as MpsServerResourcePoint[]);
    return [resourceBand];
  } else if (metadata.hasTimelineType === 'activity') {
    // Activity.
    const activityBands = toActivityBands(source, timelineData as MpsServerActivityPoint[]);
    return activityBands;
  } else {
    console.error(`raven2 - bands.ts - toRavenBandData - parameter 'graphData' has a timeline type we do not recognize: ${metadata.hasTimelineType}`);
    return [];
  }
}

/**
 * Returns a list of bands based on timelineData and point legends.
 */
export function toActivityBands(source: RavenSource, timelineData: MpsServerActivityPoint[]): RavenActivityBand[] {
  const { legends, maxTimeRange } = getActivityPointsByLegend(source.id, timelineData);
  const bands: RavenActivityBand[] = [];

  // Map each legend to a band.
  Object.keys(legends).forEach(legend => {
    const activityBand: RavenActivityBand = {
      activityStyle: 1,
      alignLabel: 3,
      baselineLabel: 3,
      height: 50,
      heightPadding: 10,
      id: v4(),
      label: `${legend} - ${source.name}`,
      labelColor: [0, 0, 0],
      layout: 1,
      legend,
      maxTimeRange,
      minorLabels: [],
      name: legend,
      parentUniqueId: null,
      points: legends[legend],
      showLabel: true,
      showTooltip: true,
      sourceId: source.id,
      sourceName: source.name,
      sourceUrl: source.url,
      trimLabel: true,
      type: 'activity',
    };

    bands.push(activityBand);
  });

  return bands;
}

/**
 * Returns a composite band.
 */
export function toCompositeBand(subBand: RavenSubBand): RavenCompositeBand {
  const compositeBandUniqueId = v4();

  const compositeBand: RavenCompositeBand = {
    containerId: '0',
    height: subBand.height,
    heightPadding: subBand.heightPadding,
    id: compositeBandUniqueId,
    name: subBand.name,
    showTooltip: subBand.showTooltip,
    sortOrder: 0,
    subBands: [{
      ...subBand,
      parentUniqueId: compositeBandUniqueId,
    }],
    type: 'composite',
  };

  return compositeBand;
}

/**
 * Returns a default divider band.
 */
export function toDividerBand(): RavenDividerBand {
  const id = v4();

  const dividerBand: RavenDividerBand = {
    color: [255, 255, 255],
    containerId: '0',
    height: 7,
    id,
    label: `Divider ${id}`,
    labelColor: [0, 0, 0],
    minorLabels: [],
    name: `Divider ${id}`,
    showTooltip: true,
    sortOrder: 0,
    type: 'divider',
  };

  return dividerBand;
}

/**
 * Returns a resource band given metadata and timelineData.
 */
export function toResourceBand(source: RavenSource, metadata: MpsServerResourceMetadata, timelineData: MpsServerResourcePoint[]): RavenResourceBand {
  const { maxTimeRange, points } = getResourcePoints(source.id, timelineData);

  const resourceBand: RavenResourceBand = {
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
    maxTimeRange,
    minorLabels: [],
    name: metadata.hasObjectName,
    parentUniqueId: null,
    points,
    rescale: true,
    showIcon: false,
    showTooltip: true,
    sourceId: source.id,
    sourceName: source.name,
    sourceUrl: source.url,
    type: 'resource',
  };

  return resourceBand;
}

/**
 * Returns a state band given metadata and timelineData.
 */
export function toStateBand(source: RavenSource, metadata: MpsServerStateMetadata, timelineData: MpsServerStatePoint[]): RavenStateBand {
  const { maxTimeRange, points } = getStatePoints(source.id, timelineData);

  const stateBand: RavenStateBand = {
    alignLabel: 3,
    baselineLabel: 3,
    height: 50,
    heightPadding: 0,
    id: v4(),
    label: metadata.hasObjectName,
    labelColor: [0, 0, 0],
    maxTimeRange,
    minorLabels: [],
    name: metadata.hasObjectName,
    parentUniqueId: null,
    points,
    showTooltip: true,
    sourceId: source.id,
    sourceName: source.name,
    sourceUrl: source.url,
    type: 'state',
  };

  return stateBand;
}

/**
 * Helper. Updates the sortOrder for the given bands within each containerId.
 * Does not change the displayed sort order, only shifts the indices so they count up from 0.
 * When we remove bands, this is good at making sure the sortOrder is always maintained.
 *
 * For example if the following bands array is given (other non-necessary band props excluded):
 *
 * let bands = [
 *    { containerId: '0', sortOrder: 100 },
 *    { containerId: '1', sortOrder: 20 },
 *    { containerId: '0', sortOrder: 10 },
 *    { containerId: '1', sortOrder: 10 },
 * ];
 *
 * First we do a sortBy containerId and sortOrder which gives:
 *
 * bands === [
 *    { containerId: '0', sortOrder: 10 },
 *    { containerId: '0', sortOrder: 100 },
 *    { containerId: '1', sortOrder: 10 },
 *    { containerId: '1', sortOrder: 20 },
 * ];
 *
 * Then we reset the sortOrder for each given containerId to start at 0:
 *
 * bands === [
 *    { containerId: '0', sortOrder: 0 },
 *    { containerId: '0', sortOrder: 1 },
 *    { containerId: '1', sortOrder: 0 },
 *    { containerId: '1', sortOrder: 1 },
 * ];
 */
export function updateSortOrder(bands: RavenCompositeBand[]): RavenCompositeBand[] {
  const sortByBands = sortBy(bands, 'containerId', 'sortOrder');
  const index = {}; // Hash of containerIds to a given index (indices start at 0).

  return sortByBands.map((band: RavenCompositeBand) => {
    if (index[band.containerId] === undefined) {
      index[band.containerId] = 0;
    } else {
      index[band.containerId]++;
    }

    return {
      ...band,
      sortOrder: index[band.containerId],
    };
  });
}

/**
 * Helper that gets new time ranges based on the current view time range and the list of given bands.
 *
 * TODO: Remove 'any' bands type for concrete type.
 */
export function updateTimeRanges(currentViewTimeRange: RavenTimeRange, bands: RavenCompositeBand[]) {
  let maxTimeRange: RavenTimeRange = { end: 0, start: 0 };
  let viewTimeRange: RavenTimeRange = { end: 0, start: 0 };

  if (bands.length > 0) {
    let endTime = Number.MIN_SAFE_INTEGER;
    let startTime = Number.MAX_SAFE_INTEGER;

    // Calculate the maxTimeRange out of every band.
    for (let i = 0, l = bands.length; i < l; ++i) {
      const band = bands[i];

      for (let j = 0, ll = band.subBands.length; j < ll; ++j) {
        const subBand = band.subBands[j];

        if (subBand.maxTimeRange.start < startTime) { startTime = subBand.maxTimeRange.start; }
        if (subBand.maxTimeRange.end > endTime) { endTime = subBand.maxTimeRange.end; }
      }
    }

    maxTimeRange = { end: endTime, start: startTime };
    viewTimeRange = { ...currentViewTimeRange };

    // Re-set viewTimeRange to maxTimeRange if both start and end are 0 (i.e. they have never been set).
    if (viewTimeRange.start === 0 && viewTimeRange.end === 0) {
      viewTimeRange = { ...maxTimeRange };
    } else {
      // Clamp new viewTimeRange start.
      if (viewTimeRange.start < maxTimeRange.start) {
        viewTimeRange.start = maxTimeRange.start;
      }

      // Clamp new viewTimeRange end.
      if (viewTimeRange.end > maxTimeRange.end) {
        viewTimeRange.end = maxTimeRange.end;
      }
    }
  }

  return {
    maxTimeRange,
    viewTimeRange,
  };
}

/**
 * Helper. Returns true of bands contain a given id. False otherwise.
 */
export function hasId(bands: RavenCompositeBand[], id: string): boolean {
  for (let i = 0, l = bands.length; i < l; ++i) {
    if (bands[i].id === id) {
      return true;
    }
  }
  return false;
}

/**
 * Helper. Returns true if a legend exists in any sub-band out of a list of composite bands. False otherwise.
 */
export function hasActivityLegend(compositeBands: RavenCompositeBand[], band: RavenActivityBand): boolean {
  if (band.type === 'activity') {
    for (let i = 0, l = compositeBands.length; i < l; ++i) {
      for (let j = 0, ll = compositeBands[i].subBands.length; j < ll; ++j) {
        const subBand = compositeBands[i].subBands[j] as RavenActivityBand;

        if (subBand.type === 'activity' && subBand.legend !== '' && subBand.legend === band.legend) {
          return true;
        }
      }
    }
  }
  return false;
}

/**
 * Helper. Returns true of we are in overlay mode and should overlay on a band with `bandId`. False otherwise.
 */
export function shouldOverlay(overlayMode: boolean, selectedBandId: string, bandId: string): boolean {
  return overlayMode && selectedBandId === bandId;
}
