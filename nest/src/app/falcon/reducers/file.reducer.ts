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
import uniqueId from 'lodash-es/uniqueId';
import { v4 as uuid } from 'uuid';
import { StringTMap } from '../../shared/models';
import { FileActions } from '../actions';
import { SequenceFile } from '../models';
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

export const reducer = createReducer(
  initialState,
  on(FileActions.addEditor, state => {
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
  }),
  on(FileActions.closeTab, (state, action) => {
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
  }),
  on(FileActions.createTab, (state, action) => {
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
  }),
  on(FileActions.setActiveEditor, (state, { editorId }) => ({
    ...state,
    activeEditor: editorId,
  })),
  on(FileActions.switchTab, (state, action) => {
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
  }),
  on(FileActions.updateChildren, (state, action) => ({
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
  })),
  on(FileActions.updateTab, (state, action) => {
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
  }),
);
