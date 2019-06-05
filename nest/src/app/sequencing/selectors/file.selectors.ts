/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { FileState } from '../reducers/file.reducer';
import { State } from '../sequencing-store';

const featureSelector = createFeatureSelector<State>('sequencing');
export const getFileState = createSelector(
  featureSelector,
  (state: State): FileState => state.file,
);

/**
 * Gets a list of the opened tabs for a editor instance
 * Used to enumerate a tabs list view
 */
export const getOpenedTabs = createSelector(
  getFileState,
  (state: FileState, props: any) => {
    if (props.editorId in state.editors) {
      const editor = state.editors[props.editorId];
      const { openedTabs } = editor;

      return openedTabs ? Object.values(openedTabs) : [];
    }

    // Handles the case where the editor was removed
    return null;
  },
);

export const getOpenedTabsByName = createSelector(
  getFileState,
  (state: FileState, props: any) => state.editors[props.editorId].openedTabs,
);

/**
 * Gets the current tab id for an editor instance
 */
export const getCurrentTab = createSelector(
  getFileState,
  (state: FileState, props: any) => {
    if (props.editorId in state.editors) {
      return state.editors[props.editorId].currentTab;
    }

    // Handles the case where the editor was removed
    return null;
  },
);

/**
 * Gets the current active file for an editor instance
 */
export const getCurrentFile = createSelector(
  getFileState,
  (state: FileState, props: any) => {
    const editor = state.editors[props.editorId];
    const { currentTab, openedTabs } = editor;

    if (editor && openedTabs && currentTab) {
      return openedTabs[currentTab];
    }
    return null;
  },
);

export const getEditors = createSelector(
  getFileState,
  (state: FileState) => state.editors,
);

export const getEditorsList = createSelector(
  getFileState,
  (state: FileState) => Object.values(state.editors),
);
