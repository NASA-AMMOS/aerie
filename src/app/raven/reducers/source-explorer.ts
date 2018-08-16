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
  omit,
  without,
} from 'lodash';

import {
  createFeatureSelector,
  createSelector,
} from '@ngrx/store';

import {
  State,
} from './../raven-store';

import {
  AddCustomFilter,
  AddFilter,
  CloseEvent,
  CollapseEvent,
  ExpandEvent,
  NewSources,
  OpenEvent,
  PinAdd,
  PinRemove,
  PinRename,
  RemoveFilter,
  RemoveSource,
  SelectSource,
  SetCustomFilter,
  SetCustomFilterSubBandId,
  SourceExplorerAction,
  SourceExplorerActionTypes,
  SubBandIdAdd,
  SubBandIdRemove,
} from '../actions/source-explorer';

import {
  BaseType,
  RavenCustomFilter,
  RavenGraphableFilterSource,
  RavenPin,
  RavenSource,
  RavenSourceAction,
  StringTMap,
} from './../../shared/models';

import {
  getAllChildIds,
} from './../../shared/util';

// Source Explorer State Interface.
export interface SourceExplorerState {
  currentStateId: string;
  customFiltersBySourceId: StringTMap<RavenCustomFilter[]>;
  fetchPending: boolean;
  filtersByTarget: StringTMap<StringTMap<string[]>>; // Target refers to an id that ties filters to a graphable source.
  initialSourcesLoaded: boolean;
  loadErrors: string[];
  pins: RavenPin[];
  selectedSourceId: string;
  treeBySourceId: StringTMap<RavenSource>;
}

