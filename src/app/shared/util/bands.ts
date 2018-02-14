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
  RavenActivityBandUpdate,
  RavenActivityPoint,
  RavenBand,
  RavenBandData,
  RavenCompositeBand,
  RavenDividerBand,
  RavenRemoveBandIds,
  RavenResourceBand,
  RavenResourcePoint,
  RavenStateBand,
  RavenStatePoint,
  RavenTimeRange,
  StringTMap,
} from './../models';

import {
  getColorFromMetadata,
} from './points';

import {
  timestamp,
  utc,
} from './time';

/**
 * This is a helper function that takes a list of bands and a sourceId that was just clicked to "close".
 *
 * If a band has a reference to the sourceId then we check:
 * 1. If the band has only one source reference, then we want to remove it, so push it's id to bandIds.
 * 2. If the band has more than one source reference, then we just want to remove only
 *    the points from that sourceId, so push it's id to pointsBandIds.
 */
export function removeBandsOrPoints(sourceId: string, bands: RavenBand[]): RavenRemoveBandIds {
  const bandIds: string[] = [];
  const pointsBandIds: string[] = [];

  for (let i = 0, l = bands.length; i < l; ++i) {
    const band: RavenBand = bands[i];

    // If the band has the source id we are closing.
    if (band.sourceIds && band.sourceIds[sourceId]) {
      const sourceIds = Object.keys(band.sourceIds);

      if (sourceIds.length === 1) {
        // If the source id we are closing is the only id this band references,
        // then we can safely remove the entire band.
        bandIds.push(band.id);
      } else if (sourceIds.length > 1) {
        // Otherwise this band has points from more than one source.
        // So remove points in this band only for the source we are closing.
        pointsBandIds.push(band.id);
      }
    }
  }

  return {
    bandIds,
    pointsBandIds,
  };
}

/**
 * Returns a data structure that transforms MpsServerGraphData to bands or points displayed in Raven.
 */
export function toRavenBandData(sourceId: string, graphData: MpsServerGraphData, currentBands: RavenBand[]): RavenBandData {
  const metadata = graphData['Timeline Metadata'];
  const timelineData = graphData['Timeline Data'];

  if (metadata.hasTimelineType === 'measurement' && (metadata as MpsServerStateMetadata).hasValueType === 'string_xdr') {
    // Resource.
    return {
      newBands: [toStateBand(sourceId, metadata as MpsServerStateMetadata, timelineData as MpsServerStatePoint[])],
      updateActivityBands: {},
    };
  } else if (metadata.hasTimelineType === 'measurement') {
    // State.
    return {
      newBands: [toResourceBand(sourceId, metadata as MpsServerResourceMetadata, timelineData as MpsServerResourcePoint[])],
      updateActivityBands: {},
    };
  } else if (metadata.hasTimelineType === 'activity') {
    // Activity.
    const updateActivityBands: StringTMap<RavenActivityBandUpdate> = {};

    const newBands = toActivityBands(sourceId, timelineData as MpsServerActivityPoint[])
      .filter((activityBand: RavenActivityBand) => {
        for (let i = 0, l = currentBands.length; i < l; ++i) {
          const currentBand = currentBands[i];

          if (activityBand.name === currentBand.name) {
            updateActivityBands[currentBand.id] = {
              name: activityBand.name,
              points: activityBand.points,
            } as RavenActivityBandUpdate;

            return false;
          }
        }
        return true;
      });

    return {
      newBands,
      updateActivityBands,
    };
  } else {
    console.warn('raven2 - bands.ts - toRavenBandData: graphData has a type we do not recognize: ', metadata.hasTimelineType);

    return {
      newBands: [],
      updateActivityBands: {},
    };
  }
}

/**
 * Returns a list of bands based on timelineData and point legends.
 */
