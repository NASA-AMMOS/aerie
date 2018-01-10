/*
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
  MpsServerResourcePoint,
  MpsServerStatePoint,
  RavenActivityPoint,
  RavenPoint,
  RavenResourcePoint,
  RavenStatePoint,
  RavenTimeRange,
  StringTMap,
} from './../models';

import {
  timestamp,
  utc,
} from './time';

/**
 * Transforms activity point timeline data from MPS Server to activity points consumable by Raven.
 */
export function mpsServerToRavenActivityPoints(sourceId: string, timelineData: MpsServerActivityPoint[]): RavenActivityPoint[] {
  const points: RavenActivityPoint[] = [];

  timelineData.forEach((data: MpsServerActivityPoint) => {
    const activityId = data['Activity ID'];
    const activityName = data['Activity Name'];
    const activityParameters = data['Activity Parameters'];
    const activityType = data['Activity Type'];
    const ancestors = data.ancestors;
    const childrenUrl = data.childrenUrl;
    const color = getActivityColorFromMetadata(data.Metadata);
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
      data.Metadata.forEach((d) => {
        if (d.Name === 'legend') {
          legend = d.Value;
          hasLegend = true;
        }
      });
    }

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
  });

  return points;
}

/**
 * Transforms resource point timeline data from MPS Server to resource points consumable by Raven.
 */
export function mpsServerToRavenResourcePoints(sourceId: string, timelineData: MpsServerResourcePoint[]): RavenResourcePoint[] {
  const points: RavenResourcePoint[] = [];

  timelineData.forEach((data) => {
    const id = data.__document_id;
    const start = utc(data['Data Timestamp']);
    const uniqueId = v4();
    const value = data['Data Value'];

    points.push({
      duration: null,
      id,
      sourceId,
      start,
      uniqueId,
      value,
    });
  });

  return points;
}

/**
 * Transforms state point timeline data from MPS Server to state points consumable by Raven.
 */
export function mpsServerToRavenStatePoints(sourceId: string, timelineData: MpsServerStatePoint[]): RavenStatePoint[] {
  const points: RavenStatePoint[] = [];

  timelineData.forEach((data, i) => {
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
  });

  return points;
}

/**
 * Helper that gets a color from activity metadata.
 */
export function getActivityColorFromMetadata(metadata: MpsServerActivityPointMetadata[]): number[] {
  let color = [0, 0, 0];

  metadata.forEach((data) => {
    if (data.Name.toLowerCase() === 'color') {
      color = getColor(data.Value);
    }
  });

  return color;
}

/**
 * Takes a string of a color name and returns an RGB representation of that color.
 */
export function getColor(color: string): number[] {
  // Helpful map of Activity and/or State colors.
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

  if (colorMap[color]) {
    return colorMap[color];
  }

  return [0, 0, 0];
}

/**
 * Finds a max time range for a list of points.
 */
export function getMaxTimeRangeForPoints(points: RavenPoint[]): RavenTimeRange {
  let maxTime = Number.MIN_SAFE_INTEGER;
  let minTime = Number.MAX_SAFE_INTEGER;

  points.forEach((point: RavenPoint) => {
    const end = point.duration ? point.start + point.duration : point.start;
    const start = point.start;

    if (start < minTime) {
      minTime = start;
    }

    if (end > maxTime) {
      maxTime = end;
    }
  });

  return {
    end: maxTime,
    start: minTime,
  };
}
