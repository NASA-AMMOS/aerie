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
  ToggleActivityTypesDrawer,
  ToggleAddActivityDrawer,
  ToggleCreatePlanDrawer,
  ToggleEditActivityDrawer,
  ToggleEditPlanDrawer,
} from '../actions/layout.actions';

export interface LayoutState {
  showActivityTypesDrawer: boolean;
  showAddActivityDrawer: boolean;
  showCreatePlanDrawer: boolean;
  showEditActivityDrawer: boolean;
  showEditPlanDrawer: boolean;
  showLoadingBar: number;
}

export const initialState: LayoutState = {
  showActivityTypesDrawer: false,
  showAddActivityDrawer: false,
  showCreatePlanDrawer: false,
  showEditActivityDrawer: false,
  showEditPlanDrawer: false,
  showLoadingBar: 0,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: LayoutState = initialState,
  action: LayoutActions,
): LayoutState {
  switch (action.type) {
    case LayoutActionTypes.LoadingBarHide:
      return loadingBarHide(state, action);
    case LayoutActionTypes.LoadingBarShow:
      return { ...state, showLoadingBar: state.showLoadingBar + 1 };
    case LayoutActionTypes.ToggleActivityTypesDrawer:
      return toggleActivityTypesDrawer(state, action);
    case LayoutActionTypes.ToggleAddActivityDrawer:
      return toggleAddActivityDrawer(state, action);
    case LayoutActionTypes.ToggleCreatePlanDrawer:
      return toggleCreatePlanDrawer(state, action);
    case LayoutActionTypes.ToggleEditActivityDrawer:
      return toggleEditActivityDrawer(state, action);
    case LayoutActionTypes.ToggleEditPlanDrawer:
      return toggleEditPlanDrawer(state, action);
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'LoadingBarHide' action.
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
 * Reduction Helper. Called when reducing the 'ToggleActivityTypesDrawer' action.
 */
export function toggleActivityTypesDrawer(
  state: LayoutState,
  action: ToggleActivityTypesDrawer,
): LayoutState {
  return {
    ...state,
    showActivityTypesDrawer:
      action.opened !== undefined
        ? action.opened
        : !state.showActivityTypesDrawer,
    showAddActivityDrawer: false,
    showCreatePlanDrawer: false,
    showEditActivityDrawer: false,
    showEditPlanDrawer: false,
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleAddActivityDrawer' action.
 */
export function toggleAddActivityDrawer(
  state: LayoutState,
  action: ToggleAddActivityDrawer,
): LayoutState {
  return {
    ...state,
    showActivityTypesDrawer: false,
    showAddActivityDrawer:
      action.opened !== undefined
        ? action.opened
        : !state.showAddActivityDrawer,
    showCreatePlanDrawer: false,
    showEditActivityDrawer: false,
    showEditPlanDrawer: false,
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleCreatePlanDrawer' action.
 */
export function toggleCreatePlanDrawer(
  state: LayoutState,
  action: ToggleCreatePlanDrawer,
): LayoutState {
  return {
    ...state,
    showActivityTypesDrawer: false,
    showAddActivityDrawer: false,
    showCreatePlanDrawer:
      action.opened !== undefined ? action.opened : !state.showCreatePlanDrawer,
    showEditActivityDrawer: false,
    showEditPlanDrawer: false,
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleEditActivityDrawer' action.
 */
export function toggleEditActivityDrawer(
  state: LayoutState,
  action: ToggleEditActivityDrawer,
): LayoutState {
  return {
    ...state,
    showActivityTypesDrawer: false,
    showAddActivityDrawer: false,
    showCreatePlanDrawer: false,
    showEditActivityDrawer:
      action.opened !== undefined
        ? action.opened
        : !state.showEditActivityDrawer,
    showEditPlanDrawer: false,
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleEditPlanDrawer' action.
 */
export function toggleEditPlanDrawer(
  state: LayoutState,
  action: ToggleEditPlanDrawer,
): LayoutState {
  return {
    ...state,
    showActivityTypesDrawer: false,
    showAddActivityDrawer: false,
    showCreatePlanDrawer: false,
    showEditActivityDrawer: false,
    showEditPlanDrawer:
      action.opened !== undefined ? action.opened : !state.showEditPlanDrawer,
  };
}
