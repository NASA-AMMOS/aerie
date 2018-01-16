/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { keyBy, map, omit } from 'lodash';
import { v4 } from 'uuid';

import { createSelector, createFeatureSelector } from '@ngrx/store';

import {
  AddBands,
  AddPointsToBands,
  RemoveBands,
  TimelineActionTypes,
  TimelineAction,
  RemovePointsFromBands,
} from './../actions/timeline';

import {
  FetchInitialSourcesSuccess,
  SourceExplorerActionTypes,
  SourceExplorerAction,
  FetchSourcesSuccess,
} from './../actions/source-explorer';

import {
  RavenBand,
  RavenSource,
  StringTMap,
} from './../models/index';

// Source Explorer Interface.
export interface SourceExplorerState {
  initialSourcesLoaded: boolean;
  treeBySourceId: StringTMap<RavenSource>;
}

// Source Explorer Initial State.
const initialState: SourceExplorerState = {
  initialSourcesLoaded: false,
  treeBySourceId: {
    // Note: The root source in the source explorer tree is never displayed.
    '0': {
      actions: [],
      bandIds: {},
      childIds: [],
      content: [],
      dbType: '',
      draggable: false,
      expandable: false,
      expanded: false,
      icon: '',
      id: v4(),
      isServer: false,
      kind: '',
      label: 'root',
      menu: false,
      name: 'root',
      openable: false,
      opened: false,
      parentId: '',
      permissions: '',
      pinnable: false,
      pinned: false,
      selectable: false,
      selected: false,
      url: '',
    },
  },
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: SourceExplorerState = initialState, action: SourceExplorerAction | TimelineAction): SourceExplorerState {
  switch (action.type) {
    case TimelineActionTypes.AddBands:
      return addBands(state, action);
    case TimelineActionTypes.RemoveBands:
    case TimelineActionTypes.RemovePointsFromBands:
      return removeBands(state, action);
    case TimelineActionTypes.AddPointsToBands:
      return addPointsToBands(state, action);
    case SourceExplorerActionTypes.FetchInitialSourcesSuccess:
      return fetchInitialSourcesSuccess(state, action);
    case SourceExplorerActionTypes.FetchSourcesSuccess:
      return fetchSourcesSuccess(state, action);
    case SourceExplorerActionTypes.SourceExplorerCollapse:
      return updateTreeSource(state, action.source.id, 'expanded', false);
    case SourceExplorerActionTypes.SourceExplorerExpand:
      return updateTreeSource(state, action.source.id, 'expanded', true);
    case SourceExplorerActionTypes.SourceExplorerExpandWithLoadContent:
      return { ...state, treeBySourceId: newTreeSources(state.treeBySourceId, action.sources, action.source.id) };
    case SourceExplorerActionTypes.SourceExplorerClose:
      return updateTreeSource(state, action.source.id, 'opened', false);
    case SourceExplorerActionTypes.SourceExplorerOpen:
      return updateTreeSource(state, action.source.id, 'opened', true);
    case SourceExplorerActionTypes.SourceExplorerPin:
      return updateTreeSource(state, action.source.id, 'pinned', true);
    case SourceExplorerActionTypes.SourceExplorerUnpin:
      return updateTreeSource(state, action.source.id, 'pinned', false);
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'AddBands' action.
 *
 * This action is defined in the timelineViewer actions.
 * Called when we need to associate one source with one or more band.
 */
export function addBands(state: SourceExplorerState, action: AddBands): SourceExplorerState {
  return {
    ...state,
    treeBySourceId: {
      ...state.treeBySourceId,
      [action.sourceId]: {
        ...state.treeBySourceId[action.sourceId],
        bandIds: {
          ...state.treeBySourceId[action.sourceId].bandIds,
          ...action.bands.reduce((bandIds: string[], band: RavenBand) => {
            bandIds[band.id] = true;
            return bandIds;
          }, {}),
        },
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveBands' or 'RemovePointsFromBands' action.
 *
 * This action is defined in the timelineViewer actions.
 * Called when we need to de-associate one source with one or more band.
 */
export function removeBands(state: SourceExplorerState, action: RemoveBands | RemovePointsFromBands): SourceExplorerState {
  return {
    ...state,
    treeBySourceId: {
      ...state.treeBySourceId,
      [action.sourceId]: {
        ...state.treeBySourceId[action.sourceId],
        bandIds: omit(state.treeBySourceId[action.sourceId].bandIds, action.bandIds),
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'AddPointsToBands' action.
 *
 * This action is defined in the timelineViewer actions.
 * Called when we need to associate one sources with one or more new band ids.
 * This is very similar to addBands() expect for the action.bandIdsToPoints Object.keys() reduction.
 */
export function addPointsToBands(state: SourceExplorerState, action: AddPointsToBands): SourceExplorerState {
  return {
    ...state,
    treeBySourceId: {
      ...state.treeBySourceId,
      [action.sourceId]: {
        ...state.treeBySourceId[action.sourceId],
        bandIds: {
          ...state.treeBySourceId[action.sourceId].bandIds,
          ...Object.keys(action.bandIdsToPoints).reduce((bandIds, bandId) => {
            bandIds[bandId] = true;
            return bandIds;
          }, {}),
        },
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'FetchInitialSourcesSuccess' action.
 *
 * Generates a new treeBySourceId data structure with updated childIds for the root source,
 * and the new child sources keyed off of their id.
 * Sets initialSourcesLoaded to true.
 */
export function fetchInitialSourcesSuccess(state: SourceExplorerState, action: FetchInitialSourcesSuccess): SourceExplorerState {
  return {
    ...state,
    initialSourcesLoaded: true,
    treeBySourceId: newTreeSources(state.treeBySourceId, action.sources, '0'),
  };
}

/**
 * Reduction Helper. Called when reducing the 'FetchSourcesSuccess' or 'LoadSourceWithContent' action.
 *
 * Generates a new treeBySourceId data structure with updated childIds for the action.source,
 * and the new child sources keyed off of their id.
 * Sets fetchSourcesRequestPending to false.
 */
export function fetchSourcesSuccess(state: SourceExplorerState, action: FetchSourcesSuccess) {
  return {
    ...state,
    fetchSourcesRequestPending: false,
    treeBySourceId: newTreeSources(state.treeBySourceId, action.sources, action.source.id),
  };
}

/**
 * Helper. Updates the treeSourceById object with new child sources for a given source.
 */
export function newTreeSources(treeBySourceId: StringTMap<RavenSource>, sources: RavenSource[], id: string): StringTMap<RavenSource> {
  return {
    ...treeBySourceId,
    ...keyBy(sources, 'id'),
    [id]: {
      ...treeBySourceId[id],
      childIds: map(sources, 'id'),
    },
  };
}

/**
 * Helper. Updates a source prop with a value.
 */
export function updateTreeSource(state: SourceExplorerState, id: string, prop: string, value: string | number | boolean): SourceExplorerState {
  return {
    ...state,
    treeBySourceId: {
      ...state.treeBySourceId,
      [id]: {
        ...state.treeBySourceId[id],
        [prop]: value,
      },
    },
  };
}

/**
 * Source Explorer state selector helper.
 */
export const getSourceExplorerState = createFeatureSelector<SourceExplorerState>('sourceExplorer');

/**
 * Create selector helper for selecting state slice.
 *
 * Every reducer module exports selector functions, however child reducers
 * have no knowledge of the overall state tree. To make them usable, we
 * need to make new selectors that wrap them.
 *
 * The createSelector function creates very efficient selectors that are memoized and
 * only recompute when arguments change. The created selectors can also be composed
 * together to select different pieces of state.
 */
export const getTreeBySourceId = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.treeBySourceId);
