/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import uniqueId from 'lodash-es/uniqueId';
import {
  RavenCompositeBand,
  RavenDefaultBandSettings,
  RavenState,
  RavenSubBand,
} from '../models';
import {
  RavenExportActivitySubBand,
  RavenExportBand,
  RavenExportCompositeBand,
  RavenExportDefaultBandSettings,
  RavenExportResourceSubBand,
  RavenExportState,
  RavenExportStateSubBand,
  RavenExportSubBand,
} from '../models/export-state';
import { RavenAppState } from '../raven-store';
import { toCompositeBand } from './bands';
import { colorRgbArrayToHex } from './color';
import { getSourceIdsForSubBand } from './source';
import { timestamp } from './time';

/**
 * Returns an exported state of the stripped down state for saving.
 */
export function getState(name: string, state: RavenAppState): any {
  return exportState(getRavenState(name, state));
}

/**
 * Returns a stripped down version of a state that we save.
 */
export function getRavenState(name: string, state: RavenAppState): RavenState {
  const bands = state.raven.timeline.bands.map(band => ({
    ...band,
    subBands: band.subBands.map((subBand: RavenSubBand) => ({
      ...subBand,
      maxTimeRange: { end: 0, start: 0 },
      points: [],
      sourceIds: getSourceIdsForSubBand(
        subBand.sourceIds,
        state.raven.sourceExplorer.treeBySourceId,
        subBand.label,
        state.raven.sourceExplorer.customFiltersBySourceId,
        state.raven.sourceExplorer.filtersByTarget,
      ),
    })),
  }));

  return {
    bands: bands,
    defaultBandSettings: state.config.raven.defaultBandSettings,
    expansionByActivityId: state.raven.timeline.expansionByActivityId,
    guides: state.raven.timeline.guides,
    ignoreShareableLinkTimes: state.config.raven.ignoreShareableLinkTimes,
    inUseEpoch: state.raven.epochs.inUseEpoch,
    maxTimeRange: state.raven.timeline.maxTimeRange,
    name: name,
    pins: state.raven.sourceExplorer.pins,
    showDetailsPanel: state.raven.layout.showDetailsPanel,
    showLeftPanel: state.raven.layout.showLeftPanel,
    showRightPanel: state.raven.layout.showRightPanel,
    showSouthBandsPanel: state.raven.layout.showSouthBandsPanel,
    version: '1.0.0',
    viewTimeRange: {
      end: timestamp(state.raven.timeline.viewTimeRange.end, true),
      start: timestamp(state.raven.timeline.viewTimeRange.start, true),
    },
  };
}

// Remove all ids from bands and sub-bands.
// Flatten composite bands with a single sub-band.
export function exportBand(band: RavenCompositeBand): RavenExportBand {
  const { id: bandId, ...bandWithNoId } = band;

  const subBands = band.subBands.map((subBand: RavenSubBand) => {
    const { id: subBandId, parentUniqueId, ...subBandWithNoIds } = subBand;
    return { ...subBandWithNoIds, points: [] };
  });

  if (subBands.length !== 1) {
    return { ...bandWithNoId, subBands };
  } else {
    return {
      ...subBands[0],
      backgroundColor: bandWithNoId.backgroundColor,
      containerId: bandWithNoId.containerId,
      sortOrder: bandWithNoId.sortOrder,
    };
  }
}

/**
 * Takes a RavenState and does some transformations before saving it to the database:
 * 1. Remove ids.
 * 2. Flattens composite bands with one sub-band.
 */
export function exportState(state: RavenState): RavenExportState {
  const bands = state.bands.map(band => exportBand(band));

  return { ...state, bands };
}

function isExportCompositeBand(
  band: RavenExportBand,
): band is RavenExportCompositeBand {
  return band.type === 'composite';
}

function isExportActivitySubBand(
  band: RavenExportSubBand,
): band is RavenExportActivitySubBand {
  return band.type === 'activity';
}