export function toActivityBands(sourceId: string, timelineData: MpsServerActivityPoint[]): RavenActivityBand[] {
  const bands: RavenActivityBand[] = [];
  const points: RavenActivityPoint[] = [];

  let maxTime = Number.MIN_SAFE_INTEGER;
  let minTime = Number.MAX_SAFE_INTEGER;

  for (let i = 0, l = timelineData.length; i < l; ++i) {
    const data: MpsServerActivityPoint = timelineData[i];

    const activityId = data['Activity ID'];
    const activityName = data['Activity Name'];
    const activityParameters = data['Activity Parameters'];
    const activityType = data['Activity Type'];
    const ancestors = data.ancestors;
    const childrenUrl = data.childrenUrl;
    const color = getColorFromMetadata(data.Metadata);
    const descendantsUrl = data.descendantsUrl;
    const endTimestamp = data['Tend Assigned'];
    const id = data.__document_id;
    const metadata = data.Metadata;
    const startTimestamp = data['Tstart Assigned'];
    const uniqueId = v4();

    const start = utc(startTimestamp);
    const end = utc(endTimestamp);
    const duration = end - start;

    let hasLegend = false;
    let legend = '';
    if (data.Metadata) {
      for (let j = 0, len = data.Metadata.length; j < len; ++j) {
        const d = data.Metadata[j];

        if (d.Name === 'legend') {
          legend = d.Value;
          hasLegend = true;
        }
      }
    }

    if (start < minTime) { minTime = start; }
    if (end > maxTime) { maxTime = end; }

    points.push({
      activityId,
      activityName,
      activityParameters,
      activityType,
      ancestors,
      childrenUrl,
      color,
      descendantsUrl,
      duration,
      end,
      endTimestamp,
      hasLegend,
      id,
      legend,
      metadata,
      sourceId,
      start,
      startTimestamp,
      uniqueId,
    });
  }

  const legends = groupBy(points, 'legend');
  const legendKeys = Object.keys(legends);

  // Map each legend to a band.
  for (let i = 0, l = legendKeys.length; i < l; ++i) {
    const legend = legendKeys[i];

    const activityBand: RavenActivityBand = {
      activityStyle: 1,
      alignLabel: 3,
      baselineLabel: 3,
      containerId: '0',
      height: 50,
      heightPadding: 10,
      id: v4(),
      label: legend,
      labelColor: [0, 0, 0],
      layout: 1,
      maxTimeRange: {
        end: maxTime,
        start: minTime,
      },
      minorLabels: [],
      name: legend,
      parentUniqueId: null,
      points: legends[legend],
      showLabel: true,
      showTooltip: true,
      sortOrder: 0,
      sourceIds: {}, // Map of source ids this band has data from.
      trimLabel: true,
      type: 'activity',
    };

    bands.push(activityBand);
  }

  return bands;
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
  const points: RavenResourcePoint[] = [];

  let maxTime = Number.MIN_SAFE_INTEGER;
  let minTime = Number.MAX_SAFE_INTEGER;

  for (let i = 0, l = timelineData.length; i < l; ++i) {
    const data: MpsServerResourcePoint = timelineData[i];

    const id = data.__document_id;
    const start = utc(data['Data Timestamp']);
    const uniqueId = v4();
    const value = data['Data Value'];

    if (start < minTime) { minTime = start; }
    if (start > maxTime) { maxTime = start; }

    points.push({
      duration: null,
      id,
      sourceId,
      start,
      uniqueId,
      value,
    });
  }

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
    maxTimeRange: {
      end: maxTime,
      start: minTime,
    },
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
  const points: RavenStatePoint[] = [];

  let maxTime = Number.MIN_SAFE_INTEGER;
  let minTime = Number.MAX_SAFE_INTEGER;

  for (let i = 0, l = timelineData.length; i < l; ++i) {
    const data: MpsServerStatePoint = timelineData[i];

    const id = data.__document_id;
    const start = utc(data['Data Timestamp']);
    const startTimestamp = data['Data Timestamp'];
    const uniqueId = v4();
    const value = data['Data Value'];

    // This may or may not be correct. We're making an assumption that if there's no end,
    // we're going to draw to the end of the day.
    const startTimePlusDelta = utc(startTimestamp) + 30;
    const endTimestamp = timelineData[i + 1] !== undefined ? timelineData[i + 1]['Data Timestamp'] : timestamp(startTimePlusDelta);
    const duration = utc(endTimestamp) - utc(startTimestamp);
    const end = start + duration;

    if (start < minTime) { minTime = start; }
    if (end > maxTime) { maxTime = end; }

    points.push({
      duration,
      end,
      id,
      interpolateEnding: true,
      sourceId,
      start,
      uniqueId,
      value,
    });
  }

  const stateBand: RavenStateBand = {
    alignLabel: 3,
    baselineLabel: 3,
    containerId: '0',
    height: 50,
    heightPadding: 0,
    id: v4(),
    label: metadata.hasObjectName,
    labelColor: [0, 0, 0],
    maxTimeRange: {
      end: maxTime,
      start: minTime,
    },
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
