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
  showActivityTypesDrawer: boolean;
  showAddActivityDrawer: boolean;
  showCreatePlanDrawer: boolean;
  showEditActivityDrawer: boolean;
  showLoadingBar: number;
}

export const initialState: LayoutState = {
  showActivityTypesDrawer: false,
  showAddActivityDrawer: false,
  showCreatePlanDrawer: false,
  showEditActivityDrawer: false,
  showLoadingBar: 0,
};

export const reducer = createReducer(
  initialState,
  on(LayoutActions.closeAllDrawers, state => ({
    ...state,
    showActivityTypesDrawer: false,
    showAddActivityDrawer: false,
    showCreatePlanDrawer: false,
    showEditActivityDrawer: false,
  })),
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
  on(LayoutActions.toggleActivityTypesDrawer, (state, action) => ({
    ...state,
    showActivityTypesDrawer:
      action.opened !== undefined
        ? action.opened
        : !state.showActivityTypesDrawer,
    showAddActivityDrawer: false,
    showCreatePlanDrawer: false,
    showEditActivityDrawer: false,
  })),
  on(LayoutActions.toggleAddActivityDrawer, (state, action) => ({
    ...state,
    showActivityTypesDrawer: false,
    showAddActivityDrawer:
      action.opened !== undefined
        ? action.opened
        : !state.showAddActivityDrawer,
    showCreatePlanDrawer: false,
    showEditActivityDrawer: false,
  })),
  on(LayoutActions.toggleCreatePlanDrawer, (state, action) => ({
    ...state,
    showActivityTypesDrawer: false,
    showAddActivityDrawer: false,
    showCreatePlanDrawer:
      action.opened !== undefined ? action.opened : !state.showCreatePlanDrawer,
    showEditActivityDrawer: false,
  })),
  on(LayoutActions.toggleEditActivityDrawer, (state, action) => ({
    ...state,
    showActivityTypesDrawer: false,
    showAddActivityDrawer: false,
    showCreatePlanDrawer: false,
    showEditActivityDrawer:
      action.opened !== undefined
        ? action.opened
        : !state.showEditActivityDrawer,
  })),
);
