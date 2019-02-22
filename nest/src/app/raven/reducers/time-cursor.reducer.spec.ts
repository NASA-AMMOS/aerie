/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { initialState, reducer, TimeCursorState } from './time-cursor.reducer';

import {
  HideTimeCursor,
  ShowTimeCursor,
  UpdateTimeCursorSettings,
} from '../actions/time-cursor.actions';

describe('time-cursor reducer', () => {
  let timeCursorState: TimeCursorState;

  beforeEach(() => {
    timeCursorState = initialState;
  });

  it('handle default', () => {
    expect(timeCursorState).toEqual(initialState);
  });

  it('handle HideTimeCursor', () => {
    timeCursorState = reducer(timeCursorState, new HideTimeCursor());
    expect(timeCursorState).toEqual({
      ...initialState,
      cursorTime: null,
      showTimeCursor: false,
    });
  });

  it('handle ShowTimeCursor', () => {
    timeCursorState = reducer(timeCursorState, new ShowTimeCursor());
    expect(timeCursorState).toEqual({
      ...initialState,
      cursorTime: timeCursorState.cursorTime,
      showTimeCursor: true,
    });
  });

  it('handle UpdateTimeCursorSettings', () => {
    timeCursorState = reducer(
      timeCursorState,
      new UpdateTimeCursorSettings({
        autoPage: true,
        clockRate: 42,
        clockUpdateIntervalInSecs: 1,
      }),
    );

    expect(timeCursorState).toEqual({
      ...initialState,
      autoPage: true,
      clockRate: 42,
      clockUpdateIntervalInSecs: 1,
    });
  });
});
