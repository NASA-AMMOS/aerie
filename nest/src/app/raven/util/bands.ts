/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import sortBy from 'lodash-es/sortBy';
import uniqueId from 'lodash-es/uniqueId';
import { StringTMap, TimeRange } from '../../shared/models';
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
  RavenCustomFilter,
  RavenCustomGraphableSource,
  RavenDefaultBandSettings,
  RavenDividerBand,
  RavenResourceBand,
  RavenSource,
  RavenStateBand,
  RavenSubBand,
} from '../models';
import {
  getActivityPointsByLegend,
  getResourcePoints,
  getStatePoints,
} from './points';

/**
 * Returns a data structure that transforms MpsServerGraphData to bands displayed in Raven.
 * Note that we do not worry about how these bands are displayed here.
 * We are just generating the band types for use elsewhere.
 */
export function toRavenBandData(
  sourceId: string,
  sourceName: string,
  graphData: MpsServerGraphData,
  defaultBandSettings: RavenDefaultBandSettings,
  customFilter: RavenCustomFilter | null,
  treeBySourceId: StringTMap<RavenSource>,
): RavenSubBand[] {
  const metadata = graphData['Timeline Metadata'];
  const timelineData = graphData['Timeline Data'];

  if (
    metadata.hasTimelineType === 'measurement' &&
    (metadata as MpsServerStateMetadata).hasValueType.startsWith('string')
  ) {
    // State.
    const stateBand = toStateBand(
      sourceId,
      metadata as MpsServerStateMetadata,
      timelineData as MpsServerStatePoint[],
      defaultBandSettings,
      treeBySourceId,
    );
    return [stateBand];
  } else if (
    metadata.hasTimelineType === 'measurement' ||
    metadata.hasTimelineType === 'state'
  ) {
    // Resource.
    const resourceBand = toResourceBand(
      sourceId,
      metadata as MpsServerResourceMetadata,
      timelineData as MpsServerResourcePoint[],
      defaultBandSettings,
      treeBySourceId,
    );
    return [resourceBand];
  } else if (metadata.hasTimelineType === 'activity') {
    // Activity.
    const activityBands = toActivityBands(
      sourceId,
      sourceName,
      null,
      timelineData as MpsServerActivityPoint[],
      defaultBandSettings,
      customFilter,
      treeBySourceId,
      metadata.editable,
    );
    return activityBands;
  } else {
    console.error(
      `raven2 - bands.ts - toRavenBandData - parameter 'graphData' has a timeline type we do not recognize: ${metadata.hasTimelineType}`,
    );
    return [];
  }
}

/**
 * Returns a list of activity points from children and recursively getting nested children.
 */
export function getChildren(children: MpsServerActivityPoint[]) {
  return children.reduce(function(acts: MpsServerActivityPoint[], child) {
    acts.push(child);
    if (child.children) {
      acts = acts.concat(getChildren(child.children));
      child.children = [];
    }
    return acts;
  }, []);
}

/**
 * Returns a data structure that transforms MpsServerGraphData containing children to bands displayed in Raven.
 * Note that we do not worry about how these bands are displayed here.
 * We are just generating the band types for use elsewhere.
 */
export function toRavenDescendantsData(
  parentSubBandId: string,
  expandedFromPointId: string,
  graphData: MpsServerGraphData,
  defaultBandSettings: RavenDefaultBandSettings,
  customFilter: RavenCustomFilter | null,
  treeBySourceId: StringTMap<RavenSource>,
): RavenSubBand[] {
  const metadata = graphData['Timeline Metadata'];

  if (metadata.hasTimelineType === 'activity') {
    const children = (graphData['Timeline Data'][0] as MpsServerActivityPoint)
      .children;
    const activityBands = toActivityBands(
      parentSubBandId,
      parentSubBandId,
      expandedFromPointId,
      getChildren(children),
      defaultBandSettings,
      customFilter,
      treeBySourceId,
      false,
    );
    return activityBands;
  } else {
    console.error(
      `raven2 - bands.ts - toRavenDescendantData - parameter 'graphData' has a timeline type not valid for descendants: ${metadata.hasTimelineType}`,
    );
    return [];
  }
}

