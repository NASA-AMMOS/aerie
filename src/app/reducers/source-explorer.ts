/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  keyBy,
  map,
  omit,
} from 'lodash';

import {
  createFeatureSelector,
  createSelector,
} from '@ngrx/store';

import {
  NewSources,
  SourceExplorerAction,
  SourceExplorerActionTypes,
  SourceExplorerSelect,
  SubBandIdAdd,
  SubBandIdRemove,
  UpdateSourceExplorer,
} from './../actions/source-explorer';

import {
  RavenSource,
  StringTMap,
} from './../shared/models/index';

// Source Explorer State Interface.
export interface SourceExplorerState {
  fetchPending: boolean;
  initialSourcesLoaded: boolean;
  selectedSourceId: string;
  treeBySourceId: StringTMap<RavenSource>;
}

// Source Explorer Initial State.
export const initialState: SourceExplorerState = {
  fetchPending: false,
  initialSourcesLoaded: false,
  selectedSourceId: '',
  treeBySourceId: {
    // Note: The root source in the source explorer tree is never displayed.
    '/': {
      actions: [],
      childIds: [],
      content: [],
      dbType: '',
      draggable: false,
      expandable: false,
      expanded: false,
      icon: '',
      id: '/',
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
      subBandIds: {},
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
    case SourceExplorerActionTypes.FetchInitialSources:
      return { ...state, fetchPending: true };
    case SourceExplorerActionTypes.ImportSource:
      return state;
    case SourceExplorerActionTypes.ImportSourceFailure:
      console.log('in reducer import source failed');
      return state;
    case SourceExplorerActionTypes.ImportSourceSuccess:
      return state;
    case SourceExplorerActionTypes.NewSources:
      return newSources(state, action);
    case SourceExplorerActionTypes.SourceExplorerExpandEvent:
    case SourceExplorerActionTypes.SourceExplorerOpenEvent:
      return { ...state, fetchPending: true };
    case SourceExplorerActionTypes.SourceExplorerSelect:
      return selectSource(state, action);
    case SourceExplorerActionTypes.SubBandIdAdd:
      return subBandIdAdd(state, action);
    case SourceExplorerActionTypes.SubBandIdRemove:
      return subBandIdRemove(state, action);
    case SourceExplorerActionTypes.UpdateSourceExplorer:
      return updateSourceExplorer(state, action);
    case SourceExplorerActionTypes.UpdateTreeSource:
      return updateTreeSource(state, action.sourceId, action.prop, action.value);
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'NewSources' action.
 *
 * Generates a new treeBySourceId data structure with updated childIds for the action.sourceId,
 * and the new child sources keyed off of their id.
 */
export function newSources(state: SourceExplorerState, action: NewSources): SourceExplorerState {
  return {
    ...state,
    treeBySourceId: {
      ...state.treeBySourceId,
      ...keyBy(action.sources, 'id'),
      [action.sourceId]: {
        ...state.treeBySourceId[action.sourceId],
        childIds: map(action.sources, 'id'),
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'SourceExplorerSelect' action.
 * Note that in some cases state.selectedSourceId === '' so we just omit '' keys from treeBySourceId.
 */
export function selectSource(state: SourceExplorerState, action: SourceExplorerSelect): SourceExplorerState {
  if (state.treeBySourceId[action.source.id].selectable) {
    return {
      ...state,
      selectedSourceId: action.source.id === state.selectedSourceId ? '' : action.source.id,
      treeBySourceId: omit({
        ...state.treeBySourceId,
        [action.source.id]: {
          ...state.treeBySourceId[action.source.id],
          selected: true,
        },
        [state.selectedSourceId]: {
          ...state.treeBySourceId[state.selectedSourceId],
          selected: false,
        },
      }, ''),
    };
  }

  return state;
}

/**
 * Reduction Helper. Called when reducing the 'SubBandIdAdd' action.
 */
export function subBandIdAdd(state: SourceExplorerState, action: SubBandIdAdd): SourceExplorerState {
  return {
    ...state,
    treeBySourceId: {
      ...state.treeBySourceId,
      [action.sourceId]: {
        ...state.treeBySourceId[action.sourceId],
        subBandIds: {
          ...state.treeBySourceId[action.sourceId].subBandIds,
          [action.subBandId]: action.subBandId,
        },
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'SubBandIdRemove' action.
 */
export function subBandIdRemove(state: SourceExplorerState, action: SubBandIdRemove): SourceExplorerState {
  return {
    ...state,
    treeBySourceId: {
      ...state.treeBySourceId,
      ...Object.keys(action.sourceIds).reduce((sourceIds, sourceId) => {
        const subBandIds = omit(state.treeBySourceId[sourceId].subBandIds, action.subBandId);
        const opened = Object.keys(subBandIds).length > 0 ? true : false;

        sourceIds[sourceId] = {
          ...state.treeBySourceId[sourceId],
          opened,
          subBandIds,
        };

        return sourceIds;
      }, {}),
    },
  };
}

/**
 * Helper. Updates a source prop with a value.
 */
export function updateTreeSource(state: SourceExplorerState, id: string, prop: string, value: string | number | boolean | string[]): SourceExplorerState {
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
 * Reduction Helper. Called when reducing the 'UpdateSourceExplorer' action.
 * This is just a top level reducer for the sourceExplorer state (top level meaning it updates base sourceExplorer state props).
 */
export function updateSourceExplorer(state: SourceExplorerState, action: UpdateSourceExplorer): SourceExplorerState {
  return {
    ...state,
    ...action.update,
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
export const getInitialSourcesLoaded = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.initialSourcesLoaded);
export const getPending = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.fetchPending);
export const getTreeBySourceId = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.treeBySourceId);
