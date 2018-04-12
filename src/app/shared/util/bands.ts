/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  sortBy,
  uniqueId,
} from 'lodash';

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
  RavenStateBand,
  RavenSubBand,
  RavenTimeRange,
} from './../models';

import {
  getActivityPointsByLegend,
  getResourcePoints,
  getStatePoints,
} from './points';

import {
  getSourceType,
} from './source';

/**
 * Returns a data structure that transforms MpsServerGraphData to bands displayed in Raven.
 * Note that we do not worry about how these bands are displayed here.
 * We are just generating the band types for use elsewhere.
 */
export function toRavenBandData(sourceId: string, graphData: MpsServerGraphData): RavenSubBand[] {
  const metadata = graphData['Timeline Metadata'];
  const timelineData = graphData['Timeline Data'];

  if (metadata.hasTimelineType === 'measurement' && (metadata as MpsServerStateMetadata).hasValueType === 'string_xdr' ||
      metadata.hasTimelineType === 'state') {
    // State.
    const stateBand = toStateBand(sourceId, metadata as MpsServerStateMetadata, timelineData as MpsServerStatePoint[]);
    return [stateBand];
  } else if (metadata.hasTimelineType === 'measurement') {
    // Resource.
    const resourceBand = toResourceBand(sourceId, metadata as MpsServerResourceMetadata, timelineData as MpsServerResourcePoint[]);
    return [resourceBand];
  } else if (metadata.hasTimelineType === 'activity') {
    // Activity.
    const activityBands = toActivityBands(sourceId, timelineData as MpsServerActivityPoint[]);
    return activityBands;
  } else {
    console.error(`raven2 - bands.ts - toRavenBandData - parameter 'graphData' has a timeline type we do not recognize: ${metadata.hasTimelineType}`);
    return [];
  }
}

/**
 * Returns a list of bands based on timelineData and point legends.
 */
export function toActivityBands(sourceId: string, timelineData: MpsServerActivityPoint[]): RavenActivityBand[] {
  const { legends, maxTimeRange } = getActivityPointsByLegend(sourceId, timelineData);
  const sourceType = getSourceType(sourceId);
  const bands: RavenActivityBand[] = [];

  // Map each legend to a band.
  Object.keys(legends).forEach(legend => {
    const activityBand: RavenActivityBand = {
      activityHeight: 20,
      activityStyle: 1,
      addTo: false,
      alignLabel: 3,
      baselineLabel: 3,
      borderWidth: 1,
      height: 50,
      heightPadding: 10,
      id: uniqueId(),
      label: `${legend}`,
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
      sourceIds: {
        [sourceId]: sourceId,
      },
      sourceType,
      trimLabel: true,
      type: 'activity',
    };

    bands.push(activityBand);
  });

  return bands;
}

/**
 * Returns a list of new composite bands.
 */
export function toCompositeBand(subBand: RavenSubBand): RavenCompositeBand {
  const compositeBandUniqueId = uniqueId();

  const compositeBand: RavenCompositeBand = {
    containerId: '0',
    height: subBand.height,
    heightPadding: subBand.heightPadding,
    id: compositeBandUniqueId,
    name: subBand.name,
    overlay: false,
    showTooltip: subBand.showTooltip,
    sortOrder: 0,
    subBands: [{
      ...subBand,
    }],
    type: 'composite',
  };

  return compositeBand;
}

/**
 * Returns a default divider band.
 */
export function toDividerBand(): RavenDividerBand {
  const id = uniqueId();

  const dividerBand: RavenDividerBand = {
    addTo: false,
    color: [255, 255, 255],
    height: 7,
    heightPadding: 10,
    id,
    label: `Divider ${id}`,
    labelColor: [0, 0, 0],
    maxTimeRange: { start: 0, end: 0 },
    minorLabels: [],
    name: `Divider ${id}`,
    points: [],
    showTooltip: true,
    sourceIds: {},
    type: 'divider',
  };

  return dividerBand;
}

/**
 * Returns a resource band given metadata and timelineData.
 */
export function toResourceBand(sourceId: string, metadata: MpsServerResourceMetadata, timelineData: MpsServerResourcePoint[]): RavenResourceBand {
  const { maxTimeRange, points } = getResourcePoints(sourceId, timelineData);

  const resourceBand: RavenResourceBand = {
    addTo: false,
    autoTickValues: true,
    color: [0, 0, 0],
    fill: false,
    fillColor: [0, 0, 0],
    height: 100,
    heightPadding: 10,
    id: uniqueId(),
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
    sourceIds: {
      [sourceId]: sourceId,
    },
    type: 'resource',
  };

  return resourceBand;
}

/**
 * Returns a state band given metadata and timelineData.
 */
