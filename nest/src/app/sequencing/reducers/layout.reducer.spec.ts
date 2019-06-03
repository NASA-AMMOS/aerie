/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  SetPanelSizes,
  ToggleLeftPanelVisible,
  ToggleRightPanelVisible,
} from '../actions/layout.actions';
import { initialState, reducer } from './layout.reducer';

describe('Layout reducer', () => {
  it('handle default', () => {
    expect(initialState).toEqual(initialState);
  });

  it('should handle SetPanelSizes', () => {
    const leftPanelSize = 100;
    const middlePanelSize = 1000;
    const rightPanelSize = 10000;

    const result = reducer(
      initialState,
      new SetPanelSizes([leftPanelSize, middlePanelSize, rightPanelSize]),
    );

    expect(result).toEqual({
      ...initialState,
      leftPanelSize,
      middlePanelSize,
      rightPanelSize,
    });
  });

  it('should handle ToggleLeftPanelVisible', () => {
    const result = reducer(initialState, new ToggleLeftPanelVisible());

    expect(result).toEqual({
      ...initialState,
      leftPanelVisible: false,
    });
  });

  it('should handle ToggleRightPanelVisible', () => {
    const result = reducer(initialState, new ToggleRightPanelVisible());

    expect(result).toEqual({
      ...initialState,
      rightPanelVisible: false,
    });
  });
});
