/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  CloseAllDrawers,
  LoadingBarHide,
  LoadingBarShow,
  Resize,
  ToggleActivityTypesDrawer,
  ToggleAddActivityDrawer,
  ToggleCreatePlanDrawer,
  ToggleEditActivityDrawer,
  ToggleEditPlanDrawer,
} from '../actions/layout.actions';
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
    layoutState = reducer(layoutState, new CloseAllDrawers());
    expect(layoutState).toEqual({
      ...initialState,
    });
  });

  it('handle LoadingBarHide', () => {
    layoutState = reducer(layoutState, new LoadingBarHide());
    expect(layoutState).toEqual({
      ...initialState,
      showLoadingBar: 0,
    });
  });

  it('handle LoadingBarShow', () => {
    layoutState = reducer(layoutState, new LoadingBarShow());
    layoutState = reducer(layoutState, new LoadingBarShow());
    expect(layoutState).toEqual({
      ...initialState,
      showLoadingBar: 2,
    });
  });

  it('handle Resize', () => {
    layoutState = reducer(layoutState, new Resize());
    expect(layoutState).toEqual({
      ...initialState,
    });
  });

  it('handle ToggleActivityTypesDrawer', () => {
    layoutState = reducer(layoutState, new ToggleActivityTypesDrawer());
    expect(layoutState).toEqual({
      ...initialState,
      showActivityTypesDrawer: true,
    });
  });

  it('handle ToggleAddActivityDrawer', () => {
    layoutState = reducer(layoutState, new ToggleAddActivityDrawer());
    expect(layoutState).toEqual({
      ...initialState,
      showAddActivityDrawer: true,
    });
  });

  it('handle ToggleCreatePlanDrawer', () => {
    layoutState = reducer(layoutState, new ToggleCreatePlanDrawer());
    expect(layoutState).toEqual({
      ...initialState,
      showCreatePlanDrawer: true,
    });
  });

  it('handle ToggleGlobalSettingsDrawer', () => {
    layoutState = reducer(layoutState, new ToggleEditActivityDrawer());
    expect(layoutState).toEqual({
      ...initialState,
      showEditActivityDrawer: true,
    });
  });

  it('handle ToggleEditPlanDrawer', () => {
    layoutState = reducer(layoutState, new ToggleEditPlanDrawer());
    expect(layoutState).toEqual({
      ...initialState,
      showEditPlanDrawer: true,
    });
  });
});