export function toStateBand(sourceId: string, metadata: MpsServerStateMetadata, timelineData: MpsServerStatePoint[]): RavenStateBand {
  const { maxTimeRange, points } = getStatePoints(sourceId, timelineData);

  const stateBand: RavenStateBand = {
    addTo: false,
    alignLabel: 3,
    baselineLabel: 3,
    borderWidth: 1,
    height: 50,
    heightPadding: 0,
    id: uniqueId(),
    label: metadata.hasObjectName,
    labelColor: [0, 0, 0],
    maxTimeRange,
    minorLabels: [],
    name: metadata.hasObjectName,
    parentUniqueId: null,
    points,
    showTooltip: true,
    sourceIds: {
      [sourceId]: sourceId,
    },
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
 */
export function updateTimeRanges(bands: RavenCompositeBand[], currentViewTimeRange: RavenTimeRange) {
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

        // Special case!
        // Since dividers don't really have a time range, make sure we do not re-calc time for them.
        if (subBand.type !== 'divider') {
          if (subBand.maxTimeRange.start < startTime) { startTime = subBand.maxTimeRange.start; }
          if (subBand.maxTimeRange.end > endTime) { endTime = subBand.maxTimeRange.end; }
        }
      }
    }

    maxTimeRange = { end: endTime, start: startTime };
    viewTimeRange = { ...currentViewTimeRange };

    // Re-set viewTimeRange to maxTimeRange if both start and end are 0 (e.g. they have never been set),
    // or they are MIN/MAX values (e.g. if we only have dividers on screen).
    if (viewTimeRange.start === 0 && viewTimeRange.end === 0 ||
        viewTimeRange.start === Number.MAX_SAFE_INTEGER && viewTimeRange.end === Number.MIN_SAFE_INTEGER) {
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
 * Helper. Updates the selectedBandId and selectedSubBandId based on the current band list.
 */
export function updateSelectedBandIds(bands: RavenCompositeBand[], selectedBandId: string, selectedSubBandId: string) {
  const band = bandById(bands, selectedBandId);
  const subBand = subBandById(bands, selectedBandId, selectedSubBandId);

  if (!band) {
    selectedBandId = '';
    selectedSubBandId = '';
  } else if (!subBand) {
    selectedSubBandId = band.subBands[0].id;
  }

  return {
    selectedBandId,
    selectedSubBandId,
  };
}

/**
 * Helper. Returns an activity-by-type band locator if a given band exists in the list of bands for a legend.
 * `null` otherwise.
 */
export function hasActivityByTypeBand(bands: RavenCompositeBand[], band: RavenSubBand) {
  if (band.type === 'activity' && (band as RavenActivityBand).sourceType === 'byType') {
    for (let i = 0, l = bands.length; i < l; ++i) {
      for (let j = 0, ll = bands[i].subBands.length; j < ll; ++j) {
        const subBand = bands[i].subBands[j] as RavenActivityBand;

        if (
          subBand.type === 'activity' &&
          subBand.sourceType === 'byType' &&
          subBand.legend === (band as RavenActivityBand).legend
        ) {
          return {
            bandId: bands[i].id,
            subBandId: subBand.id,
          };
        }
      }
    }
  }
  return null;
}

/**
 * Helper. Returns a sub-band locator if a given band has a source id.
 * `null` otherwise.
 */
export function hasSourceId(bands: RavenCompositeBand[], sourceId: string) {
  for (let i = 0, l = bands.length; i < l; ++i) {
    for (let j = 0, ll = bands[i].subBands.length; j < ll; ++j) {
      const subBand = bands[i].subBands[j];

      if (subBand.sourceIds[sourceId]) {
        return {
          bandId: bands[i].id,
          subBandId: subBand.id,
        };
      }
    }
  }
  return null;
}

/**
 * Helper. Returns a band from a list of bands with the given id. Null otherwise.
 */
export function bandById(bands: RavenCompositeBand[], id: string): RavenCompositeBand | null {
  for (let i = 0, l = bands.length; i < l; ++i) {
    if (bands[i].id === id) {
      return bands[i];
    }
  }
  return null;
}

/**
 * Helper. Returns a sub-band from a list of bands with the given id. Null otherwise.
 */
export function subBandById(bands: RavenCompositeBand[], bandId: string, subBandId: string): RavenSubBand | null {
  for (let i = 0, l = bands.length; i < l; ++i) {
    if (bands[i].id === bandId) {
      for (let j = 0, ll = bands[i].subBands.length; j < ll; ++j) {
        if (bands[i].subBands[j].id === subBandId) {
          return bands[i].subBands[j];
        }
      }
    }
  }
  return null;
}

/**
 * Helper. Returns true if the given sub-band id in a list of bands is in add-to mode. False otherwise.
 */
export function isAddTo(bands: RavenCompositeBand[], bandId: string, subBandId: string, type: string): boolean {
  const subBand = subBandById(bands, bandId, subBandId);

  if (subBand && subBand.type === type) {
    return subBand.addTo;
  }

  return false;
}

/**
 * Helper. Returns true if the given band id in a list of bands is in overlay mode. False otherwise.
 */
export function isOverlay(bands: RavenCompositeBand[], bandId: string): boolean {
  const band = bandById(bands, bandId);

  if (band) {
    return band.overlay;
  }

  return false;
}
