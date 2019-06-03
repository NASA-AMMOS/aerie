/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { omit, uniqueId } from 'lodash';
import { StringTMap } from '../../shared/models';
import { FileActions, FileActionTypes } from '../actions/file.actions';
import { SequenceTab } from '../models';

export interface FileState {
  currentTab: string | null;
  openedTabs: StringTMap<SequenceTab> | null;
}

export const initialState: FileState = {
  currentTab: null,
  openedTabs: null,
};

export function reducer(state: FileState = initialState, action: FileActions) {
  switch (action.type) {
    case FileActionTypes.CreateTab:
      return CreateTab(state);
    case FileActionTypes.SwitchTab:
      return SwitchTab(state, action.switchToId);
    case FileActionTypes.CloseTab:
      return CloseTab(state, action.docIdToClose);
    case FileActionTypes.UpdateTab:
      return UpdateTab(state, action.docIdToUpdate, action.text);
    default:
      return state;
  }
}

function CreateTab(state: FileState) {
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

function SwitchTab(state: FileState, switchToId: string) {
  if (state.openedTabs) {
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

function CloseTab(state: FileState, docIdToClose: string) {
  return {
    ...state,
    openedTabs: omit(state.openedTabs, docIdToClose),
  };
}

function UpdateTab(state: FileState, docIdToUpdate: string, text: string) {
  if (state.openedTabs) {
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
