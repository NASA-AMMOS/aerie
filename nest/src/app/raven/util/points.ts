/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import uniqueId from 'lodash-es/uniqueId';
import { StringTMap } from '../../shared/models';
import { fromDuration, timestamp, utc } from '../../shared/util/time';
import {
  MpsServerActivityPoint,
  MpsServerActivityPointMetadata,
  MpsServerResourceMetadata,
  MpsServerResourcePoint,
  MpsServerStatePoint,
  RavenActivityPoint,
  RavenActivityPointInBand,
  RavenCompositeBand,
  RavenPoint,
  RavenResourcePoint,
  RavenStatePoint,
} from '../models';
import { colorMap, colorRgbArrayToHex, getRandomColor } from './color';

/**
 * Helper that set hidden for activities not matching filter.
 */
export function filterActivityPoints(
  points: RavenActivityPoint[],
  filter: string,
) {
  points = points.map((point: RavenActivityPoint) => {
    if (filter.length > 0) {
      const match = point.activityName.match(new RegExp(filter));
      return { ...point, hidden: match === null };
    } else {
      return { ...point, hidden: false };
    }
  });
  return points;
}

/**
 * Helper that gets a color from activity metadata.
 */
export function getColorFromActivityMetadata(
  metadata: MpsServerActivityPointMetadata[],
): string {
  let color = null;

  for (let i = 0, l = metadata.length; i < l; ++i) {
    const data: MpsServerActivityPointMetadata = metadata[i];

    if (data.Name.toLowerCase() === 'color') {
      if (Array.isArray(data.Value)) {
        return colorRgbArrayToHex(data.Value);
      } else {
        if (data.Value.startsWith('#')) {
          return data.Value;
        } else {
          color = colorMap[data.Value];
        }
      }
    }
  }

  return color ? color : getRandomColor().color;
}

/**
 * Helper that gets the message in the activity metadata.
 * For activities with 'message' metadata, the labels are hidden and the tooltips for activities should display the 'message' content instead of the activity name.
 */
export function getMessage(metadata: MpsServerActivityPointMetadata[] | null) {
  if (metadata) {
    for (let i = 0, l = metadata.length; i < l; ++i) {
      const data = metadata[i];
      if (data.Name === 'message') {
        return data.Value;
      }
    }
  }
  return null;
}

/**
 * Transforms an MpsServerActivityPoint to a RavenActivityPoint.
 */
export function getActivityPoint(
  sourceId: string,
  data: MpsServerActivityPoint,
  expandedFromPointId: string | null,
): RavenActivityPoint {
  const activityId = data['Activity ID'];
  const activityName = data['Activity Name'];
  const activityParameters = data['Activity Parameters'];
  const activityType = data['Activity Type'];
  const ancestors = data.ancestors;
  const args = data.Arguments;
  const childrenUrl = data.childrenUrl;
  const color = data.Metadata
    ? getColorFromActivityMetadata(data.Metadata)
    : getRandomColor().color;
  const descendantsUrl = data.descendantsUrl;
  const endTimestamp = data['Tend Assigned'];
  const id = data.__document_id;
  const keywordLine = data['Keyword Line'];
  const message = getMessage(data.Metadata);
  const metadata = data.Metadata;
  const startTimestamp = data['Tstart Assigned'];

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
    arguments: args,
    childrenUrl,
    color,
    descendantsUrl,
    duration,
    end,
    endTimestamp,
    expandedFromPointId,
    expansion: 'noExpansion',
    hidden: false,
    id,
    keywordLine,
    legend,
    message,
    metadata,
    sourceId,
    start,
    startTimestamp,
    subBandId: '',
    type: 'activity',
    uniqueId: '',
  };

  // Set the unique activity id with the newly created point object.
  // See `getUniqueActivityId` for why `activityId` by itself does not suffice as a `uniqueId`.
  point.uniqueId = getUniqueActivityId(point);

  return point;
}

/**
 * Transforms MPS Server activity points of a given type to Raven activity points bucketed by legend. Also returns the max and min point times.
 * Note that for performance we are only looping through timelineData once.
 */
