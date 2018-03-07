/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { v4 } from 'uuid';

import {
  MpsServerActivityPoint,
  MpsServerActivityPointMetadata,
  MpsServerPoint,
  MpsServerResourcePoint,
  MpsServerStatePoint,
  RavenActivityBand,
  RavenActivityPoint,
  RavenResourcePoint,
  RavenStatePoint,
  RavenSubBand,
  StringTMap,
} from './../models';

import {
  timestamp,
  utc,
} from './time';

/**
 * Helper that gets a color from activity metadata.
 */
export function getColorFromActivityMetadata(metadata: MpsServerActivityPointMetadata[]): number[] {
  const colorMap: StringTMap<number[]> = {
    'Aquamarine': [193, 226, 236],
    'Cadet Blue': [92, 144, 198],
    'Dodger Blue': [66, 130, 198],
    'Hot Pink': [245, 105, 171],
    'Khaki': [249, 217, 119],
    'Lavender': [218, 154, 190],
    'Orange': [249, 189, 133],
    'Orange Red': [244, 145, 19],
    'Pink': [245, 213, 228],
    'Plum': [176, 150, 193],
    'Purple': [144, 111, 169],
    'Salmon': [255, 191, 193],
    'Sky Blue': [166, 203, 240],
    'Spring Green': [124, 191, 183],
    'Violet Red': [183, 80, 163],
    'Yellow': [245, 202, 46],
    'color1': [0x5f, 0x99, 0x00],
    'color2': [0x00, 0x93, 0xc3],
    'color3': [0xee, 0x99, 0x33],
    'color4': [0xcF, 0x30, 0x30],
    'color5': [0x89, 750, 0xD9],
    'color6': [0x3D, 0x3A, 0xAD],
    'color7': [0xEC, 0x82, 0xB2],
    'color8': [0x57, 0x57, 0x57],
  };

  let color = [0, 0, 0];

  for (let i = 0, l = metadata.length; i < l; ++i) {
    const data: MpsServerActivityPointMetadata = metadata[i];

    if (data.Name.toLowerCase() === 'color') {
      const newColor = colorMap[data.Value];

      if (newColor) {
        color = newColor;
      }
    }
  }

  return color;
}

/**
 * Helper that returns points for a given sub-band type.
 */
export function getPointsBySubBandType(subBand: RavenSubBand, timelineData: MpsServerPoint[]) {
  switch (subBand.type) {
    case 'activity':
      const { legends, maxTimeRange } = getActivityPointsByLegend(subBand.sourceId, timelineData as MpsServerActivityPoint[]);
      return {
        maxTimeRange,
        points: legends[(subBand as RavenActivityBand).legend],
      };
    case 'resource':
      return getResourcePoints(subBand.sourceId, timelineData as MpsServerResourcePoint[]);
    case 'state':
      return getStatePoints(subBand.sourceId, timelineData as MpsServerStatePoint[]);
    default:
      return {
        maxTimeRange: {
          end: 0,
          start: 0,
        },
        points: [],
      };
  }
}

/**
 * Transforms an MpsServerActivityPoint to a RavenActivityPoint.
 */
export function getActivityPoint(sourceId: string, data: MpsServerActivityPoint): RavenActivityPoint {
  const activityId = data['Activity ID'];
  const activityName = data['Activity Name'];
  const activityParameters = data['Activity Parameters'];
  const activityType = data['Activity Type'];
  const ancestors = data.ancestors;
  const childrenUrl = data.childrenUrl;
  const color = getColorFromActivityMetadata(data.Metadata);
  const descendantsUrl = data.descendantsUrl;
  const endTimestamp = data['Tend Assigned'];
  const id = data.__document_id;
  const metadata = data.Metadata;
  const startTimestamp = data['Tstart Assigned'];
  const uniqueId = v4();

  const start = utc(startTimestamp);
  const end = utc(endTimestamp);
  const duration = end - start;

  let legend = '';
  if (data.Metadata) {
    for (let j = 0, ll = data.Metadata.length; j < ll; ++j) {
      const d = data.Metadata[j];

      if (d.Name === 'legend') {
        legend = d.Value;
      }
    }
  }

  const point: RavenActivityPoint = {
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
    id,
    legend,
    metadata,
    sourceId,
    start,
    startTimestamp,
    uniqueId,
  };

  return point;
}

/**
 * Transforms MPS Server activity points of a given type to Raven activity points bucketed by legend. Also returns the max and min point times.
 * Note that for performance we are only looping through timelineData once.
 */
export function getActivityPointsByLegend(sourceId: string, timelineData: MpsServerActivityPoint[]) {
  const legends: StringTMap<RavenActivityPoint[]> = {};

  let maxTime = Number.MIN_SAFE_INTEGER;
  let minTime = Number.MAX_SAFE_INTEGER;

  for (let i = 0, l = timelineData.length; i < l; ++i) {
    const data: MpsServerActivityPoint = timelineData[i];
    const point: RavenActivityPoint = getActivityPoint(sourceId, data);

    if (point.start < minTime) { minTime = point.start; }
    if (point.end > maxTime) { maxTime = point.end; }

    // Group points by legend manually so we don't have to loop through timelineData twice.
    if (legends[point.legend]) {
      legends[point.legend].push(point);
    } else {
      legends[point.legend] = [point];
    }
  }

  return {
    legends,
    maxTimeRange: {
      end: maxTime,
      start: minTime,
    },
  };
}

/**
 * Transforms MPS Server resource points to Raven resource points. Also returns the max and min point times.
 * Note that for performance we are only looping through timelineData once.
 */
export function getResourcePoints(sourceId: string, timelineData: MpsServerResourcePoint[]) {
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

  return {
    maxTimeRange: {
      end: maxTime,
      start: minTime,
    },
    points,
  };
}

/**
 * Transforms MPS Server state points to Raven state points. Also returns the max and min point times.
 * Note that for performance we are only looping through timelineData once.
 */
export function getStatePoints(sourceId: string, timelineData: MpsServerStatePoint[]) {
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

  return {
    maxTimeRange: {
      end: maxTime,
      start: minTime,
    },
    points,
  };
}
