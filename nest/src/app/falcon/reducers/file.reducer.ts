/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import keyBy from 'lodash-es/keyBy';
import omit from 'lodash-es/omit';
import uniqueId from 'lodash-es/uniqueId';
import { v4 as uuid } from 'uuid';
import { SequenceFile } from '../../../../../sequencing/src/models';
import { StringTMap } from '../../shared/models';
import {
  CloseTab,
  CreateTab,
  FileActions,
  FileActionTypes,
  SetActiveEditor,
  SwitchTab,
  UpdateChildren,
  UpdateTab,
} from '../actions/file.actions';
import { Editor } from '../models';

export interface FileState {
  activeEditor: string;
  editors: StringTMap<Editor>;
  files: StringTMap<SequenceFile>;
}

export const defaultEditorId = 'editor1';

export const initialState: FileState = {
  activeEditor: defaultEditorId,
  editors: {
    [defaultEditorId]: {
      currentTab: null,
      id: defaultEditorId,
      openedTabs: null,
    },
  },
  files: {
    root: {
      childIds: [],
      content: '',
      expanded: false,
      id: 'root',
      name: 'root',
      parentId: '',
      timeCreated: 0,
      timeLastUpdated: 0,
      type: 'folder',
    },
  },
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: FileState = initialState, action: FileActions) {
  switch (action.type) {
    case FileActionTypes.AddEditor:
      return addEditor(state);
    case FileActionTypes.CloseTab:
      return closeTab(state, action);
    case FileActionTypes.CreateTab:
      return createTab(state, action);
    case FileActionTypes.SetActiveEditor:
      return setActiveEditor(state, action);
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
 * Creates and adds a new editor instance
 */
function addEditor(state: FileState) {
  const id = uuid();
  const newEditor = {
    currentTab: null,
    id,
    openedTabs: null,
  };

  return {
    ...state,
    editors: {
      ...state.editors,
      [id]: newEditor,
    },
  };
}

/**
 * Reduction helper. Called when reducing the `CloseTab` action.
 * Removes the editor instance if there are no open tabs left
 */
function closeTab(state: FileState, action: CloseTab): FileState {
  const openedTabs = Object.keys(
    omit(state.editors[action.editorId].openedTabs, action.docIdToClose),
  );
  const editors = Object.keys(state.editors);

  // If an editor has no open tabs and it's not the only editor instance, remove the editor instance
  if (openedTabs.length === 0 && editors.length > 1) {
    return {
      ...state,
      editors: omit(state.editors, action.editorId),
    };
  }

  // If an editor has no open tabs and it is the only editor instance, reset the editor instance
  if (openedTabs.length === 0 && editors.length === 1) {
    return {
      ...state,
      editors: {
        ...state.editors,
        [action.editorId]: {
          currentTab: null,
          id: action.editorId,
          openedTabs: null,
        },
      },
    };
  }

  // Otherwise remove the tab
  return {
    ...state,
    editors: {
      ...state.editors,
      [action.editorId]: {
        ...state.editors[action.editorId],
        openedTabs: omit(
          state.editors[action.editorId].openedTabs,
          action.docIdToClose,
        ),
      },
    },
  };
}

/**
 * Reduction helper. Called when reducing the `CreateTab` action.
 */
function createTab(state: FileState, action: CreateTab): FileState {
  const id = action.id || uniqueId();
  const newTab = {
    filename: `New File ${id}`,
    id,
    text: '',
  };

  let newCurrentTab = state.editors[action.editorId].currentTab;
  // If there are currently no open tabs, set the currentTab to the
  // newly created tab
  if (!newCurrentTab) {
    newCurrentTab = id;
  }

  return {
    ...state,
    editors: {
      ...state.editors,
      [action.editorId]: {
        ...state.editors[action.editorId],
        currentTab: newCurrentTab,
        openedTabs: {
          ...state.editors[action.editorId].openedTabs,
          [id]: newTab,
        },
      },
    },
  };
}

/**
 * Sets the active editor
 * Used for when an action in the UI requires interacting with the active editor instance
 * @example Adding commands from the list to the active editor instance
 */
function setActiveEditor(state: FileState, action: SetActiveEditor): FileState {
  return {
    ...state,
    activeEditor: action.editorId,
  };
}

/**
 * Reduction helper. Called when reducing the `SwitchTab` action.
 */
function switchTab(state: FileState, action: SwitchTab): FileState {
  // If the editor was removed, skip
  if (!(action.editorId in state.editors)) {
    return state;
  }

  const editor = state.editors[action.editorId];

  if (editor.openedTabs) {
    const { switchToId } = action;

    if (switchToId in editor.openedTabs) {
      // If the key still exists, switch to that tab
      return {
        ...state,
        editors: {
          ...state.editors,
          [action.editorId]: {
            ...state.editors[action.editorId],
            currentTab: action.switchToId,
          },
        },
      };
    } else {
      // If the key was deleted, switch to the farthest right tab
      const openedTabKeys = Object.keys(editor.openedTabs);
      const lastTabKey = openedTabKeys[openedTabKeys.length - 1];

      return {
        ...state,
        editors: {
          ...state.editors,
          [action.editorId]: {
            ...state.editors[action.editorId],
            currentTab: lastTabKey,
          },
        },
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
      ...state.files,
      [action.parentId]: {
        ...state.files[action.parentId],
        childIds: action.children.map(child => child.id),
        ...action.options,
      },
      ...keyBy(action.children, 'id'),
    },
  };
}

/**
 * Reduction helper. Called when reducing the `UpdateTab` action.
 * Finds the editor instance and updates the document text
 */
function updateTab(state: FileState, action: UpdateTab): FileState {
  const { docIdToUpdate, text, editorId } = action;
  const { openedTabs } = state.editors[editorId];
  let doc = null;

  if (openedTabs) {
    doc = openedTabs[docIdToUpdate];
  }

  if (doc) {
    return {
      ...state,
      editors: {
        ...state.editors,
        [editorId]: {
          ...state.editors[editorId],
          openedTabs: {
            ...state.editors[editorId].openedTabs,
            [docIdToUpdate]: {
              ...doc,
              text,
            },
          },
        },
      },
    };
  }

  return state;
}
