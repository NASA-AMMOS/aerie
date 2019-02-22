/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { initialState, LayoutState, reducer } from './layout.reducer';

import {
  Resize,
  SetMode,
  ToggleApplyLayoutDrawer,
  ToggleApplyLayoutDrawerEvent,
  ToggleDetailsPanel,
  ToggleEpochsDrawer,
  ToggleGlobalSettingsDrawer,
  ToggleLeftPanel,
  ToggleRightPanel,
  ToggleSouthBandsPanel,
  UpdateLayout,
} from '../actions/layout.actions';

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
    layoutState = reducer(
      layoutState,
      new SetMode('custom', false, false, true, false),
    );

    expect(layoutState).toEqual({
      ...initialState,
      mode: 'custom',
      showDetailsPanel: false,
      showLeftPanel: false,
      showRightPanel: true,
      showSouthBandsPanel: false,
    });
  });

  it('handle ToggleDetailsPanel', () => {
    layoutState = reducer(layoutState, new ToggleDetailsPanel());
    expect(layoutState).toEqual({
      ...initialState,
      showDetailsPanel: !initialState.showDetailsPanel,
    });
  });

  it('handle ToggleApplyLayoutDrawer', () => {
    layoutState = reducer(layoutState, new ToggleApplyLayoutDrawer());
    expect(layoutState).toEqual({
      ...initialState,
      showApplyLayoutDrawer: true,
      showGlobalSettingsDrawer: false,
    });
  });

  it('handle ToggleApplyLayoutDrawerEvent', () => {
    layoutState = reducer(layoutState, new ToggleApplyLayoutDrawerEvent());
    expect(layoutState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle ToggleEpochsDrawer', () => {
    layoutState = reducer(layoutState, new ToggleEpochsDrawer());
    expect(layoutState).toEqual({
      ...initialState,
      showEpochsDrawer: true,
      showGlobalSettingsDrawer: false,
    });
  });

  it('handle ToggleGlobalSettingsDrawer', () => {
    layoutState = reducer(layoutState, new ToggleGlobalSettingsDrawer());
    expect(layoutState).toEqual({
      ...initialState,
      showEpochsDrawer: false,
      showGlobalSettingsDrawer: true,
    });
  });

  it('handle ToggleLeftPanel', () => {
    layoutState = reducer(layoutState, new ToggleLeftPanel());
    expect(layoutState).toEqual({
      ...initialState,
      showLeftPanel: !initialState.showLeftPanel,
    });
  });

  it('handle ToggleRightPanel', () => {
    layoutState = reducer(layoutState, new ToggleRightPanel());
    expect(layoutState).toEqual({
      ...initialState,
      showRightPanel: !initialState.showRightPanel,
    });
  });

  it('handle ToggleSouthBandsPanel', () => {
    layoutState = reducer(layoutState, new ToggleSouthBandsPanel());
    expect(layoutState).toEqual({
      ...initialState,
      showSouthBandsPanel: !initialState.showSouthBandsPanel,
    });
  });

  it('handle UpdateLayout', () => {
    layoutState = reducer(
      layoutState,
      new UpdateLayout({ timelinePanelSize: 50 }),
    );
    expect(layoutState).toEqual({ ...initialState, timelinePanelSize: 50 });
  });
});
