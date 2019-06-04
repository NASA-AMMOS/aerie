/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { keyBy, omit, uniqueId } from 'lodash';
import { SequenceFile } from '../../../../../sequencing/src/models';
import { StringTMap } from '../../shared/models';
import {
  CloseTab,
  FileActions,
  FileActionTypes,
  SwitchTab,
  UpdateChildren,
  UpdateTab,
} from '../actions/file.actions';
import { SequenceTab } from '../models';

export interface FileState {
  files: StringTMap<SequenceFile>;
  currentTab: string | null;
  openedTabs: StringTMap<SequenceTab> | null;
}

export const initialState: FileState = {
  currentTab: null,
  files: {
    root: {
      childIds: [],
      content: '',
      id: 'root',
      name: 'root',
      timeCreated: 0,
      timeLastUpdated: 0,
      type: 'directory',
    },
  },
  openedTabs: null,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: FileState = initialState, action: FileActions) {
  switch (action.type) {
    case FileActionTypes.CloseTab:
      return closeTab(state, action);
    case FileActionTypes.CreateTab:
      return createTab(state);
    case FileActionTypes.SwitchTab:
      return switchTab(state, action);
    case FileActionTypes.UpdateChildren:
      return updateChildren(state, action);
    case FileActionTypes.UpdateTab:
      return updateTab(state, action);
    default:
      return state;
  }
}

/**
 * Reduction helper. Called when reducing the `CloseTab` action.
 */
function closeTab(state: FileState, action: CloseTab): FileState {
  return {
    ...state,
    openedTabs: omit(state.openedTabs, action.docIdToClose),
  };
}

/**
 * Reduction helper. Called when reducing the `CreateTab` action.
 */
function createTab(state: FileState): FileState {
  const id = uniqueId();
  const newTab = {
    filename: `New File ${id}`,
    id,
    text: '',
  };

  let newCurrentTab = state.currentTab;
  // If there are currently no open tabs, set the currentTab to the
  // newly created tab
  if (!state.currentTab) {
    newCurrentTab = id;
  }

  const newState = {
    ...state,
    currentTab: newCurrentTab,
    openedTabs: {
      ...state.openedTabs,
      [id]: newTab,
    },
  };

  return newState;
}

/**
 * Reduction helper. Called when reducing the `SwitchTab` action.
 */
function switchTab(state: FileState, action: SwitchTab): FileState {
  if (state.openedTabs) {
    const { switchToId } = action;

    if (switchToId in state.openedTabs) {
      // If the key still exists, switch to that tab
      return {
        ...state,
        currentTab: switchToId,
      };
    } else {
      // If the key was deleted, switch to the farthest right tab
      const openedTabKeys = Object.keys(state.openedTabs);
      const lastTabKey = openedTabKeys[openedTabKeys.length - 1];

      return {
        ...state,
        currentTab: lastTabKey,
      };
    }
  }

  return state;
}

/**
 * Reduction helper. Called when reducing the `UpdateChildren` action.
 */
function updateChildren(state: FileState, action: UpdateChildren): FileState {
  return {
    ...state,
    files: {
      [action.parentId]: {
        ...state.files[action.parentId],
        childIds: action.children.map(child => child.id),
      },
      ...keyBy(action.children, 'id'),
    },
  };
}

/**
 * Reduction helper. Called when reducing the `UpdateTab` action.
 */
function updateTab(state: FileState, action: UpdateTab): FileState {
  if (state.openedTabs) {
    const { docIdToUpdate, text } = action;

    return {
      ...state,
      openedTabs: {
        ...state.openedTabs,
        [docIdToUpdate]: {
          ...state.openedTabs[docIdToUpdate],
          text,
        },
      },
    };
  }
  return state;
}
