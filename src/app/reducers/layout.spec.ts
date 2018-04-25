/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  initialState,
  LayoutState,
  reducer,
} from './layout';

import {
  Resize,
  SetMode,
  ToggleDetailsDrawer,
  ToggleLeftDrawer,
  ToggleRightDrawer,
  ToggleSouthBandsDrawer,
  UpdateLayout,
} from './../actions/layout';

describe('layout reducer', () => {
  let layoutState: LayoutState;

  beforeEach(() => {
    layoutState = initialState;
  });

  it('handle default', () => {
    expect(layoutState).toEqual(initialState);
  });

  it('handle Resize', () => {
    layoutState = reducer(layoutState, new Resize());
    expect(layoutState).toEqual({
      ...initialState,
    });
  });

  it('handle SetMode', () => {
    layoutState = reducer(layoutState, new SetMode('custom', false, false, true, false));
    expect(layoutState).toEqual({
      ...initialState,
      mode: 'custom',
      showDetailsDrawer: false,
      showLeftDrawer: false,
      showRightDrawer: true,
      showSouthBandsDrawer: false,
    });
  });

  it('handle ToggleDetailsDrawer', () => {
    layoutState = reducer(layoutState, new ToggleDetailsDrawer());
    expect(layoutState).toEqual({ ...initialState, showDetailsDrawer: !initialState.showDetailsDrawer });
  });

  it('handle ToggleLeftDrawer', () => {
    layoutState = reducer(layoutState, new ToggleLeftDrawer());
    expect(layoutState).toEqual({ ...initialState, showLeftDrawer: !initialState.showLeftDrawer });
  });

  it('handle ToggleRightDrawer', () => {
    layoutState = reducer(layoutState, new ToggleRightDrawer());
    expect(layoutState).toEqual({ ...initialState, showRightDrawer: !initialState.showRightDrawer });
  });

  it('handle ToggleSouthBandsDrawer', () => {
    layoutState = reducer(layoutState, new ToggleSouthBandsDrawer());
    expect(layoutState).toEqual({ ...initialState, showSouthBandsDrawer: !initialState.showSouthBandsDrawer });
  });

  it('handle UpdateLayout', () => {
    layoutState = reducer(layoutState, new UpdateLayout({ timelinePanelSize: 50 }));
    expect(layoutState).toEqual({ ...initialState, timelinePanelSize: 50 });
  });
});
