/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createReducer, on } from '@ngrx/store';
import { LayoutActions } from '../actions';

export interface LayoutState {
  fetchPending: boolean;
  mode: string;
  showActivityPointMetadata: boolean;
  showActivityPointParameters: boolean;
  showApplyLayoutDrawer: boolean;
  showDetailsPanel: boolean;
  showEpochsDrawer: boolean;
  showFileMetadataDrawer: boolean;
  showGlobalSettingsDrawer: boolean;
  showLeftPanel: boolean;
  showOutputDrawer: boolean;
  showRightPanel: boolean;
  showSituationalAwarenessDrawer: boolean;
  showSouthBandsPanel: boolean;
  showTimeCursorDrawer: boolean;
  timelinePanelSize: number;
}

export const initialState: LayoutState = {
  fetchPending: false,
  mode: 'default',
  showActivityPointMetadata: true,
  showActivityPointParameters: true,
  showApplyLayoutDrawer: false,
  showDetailsPanel: true,
  showEpochsDrawer: false,
  showFileMetadataDrawer: false,
  showGlobalSettingsDrawer: false,
  showLeftPanel: true,
  showOutputDrawer: false,
  showRightPanel: true,
  showSituationalAwarenessDrawer: false,
  showSouthBandsPanel: true,
  showTimeCursorDrawer: false,
  timelinePanelSize: 60,
};

export const reducer = createReducer(
  initialState,
  on(LayoutActions.setMode, (state, action) => ({
    ...state,
    mode: action.mode,
    showDetailsPanel: action.showDetailsPanel,
    showLeftPanel: action.showLeftPanel,
    showRightPanel: action.showRightPanel,
    showSouthBandsPanel: action.showSouthBandsPanel,
  })),
  on(LayoutActions.toggleApplyLayoutDrawer, (state, action) => ({
    ...state,
    showApplyLayoutDrawer:
      action.opened !== undefined
        ? action.opened
        : !state.showApplyLayoutDrawer,
    showEpochsDrawer: false,
    showGlobalSettingsDrawer: false,
    showOutputDrawer: false,
    showSituationalAwarenessDrawer: false,
    showTimeCursorDrawer: false,
  })),
  on(LayoutActions.toggleApplyLayoutDrawerEvent, state => ({
    ...state,
    fetchPending: true,
  })),
  on(LayoutActions.toggleDetailsPanel, state => ({
    ...state,
    showDetailsPanel: !state.showDetailsPanel,
  })),
  on(LayoutActions.toggleEpochsDrawer, (state, action) => ({
    ...state,
    showApplyLayoutDrawer: false,
    showEpochsDrawer:
      action.opened !== undefined ? action.opened : !state.showEpochsDrawer,
    showGlobalSettingsDrawer: false,
    showOutputDrawer: false,
    showSituationalAwarenessDrawer: false,
    showTimeCursorDrawer: false,
  })),
  on(LayoutActions.toggleFileMetadataDrawer, (state, action) => {
    const showFileMetadataDrawer =
      action.opened !== undefined
        ? action.opened
        : !state.showFileMetadataDrawer;

    return { ...state, showFileMetadataDrawer };
  }),
  on(LayoutActions.toggleGlobalSettingsDrawer, (state, action) => ({
    ...state,
    showApplyLayoutDrawer: false,
    showEpochsDrawer: false,
    showGlobalSettingsDrawer:
      action.opened !== undefined
        ? action.opened
        : !state.showGlobalSettingsDrawer,
    showOutputDrawer: false,
    showSituationalAwarenessDrawer: false,
    showTimeCursorDrawer: false,
  })),
  on(LayoutActions.toggleLeftPanel, state => ({
    ...state,
    showLeftPanel: !state.showLeftPanel,
  })),
  on(LayoutActions.toggleOutputDrawer, (state, action) => ({
    ...state,
    showApplyLayoutDrawer: false,
    showEpochsDrawer: false,
    showGlobalSettingsDrawer: false,
    showOutputDrawer:
      action.opened !== undefined ? action.opened : !state.showOutputDrawer,
    showSituationalAwarenessDrawer: false,
    showTimeCursorDrawer: false,
  })),
  on(LayoutActions.toggleRightPanel, state => ({
    ...state,
    showRightPanel: !state.showRightPanel,
  })),
  on(LayoutActions.toggleSituationalAwarenessDrawer, (state, action) => ({
    ...state,
    showApplyLayoutDrawer: false,
    showEpochsDrawer: false,
    showGlobalSettingsDrawer: false,
    showOutputDrawer: false,
    showSituationalAwarenessDrawer:
      action.opened !== undefined
        ? action.opened
        : !state.showSituationalAwarenessDrawer,
    showTimeCursorDrawer: false,
  })),
  on(LayoutActions.toggleSouthBandsPanel, state => ({
    ...state,
    showSouthBandsPanel: !state.showSouthBandsPanel,
  })),
  on(LayoutActions.toggleTimeCursorDrawer, (state, action) => ({
    ...state,
    showApplyLayoutDrawer: false,
    showEpochsDrawer: false,
    showGlobalSettingsDrawer: false,
    showOutputDrawer: false,
    showSituationalAwarenessDrawer: false,
    showTimeCursorDrawer:
      action.opened !== undefined ? action.opened : !state.showTimeCursorDrawer,
  })),
  on(LayoutActions.updateLayout, (state, { update }) => ({
    ...state,
    ...update,
  })),
);
