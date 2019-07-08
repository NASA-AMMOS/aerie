/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { LayoutActions } from '../actions';
import { initialState, LayoutState, reducer } from './layout.reducer';

describe('layout reducer', () => {
  let layoutState: LayoutState;

  beforeEach(() => {
    layoutState = initialState;
  });

  it('handle default', () => {
    expect(layoutState).toEqual(initialState);
  });

  it('handle CloseAllDrawers', () => {
    layoutState = reducer(layoutState, LayoutActions.closeAllDrawers());
    expect(layoutState).toEqual({
      ...initialState,
    });
  });

  it('handle LoadingBarHide', () => {
    layoutState = reducer(layoutState, LayoutActions.loadingBarHide());
    expect(layoutState).toEqual({
      ...initialState,
      showLoadingBar: 0,
    });
  });

  it('handle LoadingBarShow', () => {
    layoutState = reducer(layoutState, LayoutActions.loadingBarShow());
    layoutState = reducer(layoutState, LayoutActions.loadingBarShow());
    expect(layoutState).toEqual({
      ...initialState,
      showLoadingBar: 2,
    });
  });

  it('handle Resize', () => {
    layoutState = reducer(layoutState, LayoutActions.resize({}));
    expect(layoutState).toEqual({
      ...initialState,
    });
  });

  it('handle ToggleActivityTypesDrawer', () => {
    layoutState = reducer(
      layoutState,
      LayoutActions.toggleActivityTypesDrawer({}),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showActivityTypesDrawer: true,
    });
  });

  it('handle ToggleAddActivityDrawer', () => {
    layoutState = reducer(
      layoutState,
      LayoutActions.toggleAddActivityDrawer({}),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showAddActivityDrawer: true,
    });
  });

  it('handle ToggleCreatePlanDrawer', () => {
    layoutState = reducer(
      layoutState,
      LayoutActions.toggleCreatePlanDrawer({}),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showCreatePlanDrawer: true,
    });
  });

  it('handle ToggleEditActivityDrawer', () => {
    layoutState = reducer(
      layoutState,
      LayoutActions.toggleEditActivityDrawer({}),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showEditActivityDrawer: true,
    });
  });
});
