/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createReducer, on } from '@ngrx/store';
import keyBy from 'lodash-es/keyBy';
import omit from 'lodash-es/omit';
import without from 'lodash-es/without';
import { SourceExplorerActions } from '../actions';
import {
  BaseType,
  FilterState,
  RavenCustomFilter,
  RavenGraphableFilterSource,
  RavenPin,
  RavenSource,
  RavenSourceAction,
  StringTMap,
} from '../models';
import { getAllChildIds } from '../util';

export interface SourceExplorerState {
  customFiltersBySourceId: StringTMap<RavenCustomFilter[]>;
  fetchPending: boolean;
  filterState: FilterState;
  filtersByTarget: StringTMap<StringTMap<string[]>>; // Target refers to an id that ties filters to a graphable source.
  initialSourcesLoaded: boolean;
  layout: string;
  layoutPath: string;
  loadErrors: string[];
  pins: RavenPin[];
  selectedSourceId: string;
  shareableName: string;
  sourcePath: string;
  statePath: string;
  treeBySourceId: StringTMap<RavenSource>;
}

export const initialState: SourceExplorerState = {
  customFiltersBySourceId: {},
  fetchPending: false,
  filterState: FilterState.empty(),
  filtersByTarget: {},
  initialSourcesLoaded: false,
  layout: '',
  layoutPath: '',
  loadErrors: [], // List of sourceIds that give errors when trying to load. Cleared after displayed.
  pins: [],
  selectedSourceId: '',
  shareableName: '',
  sourcePath: '',
  statePath: '',
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
      subBandIds: [],
      subKind: '',
      type: '',
      url: '',
    },
  },
};

