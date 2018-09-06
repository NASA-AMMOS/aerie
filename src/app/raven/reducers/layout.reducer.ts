/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { State } from '../raven-store';

import {
  LayoutAction,
  LayoutActionTypes,
  SetMode,
  ToggleApplyLayoutDrawer,
  ToggleEpochsDrawer,
  ToggleGlobalSettingsDrawer,
  ToggleOutputDrawer,
  ToggleSituationalAwarenessDrawer,
  ToggleTimeCursorDrawer,
} from '../actions/layout.actions';

// Layout State Interface.
export interface LayoutState {
  mode: string;
  rightPanelSelectedTabIndex: number | null;
  showActivityPointMetadata: boolean;
  showActivityPointParameters: boolean;
  showApplyLayoutDrawer: boolean;
  showDetailsPanel: boolean;
  showEpochsDrawer: boolean;
  showGlobalSettingsDrawer: boolean;
  showLeftPanel: boolean;
  showOutputDrawer: boolean;
  showRightPanel: boolean;
  showSituationalAwarenessDrawer: boolean;
  showSouthBandsPanel: boolean;
  showTimeCursorDrawer: boolean;
  timelinePanelSize: number;
}

// Layout State.
export const initialState: LayoutState = {
  mode: 'default',
  rightPanelSelectedTabIndex: 0,
  showActivityPointMetadata: false,
  showActivityPointParameters: true,
  showApplyLayoutDrawer: false,
  showDetailsPanel: true,
  showEpochsDrawer: false,
  showGlobalSettingsDrawer: false,
  showLeftPanel: true,
  showOutputDrawer: false,
  showRightPanel: true,
  showSituationalAwarenessDrawer: false,
  showSouthBandsPanel: true,
  showTimeCursorDrawer: false,
  timelinePanelSize: 50,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: LayoutState = initialState,
  action: LayoutAction,
): LayoutState {
  switch (action.type) {
    case LayoutActionTypes.SetMode:
      return setMode(state, action);
    case LayoutActionTypes.ToggleApplyLayoutDrawer:
      return toggleApplyLayoutDrawer(state, action);
    case LayoutActionTypes.ToggleDetailsPanel:
      return { ...state, showDetailsPanel: !state.showDetailsPanel };
    case LayoutActionTypes.ToggleEpochsDrawer:
      return toggleEpochsDrawer(state, action);
    case LayoutActionTypes.ToggleGlobalSettingsDrawer:
      return toggleGlobalSettingsDrawer(state, action);
    case LayoutActionTypes.ToggleLeftPanel:
      return { ...state, showLeftPanel: !state.showLeftPanel };
    case LayoutActionTypes.ToggleOutputDrawer:
      return toggleOutputDrawer(state, action);
    case LayoutActionTypes.ToggleRightPanel:
      return { ...state, showRightPanel: !state.showRightPanel };
    case LayoutActionTypes.ToggleSituationalAwarenessDrawer:
      return toggleSituationalAwarenessDrawer(state, action);
    case LayoutActionTypes.ToggleSouthBandsPanel:
      return { ...state, showSouthBandsPanel: !state.showSouthBandsPanel };
    case LayoutActionTypes.ToggleTimeCursorDrawer:
      return toggleTimeCursorDrawer(state, action);
    case LayoutActionTypes.UpdateLayout:
      return { ...state, ...action.update };
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'SetMode' action.
 */
export function setMode(state: LayoutState, action: SetMode): LayoutState {
  return {
    ...state,
    mode: action.mode,
    showDetailsPanel: action.showDetailsPanel,
    showLeftPanel: action.showLeftPanel,
    showRightPanel: action.showRightPanel,
    showSouthBandsPanel: action.showSouthBandsPanel,
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleApplyLayoutDrawer' action.
 */
export function toggleApplyLayoutDrawer(
  state: LayoutState,
  action: ToggleApplyLayoutDrawer,
): LayoutState {
  return {
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
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleEpochsDrawer' action.
 */
export function toggleEpochsDrawer(
  state: LayoutState,
  action: ToggleEpochsDrawer,
): LayoutState {
  return {
    ...state,
    showApplyLayoutDrawer: false,
    showEpochsDrawer:
      action.opened !== undefined ? action.opened : !state.showEpochsDrawer,
    showGlobalSettingsDrawer: false,
    showOutputDrawer: false,
    showSituationalAwarenessDrawer: false,
    showTimeCursorDrawer: false,
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleGlobalSettingsDrawer' action.
 */
export function toggleGlobalSettingsDrawer(
  state: LayoutState,
  action: ToggleGlobalSettingsDrawer,
): LayoutState {
  return {
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
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleOutputDrawer' action.
 */
export function toggleOutputDrawer(
  state: LayoutState,
  action: ToggleOutputDrawer,
): LayoutState {
  return {
    ...state,
    showApplyLayoutDrawer: false,
    showEpochsDrawer: false,
    showGlobalSettingsDrawer: false,
    showOutputDrawer:
      action.opened !== undefined ? action.opened : !state.showOutputDrawer,
    showSituationalAwarenessDrawer: false,
    showTimeCursorDrawer: false,
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleSituationalAwarenessDrawer' action.
 */
export function toggleSituationalAwarenessDrawer(
  state: LayoutState,
  action: ToggleSituationalAwarenessDrawer,
): LayoutState {
  return {
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
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleTimeCursorDrawer' action.
 */
export function toggleTimeCursorDrawer(
  state: LayoutState,
  action: ToggleTimeCursorDrawer,
): LayoutState {
  return {
    ...state,
    showApplyLayoutDrawer: false,
    showEpochsDrawer: false,
    showGlobalSettingsDrawer: false,
    showOutputDrawer: false,
    showSituationalAwarenessDrawer: false,
    showTimeCursorDrawer:
      action.opened !== undefined ? action.opened : !state.showTimeCursorDrawer,
  };
}

/**
 * Layout state selector helper.
 */
const featureSelector = createFeatureSelector<State>('raven');
export const getLayoutState = createSelector(
  featureSelector,
  (state: State): LayoutState => state.layout,
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
export const getMode = createSelector(
  getLayoutState,
  (state: LayoutState) => state.mode,
);
