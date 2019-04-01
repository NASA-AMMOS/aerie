/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { State } from '../planning-store';
import { LayoutState } from '../reducers/layout.reducer';

const featureSelector = createFeatureSelector<State>('planning');
export const getLayoutState = createSelector(
  featureSelector,
  (state: State): LayoutState => state.layout,
);

export const getShowLoadingBar = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showLoadingBar > 0,
);

export const getShowActivityTypesDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showActivityTypesDrawer,
);

export const getShowAddActivityDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showAddActivityDrawer,
);

export const getShowCreatePlanDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showCreatePlanDrawer,
);

export const getShowEditActivityDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showEditActivityDrawer,
);

export const getShowEditPlanDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showEditPlanDrawer,
);