function isExportResourceSubBand(
  band: RavenExportSubBand,
): band is RavenExportResourceSubBand {
  return band.type === 'resource';
}

function isExportStateSubBand(
  band: RavenExportSubBand,
): band is RavenExportStateSubBand {
  return band.type === 'state';
}

export function importSubBand(
  subBand: RavenExportSubBand,
  parentUniqueId: string,
): RavenSubBand {
  let labelColor;
  if (typeof subBand.labelColor !== 'string') {
    labelColor = colorRgbArrayToHex(subBand.labelColor);
  } else {
    labelColor = subBand.labelColor;
  }

  if (isExportActivitySubBand(subBand)) {
    return {
      ...subBand,
      activityFilter:
        subBand.activityFilter === undefined ? '' : subBand.activityFilter,
      editable: subBand.editable === undefined ? false : subBand.editable,
      id: uniqueId(),
      labelColor,
      parentUniqueId,
      pointsChanged: false,
      timeDelta: subBand.timeDelta === undefined ? 0 : subBand.timeDelta,
    };
  } else if (isExportResourceSubBand(subBand)) {
    return {
      ...subBand,
      editable: subBand.editable === undefined ? false : subBand.editable,
      id: uniqueId(),
      labelColor,
      parentUniqueId,
      pointsChanged: false,
      timeDelta: subBand.timeDelta === undefined ? 0 : subBand.timeDelta,
    };
  } else if (isExportStateSubBand(subBand)) {
    return {
      ...subBand,
      editable: subBand.editable === undefined ? false : subBand.editable,
      id: uniqueId(),
      labelColor,
      parentUniqueId,
      pointsChanged: false,
      timeDelta: subBand.timeDelta === undefined ? 0 : subBand.timeDelta,
    };
  } else {
    return {
      ...subBand,
      editable: false,
      id: uniqueId(),
      labelColor,
      parentUniqueId,
    };
  }
}

export function importBand(band: RavenExportBand): RavenCompositeBand {
  if (isExportCompositeBand(band)) {
    const parentUniqueId = uniqueId();

    return {
      ...band,
      id: parentUniqueId,
      subBands: band.subBands.map(subBand =>
        importSubBand(subBand, parentUniqueId),
      ),
    };
  } else {
    const {
      backgroundColor,
      containerId,
      sortOrder,
      ...subBandWithoutIds
    } = band;

    const subBand = importSubBand(subBandWithoutIds, '0');

    const compositeBand = toCompositeBand(
      subBand,
      containerId,
      sortOrder,
      backgroundColor,
    );
    compositeBand.subBands[0].parentUniqueId = compositeBand.id;

    return compositeBand;
  }
}

export function importDefaultBandSettings(
  settings: RavenExportDefaultBandSettings,
): RavenDefaultBandSettings {
  return {
    ...settings,
    activityInitiallyHidden:
      settings.activityInitiallyHidden === undefined
        ? false
        : settings.activityInitiallyHidden,
  };
}

/**
 * Takes a saved state and transforms it into a RavenState:
 * 1. Creates composite bands from flattened sub-bands.
 * 2. Adds ids.
 */
export function importState(state: RavenExportState): RavenState {
  return {
    ...state,
    bands: state.bands.map(band => importBand(band)),
    defaultBandSettings: importDefaultBandSettings(state.defaultBandSettings),
    inUseEpoch: state.inUseEpoch === undefined ? null : state.inUseEpoch,
    showDetailsPanel:
      state.showDetailsPanel === undefined ? true : state.showDetailsPanel,
    showLeftPanel:
      state.showLeftPanel === undefined ? true : state.showLeftPanel,
    showRightPanel:
      state.showRightPanel === undefined ? true : state.showRightPanel,
    showSouthBandsPanel:
      state.showSouthBandsPanel === undefined
        ? true
        : state.showSouthBandsPanel,
  };
}
