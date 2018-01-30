/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { groupBy, sortBy } from 'lodash';
import { v4 } from 'uuid';

import {
  MpsServerActivityPoint,
  MpsServerGraphData,
  MpsServerResourceMetadata,
  MpsServerResourcePoint,
  MpsServerStateMetadata,
  MpsServerStatePoint,
  RavenActivityBand,
  RavenActivityPoint,
  RavenBand,
  RavenBandData,
  RavenCompositeBand,
  RavenDividerBand,
  RavenResourceBand,
  RavenStateBand,
  RavenTimeRange,
  StringTMap,
} from './../models';

import {
  getMaxTimeRangeForPoints,
  mpsServerToRavenActivityPoints,
  mpsServerToRavenResourcePoints,
  mpsServerToRavenStatePoints,
} from './points';

/**
 * This is a helper function that takes a list of bands and a sourceId that was just clicked to "close".
 *
 * If a band has a reference to the sourceId then we check:
 * 1. If the band has only one source reference, then we want to remove it, so push it's id to removeBandIds.
 * 2. If the band has more than one source reference, then we just want to remove only
 *    the points from that sourceId, so push it's id to removePointsBandIds.
 */
export function removeBandsOrPoints(sourceId: string, bands: RavenBand[]) {
  const removeBandIds: string[] = [];
  const removePointsBandIds: string[] = [];

  bands.forEach((band: RavenBand) => {
    // If the band has the source id we are closing.
    if (band.sourceIds && band.sourceIds[sourceId]) {
      const sourceIds = Object.keys(band.sourceIds);

      if (sourceIds.length === 1) {
        // If the source id we are closing is the only id this band references,
        // then we can safely remove the entire band.
        removeBandIds.push(band.id);
      } else if (sourceIds.length > 1) {
        // Otherwise this band has points from more than one source.
        // So remove points in this band only for the source we are closing.
        removePointsBandIds.push(band.id);
      }
    }
  });

  return {
    removeBandIds,
    removePointsBandIds,
  };
}

/**
 * Helper that removes same-legend activity bands from potential bands if necessary.
 *
 * If an current and potential band are activity bands with the same name (and thus the same legend),
 * then remove the band from the potential band list, and add it's points to a hash: current band id => points.
 */
export function removeSameLegendActivityBands(currentBands: RavenBand[], potentialBands: RavenBand[]): RavenBandData {
  const newBands: RavenBand[] = [...potentialBands];
  const bandIdsToPoints: StringTMap<RavenActivityPoint[]> = {};

  currentBands.forEach((currentBand: RavenBand) => {
    if (currentBand.type === 'activity') {
      newBands.forEach((potentialBand, i) => {
        if (potentialBand.type === 'activity' && currentBand.name === potentialBand.name) {
          bandIdsToPoints[currentBand.id] = (potentialBand as RavenActivityBand).points; // This will be used in a reducer to add the potential band points to the current band.
          newBands.splice(i, 1); // Remove the potential band since it's points will be added to the current band in the reducer.
        }
      });
    }
  });

  return {
    bandIdsToPoints,
    bands: newBands,
  };
}

/**
 * Takes the list of currentBands in Raven and builds a list of new bands based on the graphData.
 * These new bands need further processing to determine if they will actually be displayed in Raven because
 * some potentialBands may just contribute their data to already existing bands.
 */
export function getPotentialBands(sourceId: string, graphData: MpsServerGraphData, currentBands: RavenBand[]): RavenBand[] {
  let potentialBands: RavenBand[] = [];

  if (graphData) {
    const metadata = graphData['Timeline Metadata'];
    const timelineData = graphData['Timeline Data'];

    if (metadata && timelineData) {
      if (metadata.hasTimelineType === 'measurement') {
        if ((metadata as MpsServerResourceMetadata | MpsServerStateMetadata).hasValueType === 'string_xdr') {
          // State (graphData only maps to a single band so we push it).
          potentialBands.push(toStateBand(sourceId, metadata as MpsServerStateMetadata, timelineData as MpsServerStatePoint[]));
        } else {
          // Resource (graphData only maps to a single band so we push it).
          potentialBands.push(toResourceBand(sourceId, metadata as MpsServerResourceMetadata, timelineData as MpsServerResourcePoint[]));
        }
      } else if (metadata.hasTimelineType === 'activity') {
        // Activity (graphData may map to many bands so we concat them to bands).
        potentialBands = potentialBands.concat(toActivityBands(sourceId, timelineData as MpsServerActivityPoint[]));
      }
    }
  }

  return potentialBands;
}