// Source Explorer Initial State.
export const initialState: SourceExplorerState = {
  currentStateId: '',
  customFiltersBySourceId: {},
  fetchPending: false,
  filtersByTarget: {},
  initialSourcesLoaded: false,
  loadErrors: [], // List of sourceIds that give errors when trying to load. Cleared after displayed.
  pins: [],
  selectedSourceId: '',
  treeBySourceId: {
    // Note: The root source in the source explorer tree is never displayed.
    '/': {
      actions: [],
      childIds: [],
      content: null,
      dbType: '',
      draggable: false,
      expandable: false,
      expanded: false,
      fileMetadata: {
        createdBy: '',
        createdOn: '',
        customMetadata: null,
        fileType: '',
        lastModified: '',
        permissions: '',
      },
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
      subBandIds: [],
      subKind: '',
      type: '',
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
    case SourceExplorerActionTypes.AddCustomFilter:
      return addCustomFilter(state, action);
    case SourceExplorerActionTypes.AddFilter:
      return addFilter(state, action);
    case SourceExplorerActionTypes.AddGraphableFilter:
    case SourceExplorerActionTypes.ApplyLayout:
    case SourceExplorerActionTypes.ApplyState:
      return { ...state, fetchPending: true };
    case SourceExplorerActionTypes.CloseEvent:
      return closeEvent(state, action);
    case SourceExplorerActionTypes.CollapseEvent:
      return collapseEvent(state, action);
    case SourceExplorerActionTypes.ExpandEvent:
      return expandEvent(state, action);
    case SourceExplorerActionTypes.FetchInitialSources:
    case SourceExplorerActionTypes.GraphCustomSource:
      return { ...state, fetchPending: true };
    case SourceExplorerActionTypes.LoadErrorsAdd:
      return { ...state, loadErrors: state.loadErrors.concat(action.sourceIds) };
    case SourceExplorerActionTypes.NewSources:
      return newSources(state, action);
    case SourceExplorerActionTypes.OpenEvent:
      return openEvent(state, action);
    case SourceExplorerActionTypes.PinAdd:
      return pinAdd(state, action);
    case SourceExplorerActionTypes.PinRemove:
      return pinRemove(state, action);
    case SourceExplorerActionTypes.PinRename:
      return pinRename(state, action);
    case SourceExplorerActionTypes.RemoveFilter:
      return removeFilter(state, action);
    case SourceExplorerActionTypes.RemoveGraphableFilter:
      return { ...state, fetchPending: true };
    case SourceExplorerActionTypes.RemoveSource:
      return removeSource(state, action);
    case SourceExplorerActionTypes.RemoveSourceEvent:
    case SourceExplorerActionTypes.SaveState:
      return { ...state, fetchPending: true };
    case SourceExplorerActionTypes.SelectSource:
      return selectSource(state, action);
    case SourceExplorerActionTypes.SetCustomFilter:
      return setCustomFilter(state, action);
    case SourceExplorerActionTypes.SetCustomFilterSubBandId:
      return setCustomFilterSubBandId(state, action);
    case SourceExplorerActionTypes.SubBandIdAdd:
      return subBandIdAdd(state, action);
    case SourceExplorerActionTypes.SubBandIdRemove:
      return subBandIdRemove(state, action);
    case SourceExplorerActionTypes.UpdateSourceExplorer:
      return { ...state, ...action.update };
    case SourceExplorerActionTypes.UpdateTreeSource:
      return { ...state, ...updateTreeSource(state, action.sourceId, action.update) };
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'AddCustomFilter' action.
 */
export function addCustomFilter(state: SourceExplorerState, action: AddCustomFilter): SourceExplorerState {
  const customFilters = state.customFiltersBySourceId[action.sourceId] || [];

  return {
    ...state,
    customFiltersBySourceId: {
      ...state.customFiltersBySourceId,
      [action.sourceId]: customFilters.concat({
        filter: action.customFilter,
        label: action.label,
        subBandId: '',
      }),
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'AddFilter' action.
 */
export function addFilter(state: SourceExplorerState, action: AddFilter): SourceExplorerState {
  const targetFilters = state.filtersByTarget[action.source.filterTarget] || {};
  const groupFilters = targetFilters[action.source.filterSetOf] || [];

  return {
    ...state,
    filtersByTarget: {
      ...state.filtersByTarget,
      [action.source.filterTarget]: {
        ...targetFilters,
        [action.source.filterSetOf]: groupFilters
          .filter(filter => filter !== action.source.id)
          .concat(action.source.id),
      },
    },
    ...updateTreeSource(state, action.source.id, { opened: true }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'CloseEvent' action.
 */
export function closeEvent(state: SourceExplorerState, action: CloseEvent): SourceExplorerState {
  return {
    ...state,
    ...updateTreeSource(state, action.sourceId, {
      opened: false,
      subBandIds: [],
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'CollapseEvent' action.
 */
export function collapseEvent(state: SourceExplorerState, action: CollapseEvent): SourceExplorerState {
  return {
    ...state,
    ...updateTreeSource(state, action.sourceId, {
      expanded: false,
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'ExpandEvent' action.
 */
export function expandEvent(state: SourceExplorerState, action: ExpandEvent): SourceExplorerState {
  return {
    ...state,
    fetchPending: true,
    ...updateTreeSource(state, action.sourceId, {
      expanded: true,
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'NewSources' action.
 *
 * Generates a new treeBySourceId data structure with updated childIds for the action.sourceId,
 * and the new child sources keyed off of their id.
 */
export function newSources(state: SourceExplorerState, action: NewSources): SourceExplorerState {
  const parentSource = state.treeBySourceId[action.sourceId];
  const sources = action.sources.filter(source => !state.treeBySourceId[source.id]); // Exclude sources that already exist.

  return {
    ...state,
    treeBySourceId: {
      ...state.treeBySourceId,
      ...keyBy(sources, 'id'),
      [action.sourceId]: {
        ...parentSource,
        childIds: parentSource.childIds.concat(sources.map(source => source.id)),
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'OpenEvent' action.
 */
export function openEvent(state: SourceExplorerState, action: OpenEvent): SourceExplorerState {
  return {
    ...state,
    fetchPending: true,
    ...updateTreeSource(state, action.sourceId, {
      opened: true,
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'PinAdd' action.
 */
export function pinAdd(state: SourceExplorerState, action: PinAdd): SourceExplorerState {
  return {
    ...state,
    pins: state.pins.concat(action.pin),
    treeBySourceId: {
      ...state.treeBySourceId,
      [action.pin.sourceId]: {
        ...state.treeBySourceId[action.pin.sourceId],
        actions: state.treeBySourceId[action.pin.sourceId].actions.reduce((actions: RavenSourceAction[], currentAction: RavenSourceAction) => {
          if (currentAction.event === 'pin-add') {
            actions.push(
              { event: 'pin-remove', name: 'Remove Pin' },
              { event: 'pin-rename', name: 'Rename Pin' },
            );
          } else {
            actions.push(currentAction);
          }
          return actions;
        }, []),
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'PinRemove' action.
 */
export function pinRemove(state: SourceExplorerState, action: PinRemove): SourceExplorerState {
  return {
    ...state,
    pins: state.pins.filter(pin => pin.sourceId !== action.sourceId),
    treeBySourceId: {
      ...state.treeBySourceId,
      [action.sourceId]: {
        ...state.treeBySourceId[action.sourceId],
        actions: state.treeBySourceId[action.sourceId].actions
          .filter(currentAction => currentAction.event !== 'pin-remove' && currentAction.event !== 'pin-rename')
          .concat({ event: 'pin-add', name: 'Add Pin' }),
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'PinRename' action.
 */
export function pinRename(state: SourceExplorerState, action: PinRename): SourceExplorerState {
  return {
    ...state,
    pins: state.pins.map(pin => {
      if (pin.sourceId === action.sourceId) {
        return {
          ...pin,
          name: action.newName,
        };
      }
      return pin;
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveFilter' action.
 * Removes filter from filter array and unselects the source.
 */
export function removeFilter(state: SourceExplorerState, action: RemoveFilter): SourceExplorerState {
  const targetFilters = state.filtersByTarget[action.source.filterTarget] || {};
  const groupFilters = targetFilters[action.source.filterSetOf] || [];

  return {
    ...state,
    filtersByTarget: {
      ...state.filtersByTarget,
      [action.source.filterTarget]: {
        ...targetFilters,
        [action.source.filterSetOf]: groupFilters.filter(filter => filter !== action.source.id),
      },
    },
    ...updateTreeSource(state, action.source.id, { opened: false }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveSource' action.
 * Removes a source and all it's children from the source tree.
 * Make sure we reset the selected source id if we remove the selected source.
 */
export function removeSource(state: SourceExplorerState, action: RemoveSource): SourceExplorerState {
  const source = state.treeBySourceId[action.sourceId];
  const parentSource = state.treeBySourceId[source.parentId];
  const allChildIds = getAllChildIds(state.treeBySourceId, source.id);

  if (parentSource) {
    return {
      ...state,
      selectedSourceId: action.sourceId === state.selectedSourceId ? '' : state.selectedSourceId,
      treeBySourceId: {
        ...omit(state.treeBySourceId, action.sourceId, allChildIds),
        [parentSource.id]: {
          ...parentSource,
          childIds: parentSource.childIds.filter(childId => childId !== action.sourceId),
        },
      },
    };
  } else {
    console.error('source-explorer.ts - removeSource: you cannot remove a source without a parent: ', action.sourceId);

    return {
      ...state,
    };
  }
}

/**
 * Reduction Helper. Called when reducing the 'SelectSource' action.
 * Note that in some cases state.selectedSourceId === '' so we just omit '' keys from treeBySourceId.
 */
export function selectSource(state: SourceExplorerState, action: SelectSource): SourceExplorerState {
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
 * Reduction Helper. Called when reducing the 'SetCustomFilter' action.
 */
export function setCustomFilter(state: SourceExplorerState, action: SetCustomFilter): SourceExplorerState {
  return {
    ...state,
    filtersByTarget: {
      ...state.filtersByTarget,
      [action.source.filterTarget]: {
        ...state.filtersByTarget[action.source.filterTarget],
        [action.source.filterSetOf]: [action.source.id],
      },
    },
    ...updateTreeSource(state, action.source.id, { opened: action.filter ? true : false, filter: action.filter }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'SetCustomFilterSubBandId' action.
 */
export function setCustomFilterSubBandId(state: SourceExplorerState, action: SetCustomFilterSubBandId): SourceExplorerState {
  const customFilters = state.customFiltersBySourceId[action.sourceId] || [];

  return {
    ...state,
    customFiltersBySourceId: {
      ...state.customFiltersBySourceId,
      [action.sourceId]: customFilters.map(customFilter => ({
        ...customFilter,
        subBandId: customFilter.label === action.label ? action.subBandId : customFilter.subBandId,
      })),
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'SubBandIdAdd' action.
 */
export function subBandIdAdd(state: SourceExplorerState, action: SubBandIdAdd): SourceExplorerState {
  const subBandIds = without(state.treeBySourceId[action.sourceId].subBandIds, action.subBandId).concat(action.subBandId);

  return {
    ...state,
    treeBySourceId: {
      ...state.treeBySourceId,
      [action.sourceId]: {
        ...state.treeBySourceId[action.sourceId],
        subBandIds,
      },
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'SubBandIdRemove' action.
 */
export function subBandIdRemove(state: SourceExplorerState, action: SubBandIdRemove): SourceExplorerState {
  let newCustomFiltersBySourceId = {
    ...state.customFiltersBySourceId,
  };

  let newFiltersByTarget = {
    ...state.filtersByTarget,
  };

  action.sourceIds.forEach(sourceId => {
    const source = state.treeBySourceId[sourceId];

    if (source && source.type === 'customGraphable') {
      // Custom Graphable.
      const customFilters = state.customFiltersBySourceId[sourceId] || [];

      newCustomFiltersBySourceId = {
        ...state.customFiltersBySourceId,
        [sourceId]: customFilters.filter(
          customFilter => customFilter.subBandId !== action.subBandId,
        ),
      };
    } else if (source && source.type === 'graphableFilter') {
      // Graphable Filter.
      const graphableFilterSource = source as RavenGraphableFilterSource;
      const filterTarget = graphableFilterSource.filterTarget;
      const filters = newFiltersByTarget[filterTarget] || [];

      newFiltersByTarget = {
        ...newFiltersByTarget,
        [filterTarget]: {
          ...filters,
          [graphableFilterSource.filterSetOf]: filters[graphableFilterSource.filterSetOf].filter(
            filterSourceId => filterSourceId !== graphableFilterSource.name,
          ),
        },
      };
    }
  });

  return {
    ...state,
    customFiltersBySourceId: newCustomFiltersBySourceId,
    filtersByTarget: newFiltersByTarget,
    treeBySourceId: {
      ...state.treeBySourceId,
      ...action.sourceIds.reduce((sourceIds, sourceId) => {
        const subBandIds = state.treeBySourceId[sourceId].subBandIds.filter(subBandId => subBandId !== action.subBandId);
        const opened = subBandIds.length > 0 ? true : false;

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
 * Helper. Return an updated tree source object. Can be used to spread over the state to update it.
 */
export function updateTreeSource(state: SourceExplorerState, sourceId: string, update: StringTMap<BaseType>) {
  return {
    treeBySourceId: {
      ...state.treeBySourceId,
      [sourceId]: {
        ...state.treeBySourceId[sourceId],
        ...update,
      },
    },
  };
}

/**
 * Source Explorer state selector helper.
 */
export const getSourceExplorerState = createSelector(
  createFeatureSelector<State>('raven'),
  (state: State): SourceExplorerState => state.sourceExplorer,
);

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
export const getFiltersByTarget = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.filtersByTarget);
export const getInitialSourcesLoaded = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.initialSourcesLoaded);
export const getPending = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.fetchPending);
export const getPins = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.pins);
export const getSelectedSourceId = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.selectedSourceId);
export const getTreeBySourceId = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.treeBySourceId);
