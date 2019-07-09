/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  LayoutActions,
  LayoutActionTypes,
  LoadingBarHide,
  SetPanelSizes,
} from '../actions/layout.actions';

export interface LayoutState {
  editorPanelsDirection: string;
  leftPanelSize: number;
  leftPanelVisible: boolean;
  middlePanelSize: number;
  middlePanelVisible: boolean;
  rightPanelSize: number;
  rightPanelVisible: boolean;
  showLoadingBar: number;
}

export const initialState: LayoutState = {
  editorPanelsDirection: 'vertical',
  leftPanelSize: 20,
  leftPanelVisible: true,
  middlePanelSize: 50,
  middlePanelVisible: true,
  rightPanelSize: 30,
  rightPanelVisible: true,
  showLoadingBar: 0,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: LayoutState = initialState,
  action: LayoutActions,
) {
  switch (action.type) {
    case LayoutActionTypes.LoadingBarHide:
      return loadingBarHide(state, action);
    case LayoutActionTypes.LoadingBarShow:
      return { ...state, showLoadingBar: state.showLoadingBar + 1 };
    case LayoutActionTypes.SetPanelSizes:
      return setPanelSizes(state, action);
    case LayoutActionTypes.ToggleEditorPanelsDirection:
      return toggleEditorPanelsDirection(state);
    case LayoutActionTypes.ToggleLeftPanelVisible:
      return { ...state, leftPanelVisible: !state.leftPanelVisible };
    case LayoutActionTypes.ToggleRightPanelVisible:
      return { ...state, rightPanelVisible: !state.rightPanelVisible };
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the `LoadingBarHide` action.
 */
export function loadingBarHide(
  state: LayoutState,
  _: LoadingBarHide,
): LayoutState {
  const showLoadingBar = state.showLoadingBar - 1;
  return {
    ...state,
    showLoadingBar: showLoadingBar < 0 ? 0 : showLoadingBar,
  };
}

/**
 * Reduction helper. Called when reducing the `SetPanelSizes` action.
 */
function setPanelSizes(state: LayoutState, action: SetPanelSizes): LayoutState {
  const [leftPanelSize, middlePanelSize, rightPanelSize] = action.sizes;
  return {
    ...state,
    leftPanelSize,
    middlePanelSize,
    rightPanelSize,
  };
}

/**
 * Reduction Helper. Called when reducing the `ToggleEditorPanelsDirection` action.
 */
function toggleEditorPanelsDirection(state: LayoutState): LayoutState {
  return {
    ...state,
    editorPanelsDirection:
      state.editorPanelsDirection === 'horizontal' ? 'vertical' : 'horizontal',
  };
}