export const reducer = createReducer(
  initialState,
  on(SourceExplorerActions.addCustomFilter, (state, action) => {
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
  }),
  on(SourceExplorerActions.addFilter, (state, action) => {
    const targetFilters =
      state.filtersByTarget[action.source.filterTarget] || {};
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
  }),
  on(
    SourceExplorerActions.addGraphableFilter,
    SourceExplorerActions.applyLayout,
    SourceExplorerActions.applyState,
    SourceExplorerActions.fetchInitialSources,
    SourceExplorerActions.graphCustomSource,
    SourceExplorerActions.removeSourceEvent,
    SourceExplorerActions.saveState,
    state => ({
      ...state,
      fetchPending: true,
    }),
  ),
  on(SourceExplorerActions.closeEvent, (state, { sourceId }) => ({
    ...state,
    ...updateTreeSource(state, sourceId, {
      opened: false,
      subBandIds: [],
    }),
  })),
  on(SourceExplorerActions.collapseEvent, (state, { sourceId }) => ({
    ...state,
    ...updateTreeSource(state, sourceId, {
      expanded: false,
    }),
  })),
  on(SourceExplorerActions.expandEvent, (state, { sourceId }) => ({
    ...state,
    fetchPending: true,
    ...updateTreeSource(state, sourceId, {
      expanded: true,
    }),
  })),
  on(SourceExplorerActions.loadErrorsAdd, (state, { sourceIds }) => ({
    ...state,
    loadErrors: state.loadErrors.concat(sourceIds),
  })),
  on(SourceExplorerActions.newSources, (state, action) => {
    // Generates a new treeBySourceId data structure with updated childIds for the action.sourceId,
    // and the new child sources keyed off of their id. All child sources need to be updated since the importJobStatus of child
    // source could have been updated.
    const parentSource = state.treeBySourceId[action.sourceId];
    if (parentSource) {
      const sources = action.sources;
      const newChildIds = sources.map(source => source.id);
      const originalChildIds = parentSource.childIds;

      const deletedSourceIds: string[] = [];
      originalChildIds.forEach((originalChildId: string) => {
        if (!newChildIds.includes(originalChildId)) {
          // Source has been deleted, remove source and its descendants.
          deletedSourceIds.push(
            originalChildId,
            ...getAllChildIds(state.treeBySourceId, originalChildId),
          );
        } else if (
          state.treeBySourceId[originalChildId].fileMetadata.createdOn !==
          sources.filter(source => source.id === originalChildId)[0]
            .fileMetadata.createdOn
        ) {
          // Source has been updated, remove its descedants.
          deletedSourceIds.push(
            ...getAllChildIds(state.treeBySourceId, originalChildId),
          );
        }
      });
      return {
        ...state,
        treeBySourceId: {
          ...omit(state.treeBySourceId, deletedSourceIds),
          ...keyBy(sources, 'id'),
          [action.sourceId]: {
            ...parentSource,
            childIds: sources.map(source => source.id),
          },
        },
      };
    } else {
      return { ...state };
    }
  }),
  on(SourceExplorerActions.openEvent, (state, { sourceId }) => ({
    ...state,
    fetchPending: true,
    ...updateTreeSource(state, sourceId, {
      opened: true,
    }),
  })),
  on(SourceExplorerActions.pinAdd, (state, action) => {
    if (state.treeBySourceId[action.pin.sourceId]) {
      return {
        ...state,
        pins: state.pins.concat(action.pin),
        treeBySourceId: {
          ...state.treeBySourceId,
          [action.pin.sourceId]: {
            ...state.treeBySourceId[action.pin.sourceId],
            actions: state.treeBySourceId[action.pin.sourceId].actions.reduce(
              (
                actions: RavenSourceAction[],
                currentAction: RavenSourceAction,
              ) => {
                if (currentAction.event === 'pin-add') {
                  actions.push(
                    { event: 'pin-remove', name: 'Remove Pin' },
                    { event: 'pin-rename', name: 'Rename Pin' },
                  );
                } else {
                  actions.push(currentAction);
                }
                return actions;
              },
              [],
            ),
          },
        },
      };
    }

    return {
      ...state,
    };
  }),
  on(SourceExplorerActions.pinRemove, (state, { sourceId }) => ({
    ...state,
    pins: state.pins.filter(pin => pin.sourceId !== sourceId),
    treeBySourceId: {
      ...state.treeBySourceId,
      [sourceId]: {
        ...state.treeBySourceId[sourceId],
        actions: state.treeBySourceId[sourceId].actions
          .filter(
            (currentAction: any) =>
              currentAction.event !== 'pin-remove' &&
              currentAction.event !== 'pin-rename',
          )
          .concat({ event: 'pin-add', name: 'Add Pin' }),
      },
    },
  })),
  on(SourceExplorerActions.pinRename, (state, action) => ({
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
  })),
  on(SourceExplorerActions.removeCustomFilter, (state, action) => {
    const customFilters = state.customFiltersBySourceId[action.sourceId] || [];

    return {
      ...state,
      customFiltersBySourceId: {
        ...state.customFiltersBySourceId,
        [action.sourceId]: customFilters.filter(
          customFilter => customFilter.label !== action.label,
        ),
      },
    };
  }),
  on(SourceExplorerActions.removeFilter, (state, action) => {
    const targetFilters =
      state.filtersByTarget[action.source.filterTarget] || {};
    const groupFilters = targetFilters[action.source.filterSetOf] || [];

    return {
      ...state,
      filtersByTarget: {
        ...state.filtersByTarget,
        [action.source.filterTarget]: {
          ...targetFilters,
          [action.source.filterSetOf]: groupFilters.filter(
            filter => filter !== action.source.id,
          ),
        },
      },
      ...updateTreeSource(state, action.source.id, { opened: false }),
    };
  }),
  on(SourceExplorerActions.removeGraphableFilter, state => ({
    ...state,
    fetchPending: true,
  })),
  on(SourceExplorerActions.removeSource, (state, { sourceId }) => {
    const source = state.treeBySourceId[sourceId];
    const parentSource = state.treeBySourceId[source.parentId];
    const allChildIds = getAllChildIds(state.treeBySourceId, source.id);

    if (parentSource) {
      return {
        ...state,
        selectedSourceId:
          sourceId === state.selectedSourceId ? '' : state.selectedSourceId,
        treeBySourceId: {
          ...omit(state.treeBySourceId, sourceId, allChildIds),
          [parentSource.id]: {
            ...parentSource,
            childIds: parentSource.childIds.filter(
              (childId: string) => childId !== sourceId,
            ),
          },
        },
      };
    } else {
      console.error(
        'source-explorer.ts - removeSource: you cannot remove a source without a parent: ',
        sourceId,
      );

      return {
        ...state,
      };
    }
  }),
  on(SourceExplorerActions.selectSource, (state, { sourceId }) => ({
    ...state,
    selectedSourceId: sourceId,
  })),
  on(SourceExplorerActions.setCustomFilter, (state, action) => ({
    ...state,
    filtersByTarget: {
      ...state.filtersByTarget,
      [action.source.filterTarget]: {
        ...state.filtersByTarget[action.source.filterTarget],
        [action.source.filterSetOf]: [action.source.id],
      },
    },
    ...updateTreeSource(state, action.source.id, {
      filter: action.filter,
      opened: action.filter ? true : false,
    }),
  })),
  on(SourceExplorerActions.setCustomFilterSubBandId, (state, action) => {
    const customFilters = state.customFiltersBySourceId[action.sourceId] || [];

    return {
      ...state,
      customFiltersBySourceId: {
        ...state.customFiltersBySourceId,
        [action.sourceId]: customFilters.map(customFilter => ({
          ...customFilter,
          subBandId:
            customFilter.label === action.label
              ? action.subBandId
              : customFilter.subBandId,
        })),
      },
    };
  }),
  on(SourceExplorerActions.subBandIdAdd, (state, action) => {
    const subBandIds = without(
      state.treeBySourceId[action.sourceId].subBandIds,
      action.subBandId,
    ).concat(action.subBandId);

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
  }),
  on(SourceExplorerActions.subBandIdRemove, (state, action) => {
    let newCustomFiltersBySourceId = {
      ...state.customFiltersBySourceId,
    };

    let newFiltersByTarget = {
      ...state.filtersByTarget,
    };

    action.sourceIds.forEach(sourceId => {
      const source = state.treeBySourceId[sourceId];

      if (source) {
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
              [graphableFilterSource.filterSetOf]: filters[
                graphableFilterSource.filterSetOf
              ].filter(
                filterSourceId => filterSourceId !== graphableFilterSource.name,
              ),
            },
          };
        }
      }
    });

    return {
      ...state,
      customFiltersBySourceId: newCustomFiltersBySourceId,
      filtersByTarget: newFiltersByTarget,
      treeBySourceId: {
        ...state.treeBySourceId,
        ...action.sourceIds.reduce((sourceIds, sourceId) => {
          if (state.treeBySourceId[sourceId]) {
            const subBandIds = state.treeBySourceId[sourceId].subBandIds.filter(
              (subBandId: string) => subBandId !== action.subBandId,
            );
            const opened = subBandIds.length > 0;
            sourceIds[sourceId] = {
              ...state.treeBySourceId[sourceId],
              opened,
              subBandIds,
            };
          }

          return sourceIds;
        }, {}),
      },
    };
  }),
  on(SourceExplorerActions.updateSourceExplorer, (state, { update }) => ({
    ...state,
    ...update,
  })),
  on(SourceExplorerActions.updateTreeSource, (state, action) => ({
    ...state,
    ...updateTreeSource(state, action.sourceId, action.update),
  })),
);

/**
 * Helper. Return an updated tree source object. Can be used to spread over the state to update it.
 */
function updateTreeSource(
  state: SourceExplorerState,
  sourceId: string,
  update: StringTMap<BaseType>,
) {
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
