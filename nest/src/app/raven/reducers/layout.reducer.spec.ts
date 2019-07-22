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

  it('handle Resize', () => {
    layoutState = reducer(layoutState, LayoutActions.resize());
    expect(layoutState).toEqual({
      ...initialState,
    });
  });

  it('handle SetMode', () => {
    layoutState = reducer(
      layoutState,
      LayoutActions.setMode({
        mode: 'custom',
        showDetailsPanel: false,
        showLeftPanel: false,
        showRightPanel: true,
        showSouthBandsPanel: false,
      }),
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
    layoutState = reducer(layoutState, LayoutActions.toggleDetailsPanel());
    expect(layoutState).toEqual({
      ...initialState,
      showDetailsPanel: !initialState.showDetailsPanel,
    });
  });

  it('handle ToggleApplyLayoutDrawer', () => {
    layoutState = reducer(
      layoutState,
      LayoutActions.toggleApplyLayoutDrawer({}),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showApplyLayoutDrawer: true,
      showGlobalSettingsDrawer: false,
    });
  });

  it('handle ToggleApplyLayoutDrawerEvent', () => {
    layoutState = reducer(
      layoutState,
      LayoutActions.toggleApplyLayoutDrawerEvent({}),
    );
    expect(layoutState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle ToggleEpochsDrawer', () => {
    layoutState = reducer(layoutState, LayoutActions.toggleEpochsDrawer({}));
    expect(layoutState).toEqual({
      ...initialState,
      showEpochsDrawer: true,
      showGlobalSettingsDrawer: false,
    });
  });

  it('should handle ToggleFileMetadataDrawer', () => {
    // An action without a parameter should toggle the current state.
    layoutState = reducer(
      layoutState,
      LayoutActions.toggleFileMetadataDrawer({}),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showFileMetadataDrawer: true,
    });

    layoutState = reducer(
      layoutState,
      LayoutActions.toggleFileMetadataDrawer({}),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showFileMetadataDrawer: false,
    });

    // An action with a parameter should set the state to that parameter.
    layoutState = reducer(
      layoutState,
      LayoutActions.toggleFileMetadataDrawer({ opened: true }),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showFileMetadataDrawer: true,
    });

    layoutState = reducer(
      layoutState,
      LayoutActions.toggleFileMetadataDrawer({ opened: true }),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showFileMetadataDrawer: true,
    });

    layoutState = reducer(
      layoutState,
      LayoutActions.toggleFileMetadataDrawer({ opened: false }),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showFileMetadataDrawer: false,
    });

    layoutState = reducer(
      layoutState,
      LayoutActions.toggleFileMetadataDrawer({ opened: false }),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showFileMetadataDrawer: false,
    });
  });

  it('handle ToggleGlobalSettingsDrawer', () => {
    layoutState = reducer(
      layoutState,
      LayoutActions.toggleGlobalSettingsDrawer({}),
    );
    expect(layoutState).toEqual({
      ...initialState,
      showEpochsDrawer: false,
      showGlobalSettingsDrawer: true,
    });
  });

  it('handle ToggleLeftPanel', () => {
    layoutState = reducer(layoutState, LayoutActions.toggleLeftPanel());
    expect(layoutState).toEqual({
      ...initialState,
      showLeftPanel: !initialState.showLeftPanel,
    });
  });

  it('handle ToggleRightPanel', () => {
    layoutState = reducer(layoutState, LayoutActions.toggleRightPanel());
    expect(layoutState).toEqual({
      ...initialState,
      showRightPanel: !initialState.showRightPanel,
    });
  });

  it('handle ToggleSouthBandsPanel', () => {
    layoutState = reducer(layoutState, LayoutActions.toggleSouthBandsPanel());
    expect(layoutState).toEqual({
      ...initialState,
      showSouthBandsPanel: !initialState.showSouthBandsPanel,
    });
  });

  it('handle UpdateLayout', () => {
    layoutState = reducer(
      layoutState,
      LayoutActions.updateLayout({ update: { timelinePanelSize: 50 } }),
    );
    expect(layoutState).toEqual({ ...initialState, timelinePanelSize: 50 });
  });
});