export function getActivityPointsByLegend(
  sourceId: string,
  sourceName: string,
  expandedFromPointId: string | null,
  timelineData: MpsServerActivityPoint[],
) {
  const legends: StringTMap<RavenActivityPoint[]> = {};

  let maxTime = Number.MIN_SAFE_INTEGER;
  let minTime = Number.MAX_SAFE_INTEGER;

  for (let i = 0, l = timelineData.length; i < l; ++i) {
    const data: MpsServerActivityPoint = timelineData[i];
    const point: RavenActivityPoint = getActivityPoint(
      sourceId,
      data,
      expandedFromPointId,
    );

    if (point.start < minTime) {
      minTime = point.start;
    }
    if (point.end > maxTime) {
      maxTime = point.end;
    }

    // Group points by legend manually so we don't have to loop through timelineData twice.
    if (!point.legend) {
      if (legends[sourceName]) {
        legends[sourceName].push(point);
      } else {
        legends[sourceName] = [point];
      }
    } else if (legends[point.legend]) {
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
export function getResourcePoints(
  sourceId: string,
  metadata: MpsServerResourceMetadata,
  timelineData: MpsServerResourcePoint[],
) {
  const points: RavenResourcePoint[] = [];
  const isDuration = metadata.hasValueType.toLowerCase() === 'duration';
  const isTime = metadata.hasValueType.toLowerCase() === 'time';

  let maxTime = Number.MIN_SAFE_INTEGER;
  let minTime = Number.MAX_SAFE_INTEGER;

  for (let i = 0, l = timelineData.length; i < l; ++i) {
    const data: MpsServerResourcePoint = timelineData[i];

    const id = data.__document_id;
    const start = utc(data['Data Timestamp']);
    let value = data['Data Value'];

    if (isDuration && typeof value === 'string') {
      value = fromDuration(value as string);
    } else if (isTime) {
      value = utc(value as string);
    }

    if (start < minTime) {
      minTime = start;
    }
    if (start > maxTime) {
      maxTime = start;
    }

    points.push({
      duration: null,
      id,
      isDuration,
      isTime,
      sourceId,
      start,
      subBandId: '',
      type: 'resource',
      uniqueId: uniqueId(),
      value: value as number,
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
export function getStatePoints(
  sourceId: string,
  timelineData: MpsServerStatePoint[],
) {
  const points: RavenStatePoint[] = [];

  let maxTime = Number.MIN_SAFE_INTEGER;
  let minTime = Number.MAX_SAFE_INTEGER;

  for (let i = 0, l = timelineData.length; i < l; ++i) {
    const data: MpsServerStatePoint = timelineData[i];

    const id = data.__document_id;
    const start = utc(data['Data Timestamp']);
    const startTimestamp = data['Data Timestamp'];
    const value = data['Data Value'];

    // This may or may not be correct. We're making an assumption that if there's no end,
    // we're going to draw to the end of the day.
    const startTimePlusDelta = utc(startTimestamp) + 30;
    const endTimestamp =
      timelineData[i + 1] !== undefined
        ? timelineData[i + 1]['Data Timestamp']
        : timestamp(startTimePlusDelta);
    const duration = utc(endTimestamp) - utc(startTimestamp);
    const end = start + duration;

    if (start < minTime) {
      minTime = start;
    }
    if (end > maxTime) {
      maxTime = end;
    }

    points.push({
      duration,
      end,
      id,
      interpolateEnding: true,
      sourceId,
      start,
      subBandId: '',
      type: 'state',
      uniqueId: uniqueId(),
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
 * Get a raven point from a list of bands by bandId, subBandId, and pointId. Returns null if no point is found.
 */
export function getPoint(
  bands: RavenCompositeBand[],
  bandId: string | null,
  subBandId: string,
  pointId: string,
): RavenPoint | null {
  for (let i = 0, l = bands.length; i < l; ++i) {
    if (bandId === null || bands[i].id === bandId) {
      // Optional bandId.
      for (let j = 0, ll = bands[i].subBands.length; j < ll; ++j) {
        const subBand = bands[i].subBands[j];
        if (subBand.id === subBandId) {
          for (let k = 0, lll = subBand.points.length; k < lll; ++k) {
            if (subBand.points[k].uniqueId === pointId) {
              return {
                ...subBand.points[k],
                subBandId: subBand.id,
              };
            }
          }
        }
      }
    }
  }
  return null;
}

/**
 * Helper that returns an activityPoint for a given activityId in bands.
 */
export function getActivityPointInBand(
  bands: RavenCompositeBand[],
  activityId: string,
): RavenActivityPointInBand | null {
  for (let i = 0, l = bands.length; i < l; ++i) {
    for (let j = 0, ll = bands[i].subBands.length; j < ll; ++j) {
      const subBand = bands[i].subBands[j];
      for (let k = 0, lll = subBand.points.length; k < lll; ++k) {
        if (subBand.points[k].activityId === activityId) {
          return {
            activityPoint: subBand.points[k],
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
 * Helper that returns a unique activity id which is composed of the activity id and the start/end times.
 * In some rare cases (e.g. in a PEF) an activity id can be set to a non-unique string in the adaptation.
 * This adds the start/end times to ensure uniqueness.
 */
export function getUniqueActivityId(point: RavenActivityPoint): string {
  return point.activityId ? `${point.activityId}-${uniqueId()}` : uniqueId();
}

/**
 * Helper that checks if we need to reset the selectedPoint.
 * If we don't need to reset just return the original selectedPoint.
 */
export function updateSelectedPoint(
  bands: RavenCompositeBand[],
  selectedPoint: RavenPoint | null,
) {
  if (
    selectedPoint &&
    getPoint(bands, null, selectedPoint.subBandId, selectedPoint.uniqueId)
  ) {
    return {
      selectedPoint,
    };
  } else {
    return {
      selectedPoint: null,
    };
  }
}
