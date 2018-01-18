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
  FetchGraphDataSuccess,
  FetchInitialSourcesSuccess,
  FetchSourcesSuccess,
  LoadContent,
  RemoveBands,
  SourceExplorerActionTypes,
  SourceExplorerAction,
} from './../actions/source-explorer';

import {
  RavenBand,
  RavenSource,
  StringTMap,
} from './../models/index';

// Source Explorer Interface.
export interface SourceExplorerState {
  fetchGraphDataRequestPending: boolean;
  fetchInitialSourcesRequestPending: boolean;
  fetchSourcesRequestPending: boolean;
  initialSourcesLoaded: boolean;
  treeBySourceId: StringTMap<RavenSource>;
}

// Source Explorer Initial State.
const initialState: SourceExplorerState = {
  fetchGraphDataRequestPending: false,
  fetchInitialSourcesRequestPending: false,
  fetchSourcesRequestPending: false,
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
export function reducer(state: SourceExplorerState = initialState, action: SourceExplorerAction): SourceExplorerState {
  switch (action.type) {
    case SourceExplorerActionTypes.FetchGraphData:
      return { ...state, fetchGraphDataRequestPending: true };
    case SourceExplorerActionTypes.FetchGraphDataFailure:
      return { ...state, fetchGraphDataRequestPending: false };
    case SourceExplorerActionTypes.FetchGraphDataSuccess:
      return addBands(state, action);
    case SourceExplorerActionTypes.FetchInitialSources:
      return { ...state, fetchInitialSourcesRequestPending: true };
    case SourceExplorerActionTypes.FetchInitialSourcesFailure:
      return { ...state, fetchInitialSourcesRequestPending: false };
    case SourceExplorerActionTypes.FetchInitialSourcesSuccess:
      return fetchInitialSourcesSuccess(state, action);
    case SourceExplorerActionTypes.FetchSources:
      return { ...state, fetchSourcesRequestPending: true };
    case SourceExplorerActionTypes.FetchSourcesFailure:
      return { ...state, fetchSourcesRequestPending: false };
    case SourceExplorerActionTypes.FetchSourcesSuccess:
    case SourceExplorerActionTypes.LoadContent:
      return newSources(state, action);
    case SourceExplorerActionTypes.RemoveBands:
      return removeBands(state, action);
    case SourceExplorerActionTypes.SourceExplorerCollapse:
      return updateTreeSource(state, action.source.id, 'expanded', false);
    case SourceExplorerActionTypes.SourceExplorerExpand:
      return updateTreeSource(state, action.source.id, 'expanded', true);
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
 * Reduction Helper. Called when reducing the 'FetchGraphDataSuccess' action.
 *
 * Called when we need to associate one source with one or more band ids.
 */
export function addBands(state: SourceExplorerState, action: FetchGraphDataSuccess): SourceExplorerState {
  return {
    ...state,
    fetchGraphDataRequestPending: false,
    treeBySourceId: {
      ...state.treeBySourceId,
      [action.source.id]: {
        ...state.treeBySourceId[action.source.id],
        bandIds: {
          ...state.treeBySourceId[action.source.id].bandIds,
          ...action.bands.reduce((bandIds: StringTMap<boolean>, band: RavenBand) => {
            bandIds[band.id] = true;
            return bandIds;
          }, {}),
          ...Object.keys(action.bandIdsToPoints).reduce((bandIds: StringTMap<boolean>, bandId: string) => {
            bandIds[bandId] = true;
            return bandIds;
          }, {}),
        },
        opened: true,
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveBands' action.
 *
 * Called when we need to de-associate one source with one or more band.
 */
export function removeBands(state: SourceExplorerState, action: RemoveBands): SourceExplorerState {
  return {
    ...state,
    treeBySourceId: {
      ...state.treeBySourceId,
      [action.source.id]: {
        ...state.treeBySourceId[action.source.id],
        bandIds: omit(state.treeBySourceId[action.source.id].bandIds, action.removeBandIds),
        opened: false,
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'FetchInitialSourcesSuccess' action.
 *
 * Generates a new treeBySourceId data structure with updated childIds for the root source,
 * and the new child sources keyed off of their id.
 */
export function fetchInitialSourcesSuccess(state: SourceExplorerState, action: FetchInitialSourcesSuccess): SourceExplorerState {
  return {
    ...state,
    fetchInitialSourcesRequestPending: false,
    initialSourcesLoaded: true,
    treeBySourceId: {
      ...state.treeBySourceId,
      ...keyBy(action.sources, 'id'),
      '0': {
        ...state.treeBySourceId['0'],
        childIds: map(action.sources, 'id'),
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'FetchSourcesSuccess' or 'LoadContent' action.
 *
 * Generates a new treeBySourceId data structure with updated childIds for the action.source,
 * and the new child sources keyed off of their id.
 */
export function newSources(state: SourceExplorerState, action: FetchSourcesSuccess | LoadContent): SourceExplorerState {
  return {
    ...state,
    fetchSourcesRequestPending: false,
    treeBySourceId: {
      ...state.treeBySourceId,
      ...keyBy(action.sources, 'id'),
      [action.source.id]: {
        ...state.treeBySourceId[action.source.id],
        childIds: map(action.sources, 'id'),
        expanded: true,
      },
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
export const getFetchGraphDataRequestPending = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.fetchGraphDataRequestPending);
export const getFetchInitialSourcesRequestPending = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.fetchInitialSourcesRequestPending);
export const getFetchSourcesRequestPending = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.fetchSourcesRequestPending);
export const getTreeBySourceId = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.treeBySourceId);