/**
 * Returns a list of bands based on timelineData and point legends.
 *
 * Note: For bands with activity points containing 'message' or 'keywordLine', labels should not be shown.
 * Warnings, errors, and comments contain 'message'.
 * DKF spec advisories contain 'keywordLine'.
 * 'message' and 'keywordLine' are displayed in the tooltips for activities
 * containing 'message' or 'keywordLine' instead of the 'Activity Name' in regular activities.
 */
export function toActivityBands(
  sourceId: string,
  sourceName: string,
  expandedFromPointId: string | null,
  timelineData: MpsServerActivityPoint[],
  defaultBandSettings: RavenDefaultBandSettings,
  customFilter: RavenCustomFilter | null,
  treeBySourceId: StringTMap<RavenSource>,
  editable: boolean,
): RavenActivityBand[] {
  const { legends, maxTimeRange } = getActivityPointsByLegend(
    sourceId,
    sourceName,
    expandedFromPointId,
    timelineData,
    defaultBandSettings.activityInitiallyHidden,
  );
  const bands: RavenActivityBand[] = [];
  const customGraphableSource = treeBySourceId[sourceId]
    ? (treeBySourceId[sourceId] as RavenCustomGraphableSource)
    : null;

  // Map each legend to a band.
  Object.keys(legends).forEach(legend => {
    const activityBand: RavenActivityBand = {
      activityFilter: '',
      activityHeight: isMessageTypeActivity(legends[legend][0]) ? 5 : 20,
      activityLabelFontSize: 9,
      activityStyle: isMessageTypeActivity(legends[legend][0]) ? 2 : 1,
      addTo: false,
      alignLabel: 3,
      baselineLabel: 3,
      borderWidth: 1,
      editable: editable,
      filterTarget: null,
      height: 50,
      heightPadding: 10,
      icon: defaultBandSettings.icon,
      id: uniqueId(),
      label: `${legend}`,
      labelColor: '#000000',
      labelFont: defaultBandSettings.labelFont,
      labelPin: '',
      layout: defaultBandSettings.activityLayout,
      legend,
      maxTimeRange,
      minorLabels:
        customFilter && customFilter.filter && customGraphableSource
          ? [getFilterLabel(customGraphableSource, customFilter)]
          : [],
      name: legend,
      parentUniqueId: null,
      points: legends[legend],
      pointsChanged: false,
      showActivityTimes: false,
      showLabel: legends[legend][0].activityName !== undefined,
      showLabelPin: true,
      showTooltip: true,
      sourceIds: [sourceId],
      tableColumns: [],
      timeDelta: 0,
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
export function toCompositeBand(
  subBand: RavenSubBand,
  containerId?: string,
  sortOrder?: number,
  backgroundColor?: string,
): RavenCompositeBand {
  const compositeBandUniqueId = uniqueId();

  const compositeBand: RavenCompositeBand = {
    backgroundColor: backgroundColor || '#FFFFFF',
    compositeAutoScale: true,
    compositeLogTicks: false,
    compositeScientificNotation: false,
    compositeYAxisLabel: false,
    containerId: containerId || '0',
    height: subBand.height + subBand.heightPadding,
    heightPadding: subBand.heightPadding,
    id: compositeBandUniqueId,
    name: subBand.name,
    overlay: false, // Composite bands with a single sub-band cannot be overlay by default.
    showTooltip: subBand.showTooltip,
    sortOrder: sortOrder || 0,
    subBands: [
      {
        ...subBand,
      },
    ],
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
    color: '#ffffff',
    editable: false,
    height: 10,
    heightPadding: 0,
    id,
    label: `Divider ${id}`,
    labelColor: '#000000',
    labelPin: '',
    maxTimeRange: { start: 0, end: 0 },
    name: `Divider ${id}`,
    parentUniqueId: null,
    points: [],
    showTooltip: true,
    sourceIds: [],
    tableColumns: [],
    type: 'divider',
  };

  return dividerBand;
}

/**
 * Returns a resource band given metadata and timelineData.
 */
export function toResourceBand(
  sourceId: string,
  metadata: MpsServerResourceMetadata,
  timelineData: MpsServerResourcePoint[],
  defaultBandSettings: RavenDefaultBandSettings,
  treeBySourceId: StringTMap<RavenSource>,
): RavenResourceBand {
  const { maxTimeRange, points } = getResourcePoints(
    sourceId,
    metadata,
    timelineData,
  );

  const resourceBand: RavenResourceBand = {
    addTo: false,
    autoScale: true,
    color: defaultBandSettings.resourceColor,
    decimate: metadata.decimatedData,
    editable: metadata.editable,
    fill: false,
    fillColor: defaultBandSettings.resourceFillColor,
    height: 100,
    heightPadding: 10,
    icon: defaultBandSettings.icon,
    id: uniqueId(),
    interpolation: metadata.hasInterpolatorType || 'linear',
    isDuration: metadata.hasValueType.toLowerCase() === 'duration',
    isTime: metadata.hasValueType.toLowerCase() === 'time',
    label: metadata.hasObjectName,
    labelColor: '#000000',
    labelFont: defaultBandSettings.labelFont,
    labelPin: '',
    labelUnit: metadata.hasUnits || '',
    logTicks: false,
    maxTimeRange,
    name: metadata.hasObjectName,
    parentUniqueId: null,
    points,
    pointsChanged: false,
    scientificNotation: false,
    showIcon: false,
    showLabelPin: true,
    showLabelUnit: true,
    showTooltip: true,
    sourceIds: [sourceId],
    tableColumns: [],
    timeDelta: 0,
    type: 'resource',
  };
  if (metadata.maxLimit !== undefined) {
    resourceBand.maxLimit = metadata.maxLimit;
  }
  if (metadata.minLimit !== undefined) {
    resourceBand.minLimit = metadata.minLimit;
  }

  return resourceBand;
}

/**
 * Returns a state band given metadata and timelineData.
 */
export function toStateBand(
  sourceId: string,
  metadata: MpsServerStateMetadata,
  timelineData: MpsServerStatePoint[],
  defaultBandSettings: RavenDefaultBandSettings,
  treeBySourceId: StringTMap<RavenSource>,
): RavenStateBand {
  const { maxTimeRange, points } = getStatePoints(sourceId, timelineData);

  const stateBand: RavenStateBand = {
    addTo: false,
    alignLabel: 3,
    baselineLabel: 3,
    borderWidth: 1,
    color: defaultBandSettings.resourceColor,
    editable: metadata.editable,
    fill: false,
    fillColor: defaultBandSettings.resourceFillColor,
    height: 50,
    heightPadding: 0,
    icon: defaultBandSettings.icon,
    id: uniqueId(),
    isNumeric: false,
    label: metadata.hasObjectName,
    labelColor: '#000000',
    labelFont: defaultBandSettings.labelFont,
    labelPin: '',
    maxTimeRange,
    name: metadata.hasObjectName,
    parentUniqueId: null,
    points,
    pointsChanged: false,
    possibleStates: metadata.hasPossibleStates
      ? metadata.hasPossibleStates
      : [],
    showIcon: false,
    showLabelPin: true,
    showStateChangeTimes: false,
    showTooltip: true,
    sourceIds: [sourceId],
    stateLabelFontSize: 9,
    tableColumns: [],
    timeDelta: 0,
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
export function updateSortOrder(
  bands: RavenCompositeBand[],
): RavenCompositeBand[] {
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
 * Helper that finds the sortOrder of a band by its id.
 */
export function sortOrderForBand(
  bands: RavenCompositeBand[],
  bandId: string,
): number {
  const bandIndex = bands
    .filter(b => b.containerId === '0')
    .findIndex(b => b.id === bandId);

  if (bandIndex !== -1) {
    return bands[bandIndex].sortOrder;
  } else {
    return bands.filter(b => b.containerId === '0').length - 1;
  }
}

/**
 * Helper that gets new time ranges based on the current view time range and the list of given bands.
 * Need to pad end time in order to view the last point or when there is only one instantaneous point.
 */
export function updateTimeRanges(
  bands: RavenCompositeBand[],
  currentViewTimeRange: TimeRange,
) {
  const padTime = 10; // 10 secs
  let maxTimeRange: TimeRange = { end: 0, start: 0 };
  let viewTimeRange: TimeRange = { end: 0, start: 0 };

  if (bands.length > 0) {
    let endTime = Number.MIN_SAFE_INTEGER;
    let startTime = Number.MAX_SAFE_INTEGER;

    // Calculate the maxTimeRange out of every band.
    for (let i = 0, l = bands.length; i < l; ++i) {
      const band = bands[i];

      for (let j = 0, ll = band.subBands.length; j < ll; ++j) {
        const subBand = band.subBands[j];

        // Since dividers don't really have a time range, make sure we do not re-calc time for them.
        // Also make sure we dont account for 0's in maxTimeRange (e.g. when loading layouts).
        if (subBand.type !== 'divider') {
          if (
            subBand.maxTimeRange.start !== 0 &&
            subBand.maxTimeRange.start < startTime
          ) {
            startTime = subBand.maxTimeRange.start;
          }
          if (
            subBand.maxTimeRange.end !== 0 &&
            subBand.maxTimeRange.end > endTime
          ) {
            endTime = subBand.maxTimeRange.end;
          }
        }
      }
    }

    endTime += padTime;

    maxTimeRange = { end: endTime, start: startTime };
    viewTimeRange = { ...currentViewTimeRange };

    // Re-set viewTimeRange to maxTimeRange if both start and end are 0 (e.g. they have never been set),
    // or they are MIN/MAX values (e.g. if we only have dividers on screen).
    if (
      (viewTimeRange.start === 0 && viewTimeRange.end === 0) ||
      (viewTimeRange.start === Number.MAX_SAFE_INTEGER &&
        viewTimeRange.end === Number.MIN_SAFE_INTEGER)
    ) {
      viewTimeRange = { ...maxTimeRange };
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
export function updateSelectedBandIds(
  bands: RavenCompositeBand[],
  selectedBandId: string,
  selectedSubBandId: string,
) {
  const band = bandById(bands, selectedBandId) as RavenCompositeBand;
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
 * Helper returns band label with composed label and unit if they exist.
 * Note the typing on `subBand` is cast to `RavenResourceBand` since the `RavenResourceBand` has the all the required properties.
 * This should not be a problem since we are checking `subBand.(...)` before accessing any properties.
 */
export function getBandLabel(band: RavenSubBand): string {
  const subBand = band as RavenResourceBand;
  let labelPin = '';
  let labelUnit = '';

  if (subBand.showLabelPin && subBand.labelPin !== '') {
    labelPin = ` (${subBand.labelPin})`;
  }

  if (subBand.showLabelUnit && subBand.labelUnit !== '') {
    labelUnit = ` (${subBand.labelUnit})`;
  }

  return subBand.type === 'resource'
    ? `${subBand.label}${labelPin}${labelUnit}`
    : `${subBand.label}${labelPin}`;
}

/**
 * Helper. Get customFilters from sourceIds in bands. e.g ../command?label=ips&filter=.*IPS.*
 */
export function getCustomFiltersBySourceId(
  bands: RavenCompositeBand[],
  treeBySourceId: StringTMap<RavenSource>,
) {
  const customFiltersBySourceId = {};

  bands.forEach((band: RavenCompositeBand) => {
    band.subBands.forEach((subBand: RavenSubBand) => {
      subBand.sourceIds.forEach((sourceId: string) => {
        const hasQueryString = sourceId.match(new RegExp('(.*)\\?(.*)'));

        if (hasQueryString) {
          const [, id, args] = hasQueryString;
          const source = treeBySourceId[id];

          if (source && source.type === 'customGraphable') {
            const hasQueryStringArgs = args.match(
              new RegExp('(.*)=(.*)&(.*)=(.*)'),
            );

            if (hasQueryStringArgs) {
              // Name/Value pairs here are parsed from the query string: ?name1=value1&name2=value2.
              const [, name1, value1, name2, value2] = hasQueryStringArgs;

              const customFilter = {
                [name1]: value1,
                [name2]: value2,
              };

              const customFilters = customFiltersBySourceId[id] || [];
              customFiltersBySourceId[id] = customFilters.concat(customFilter);
            }
          }
        }
      });
    });
  });

  return customFiltersBySourceId;
}

/**
 * Helper returns the filter for a customGraphableSource label.
 */
export function getFilterLabel(
  customGraphableSource: RavenCustomGraphableSource,
  customFilter: RavenCustomFilter,
) {
  if (customGraphableSource.arg === 'filter') {
    return `(${customGraphableSource.filterKey}=[${customFilter.filter}])`;
  } else {
    return `${customGraphableSource.arg}=${customFilter.filter}`;
  }
}

/**
 * Helper. Returns an activity-by-type band locator if a given band exists in the list of bands for a legend and sourceId if not ''.
 * `null` otherwise.
 */
export function activityBandsWithLegendAndSourceId(
  bands: RavenCompositeBand[],
  band: RavenSubBand,
  pinLabel: string,
  sourceId: string,
) {
  const bandsWithLegend = [];
  if (band.type === 'activity') {
    for (let i = 0, l = bands.length; i < l; ++i) {
      for (let j = 0, ll = bands[i].subBands.length; j < ll; ++j) {
        const subBand = bands[i].subBands[j] as RavenActivityBand;
        const withSourceId = sourceId.length
          ? subBand.sourceIds.includes(sourceId)
          : true;
        if (
          subBand.type === 'activity' &&
          withSourceId &&
          subBand.legend === (band as RavenActivityBand).legend &&
          subBand.labelPin === pinLabel
        ) {
          bandsWithLegend.push({
            bandId: bands[i].id,
            subBandId: subBand.id,
          });
        }
      }
    }
  }
  return bandsWithLegend;
}

/**
 * Helper. Returns an activity band for a filterTarget.
 * `null` otherwise.
 */
export function hasActivityBandForFilterTarget(
  bands: RavenCompositeBand[],
  filterTarget: string,
) {
  for (let i = 0, l = bands.length; i < l; ++i) {
    for (let j = 0, ll = bands[i].subBands.length; j < ll; ++j) {
      const subBand = bands[i].subBands[j] as RavenActivityBand;
      if (
        subBand.type === 'activity' &&
        subBand.filterTarget === filterTarget
      ) {
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
 * Helper. Returns true if band contains two resource bands.
 */
export function hasTwoResourceBands(band: RavenCompositeBand) {
  return (
    band.subBands.reduce((count, subBand) => {
      if (subBand.type === 'resource') {
        count++;
      }
      return count;
    }, 0) === 2
  );
}

/**
 * Helper. Returns an addTo band. Returns null if none of the subBands in addTo mode.
 */
export function getAddToSubBandId(
  bands: RavenCompositeBand[],
  bandId: string,
): string | null {
  const band = bandById(bands, bandId) as RavenCompositeBand;
  if (band && band.subBands) {
    const addToSubBands = band.subBands.filter(subBand => subBand.addTo);
    return addToSubBands.length > 0 ? addToSubBands[0].id : null;
  }
  return null;
}

/**
 * Helper. Returns all sub-bands that has source id.
 */
export function getBandsWithSourceId(
  bands: RavenCompositeBand[],
  sourceId: string,
) {
  const bandsWithSourceId = [];
  for (let i = 0, l = bands.length; i < l; ++i) {
    for (let j = 0, ll = bands[i].subBands.length; j < ll; ++j) {
      const subBand = bands[i].subBands[j];

      if (subBand.sourceIds.includes(sourceId)) {
        bandsWithSourceId.push({
          bandId: bands[i].id,
          subBandId: subBand.id,
        });
      }
    }
  }
  return bandsWithSourceId;
}

/**
 * Helper. Returns a band from a list of bands with the given id. Null otherwise.
 */
export function bandById(
  bands: RavenSubBand[] | RavenCompositeBand[],
  id: string,
): RavenSubBand | RavenCompositeBand | null {
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
export function subBandById(
  bands: RavenCompositeBand[],
  bandId: string,
  subBandId: string,
): RavenSubBand | null {
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
export function isAddTo(
  bands: RavenCompositeBand[],
  bandId: string,
  subBandId: string,
  type: string,
): boolean {
  const subBand = subBandById(bands, bandId, subBandId);

  if (subBand && subBand.type === type) {
    return subBand.addTo;
  }

  return false;
}

/**
 * Helper. Returns true if an activity is a `message` type. False otherwise.
 */
export function isMessageTypeActivity(activity: RavenActivityPoint): boolean {
  return activity.message && !activity.keywordLine ? true : false;
}

/**
 * Helper. Returns true if the given band id in a list of bands is in overlay mode. False otherwise.
 */
export function isOverlay(
  bands: RavenCompositeBand[],
  bandId: string,
): boolean {
  const band = bandById(bands, bandId) as RavenCompositeBand;

  if (band) {
    return band.overlay;
  }

  return false;
}

/**
 * Returns a new time range based on the view time range and some delta.
 */
export function changeZoom(delta: number, viewTimeRange: TimeRange): TimeRange {
  const { end, start } = viewTimeRange;
  const range = end - start;
  const zoomAmount = range / delta;

  return {
    end: end - zoomAmount,
    start: start + zoomAmount,
  };
}
