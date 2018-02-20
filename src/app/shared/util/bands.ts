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
  RavenCompositeBand,
  RavenDividerBand,
  RavenResourceBand,
  RavenResourcePoint,
  RavenSource,
  RavenStateBand,
  RavenStatePoint,
  RavenSubBand,
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
 * This is a helper function that takes a list of current bands and a sourceId that was just clicked to "close",
 * and returns a list of band ids that we want to remove.
 */
export function removeBands(source: RavenSource, bands: RavenCompositeBand[]): string[] {
  const bandIds: string[] = [];

  bands.forEach((band: RavenCompositeBand) => {
    band.bands.forEach((subBand: RavenSubBand) => {
      if (subBand.sourceId === source.id) {
        bandIds.push(subBand.id);
      }
    });
  });

  return bandIds;
}

/**
 * Returns a data structure that transforms MpsServerGraphData to bands displayed in Raven.
 * Note that we do not worry about how these bands are displayed here. That is the job of the reducer.
 */
export function toRavenBandData(source: RavenSource, graphData: MpsServerGraphData): RavenSubBand[] {
  const metadata = graphData['Timeline Metadata'];
  const timelineData = graphData['Timeline Data'];

  if (metadata.hasTimelineType === 'measurement' && (metadata as MpsServerStateMetadata).hasValueType === 'string_xdr') {
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
  const bands: RavenActivityBand[] = [];
  const points: RavenActivityPoint[] = [];
  let legends: StringTMap<RavenActivityPoint[]> = {};

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
      sourceId: source.id,
      start,
      startTimestamp,
      uniqueId,
    });
  }

  legends = groupBy(points, 'legend');

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
      sourceId: '',
      sourceName: '',
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
export function toCompositeBand(band: RavenSubBand): RavenCompositeBand {
  const compositeBandUniqueId = v4();

  const compositeBand: RavenCompositeBand = {
    bands: [{
      ...band,
      parentUniqueId: compositeBandUniqueId,
    }],
    containerId: '0',
    height: band.height,
    heightPadding: band.heightPadding,
    id: compositeBandUniqueId,
    name: band.name,
    showTooltip: band.showTooltip,
    sortOrder: 0,
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
      sourceId: source.id,
      start,
      uniqueId,
      value,
    });
  }

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
    sourceId: '',
    sourceName: '',
    type: 'resource',
  };

  return resourceBand;
}

/**
 * Returns a state band given metadata and timelineData.
 */
export function toStateBand(source: RavenSource, metadata: MpsServerStateMetadata, timelineData: MpsServerStatePoint[]): RavenStateBand {
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
      sourceId: source.id,
      start,
      uniqueId,
      value,
    });
  }

  const stateBand: RavenStateBand = {
    alignLabel: 3,
    baselineLabel: 3,
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
    sourceId: '',
    sourceName: '',
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
    bands.forEach((band: RavenCompositeBand) => {
      band.bands.forEach((subBand: RavenSubBand) => {
        if (subBand.maxTimeRange.start < startTime) { startTime = subBand.maxTimeRange.start; }
        if (subBand.maxTimeRange.end > endTime) { endTime = subBand.maxTimeRange.end; }
      });
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
export function hasLegend(compositeBands: RavenCompositeBand[], band: RavenActivityBand): boolean {
  if (band.type === 'activity') {
    for (let i = 0, l = compositeBands.length; i < l; ++i) {
      for (let j = 0, k = compositeBands[i].bands.length; j < k; ++j) {
        const subBand = compositeBands[i].bands[j] as RavenActivityBand;

        if (subBand.type === 'activity' && subBand.legend === band.legend) {
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
