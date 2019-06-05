/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { SequenceFile } from '../../../../../sequencing/src/models';
import { StringTMap } from '../../shared/models';
import { FileState } from '../reducers/file.reducer';
import { State } from '../sequencing-store';

const featureSelector = createFeatureSelector<State>('sequencing');
export const getFileState = createSelector(
  featureSelector,
  (state: State): FileState => state.file,
);

export const getFiles = createSelector(
  getFileState,
  (state: FileState) => state.files,
);

export const getOpenedTabs = createSelector(
  getFileState,
  (state: FileState) =>
    state.openedTabs ? Object.values(state.openedTabs) : [],
);

export const getOpenedTabsByName = createSelector(
  getFileState,
  (state: FileState) => state.openedTabs,
);

export const getCurrentTab = createSelector(
  getFileState,
  (state: FileState) => state.currentTab,
);

export const getCurrentFile = createSelector(
  getFileState,
  (state: FileState) =>
    state.openedTabs && state.currentTab
      ? state.openedTabs[state.currentTab]
      : null,
);

export const getRootFileChildIds = createSelector(
  getFiles,
  (files: StringTMap<SequenceFile>) => files.root.childIds,
);