/**
 * Returns a data structure that transforms MpsServerGraphData to bands or points displayed in Raven.
 * 1. bandIdsToPoints: This is a hash that maps current band ids that are displayed in Raven to new points we need to display in those bands.
 * 2. bandIdsToPointsKeys: Just all the keys from 'bandIdsToPoints'.
 * 3. newBands: Any new bands we need to display.
 */
export function toRavenBandData(sourceId: string, graphData: MpsServerGraphData, currentBands: RavenBand[]): RavenBandData {
  const potentialBands: RavenBand[] = getPotentialBands(sourceId, graphData, currentBands);
  const { bandIdsToPoints = {}, bands = [] } = removeSameLegendActivityBands(currentBands, potentialBands);

  return {
    bandIdsToPoints,
    bands,
  };
}

/**
 * Returns a list of bands based on timelineData and point legends.
 */
export function toActivityBands(sourceId: string, timelineData: MpsServerActivityPoint[]): RavenActivityBand[] {
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
  const activityBand: RavenActivityBand = {
    activityStyle: 1,
    alignLabel: 3,
    baselineLabel: 3,
    containerId: '0',
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
    sortOrder: 0,
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

  const compositeBand: RavenCompositeBand = {
    bands,
    height: 100,
    heightPadding: 10,
    id: compositeBandUniqueId,
    name: compositeBandName,
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

  const dividerBand: RavenDividerBand = {
    color: [255, 255, 255],
    height: 7,
    id,
    label: `Divider ${id}`,
    labelColor: [0, 0, 0],
    minorLabels: [],
    name: `Divider ${id}`,
    showTooltip: true,
    type: 'divider',
  };

  return dividerBand;
}

/**
 * Returns a resource band given metadata and timelineData.
 */
export function toResourceBand(sourceId: string, metadata: MpsServerResourceMetadata, timelineData: MpsServerResourcePoint[]): RavenResourceBand {
  // Map raw resource timeline data to points for a band.
  const points = mpsServerToRavenResourcePoints(sourceId, timelineData);

  const resourceBand: RavenResourceBand = {
    autoTickValues: true,
    color: [0, 0, 0],
    containerId: '0',
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
    sortOrder: 0,
    sourceIds: {}, // Map of source ids this band has data from.
    type: 'resource',
  };

  return resourceBand;
}

/**
 * Returns a state band given metadata and timelineData.
 */
export function toStateBand(sourceId: string, metadata: MpsServerStateMetadata, timelineData: MpsServerStatePoint[]): RavenStateBand {
  // Map raw state timeline data (stringXdr type in MPS Server) to points for a band.
  const points = mpsServerToRavenStatePoints(sourceId, timelineData);

  const stateBand: RavenStateBand = {
    alignLabel: 3,
    baselineLabel: 3,
    containerId: '0',
    height: 50,
    heightPadding: 0,
    id: v4(),
    label: metadata.hasObjectName,
    labelColor: [0, 0, 0],
    maxTimeRange: getMaxTimeRangeForPoints(points),
    minorLabels: [],
    name: metadata.hasObjectName,
    parentUniqueId: null,
    points,
    showTooltip: true,
    sortOrder: 0,
    sourceIds: {}, // Map of source ids this band has data from.
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
export function updateSortOrder(bands: RavenBand[]): RavenBand[] {
  const sortByBands = sortBy(bands, 'containerId', 'sortOrder');
  const index = {}; // Hash of containerIds to a given index (indices start at 0).

  return sortByBands.map((band: RavenBand) => {
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
export function updateTimeRanges(currentViewTimeRange: RavenTimeRange, bands: RavenBand[]) {
  let maxTimeRange: RavenTimeRange = { end: 0, start: 0 };
  let viewTimeRange: RavenTimeRange = { end: 0, start: 0 };

  if (bands.length > 0) {
    let endTime = Number.MIN_SAFE_INTEGER;
    let startTime = Number.MAX_SAFE_INTEGER;

    // Calculate the maxTimeRange out of every band (including composite bands).
    bands.forEach((band: RavenBand) => {
      if (band.type !== 'composite') {
        if (band.maxTimeRange.start < startTime) { startTime = band.maxTimeRange.start; }
        if (band.maxTimeRange.end > endTime) { endTime = band.maxTimeRange.end; }
      } else if (band.type === 'composite') {
        (band as any).bands.forEach((subBand: RavenBand) => {
          if (subBand.maxTimeRange) {
            if (subBand.maxTimeRange.start < startTime) { startTime = subBand.maxTimeRange.start; }
            if (subBand.maxTimeRange.end > endTime) { endTime = subBand.maxTimeRange.end; }
          }
        });
      } else {
        // TODO: Error case.
      }
    });

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
