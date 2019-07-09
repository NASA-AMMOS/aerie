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

export const reducer = createReducer(
  initialState,
  on(LayoutActions.loadingBarHide, state => {
    const showLoadingBar = state.showLoadingBar - 1;
    return {
      ...state,
      showLoadingBar: showLoadingBar < 0 ? 0 : showLoadingBar,
    };
  }),
  on(LayoutActions.loadingBarShow, state => ({
    ...state,
    showLoadingBar: state.showLoadingBar + 1,
  })),
  on(LayoutActions.setPanelSizes, (state, { sizes }) => {
    const [leftPanelSize, middlePanelSize, rightPanelSize] = sizes;
    return {
      ...state,
      leftPanelSize,
      middlePanelSize,
      rightPanelSize,
    };
  }),
  on(LayoutActions.toggleEditorPanelsDirection, state => ({
    ...state,
    editorPanelsDirection:
      state.editorPanelsDirection === 'horizontal' ? 'vertical' : 'horizontal',
  })),
  on(LayoutActions.toggleLeftPanelVisible, state => ({
    ...state,
    leftPanelVisible: !state.leftPanelVisible,
  })),
  on(LayoutActions.toggleRightPanelVisible, state => ({
    ...state,
    rightPanelVisible: !state.rightPanelVisible,
  })),
);
