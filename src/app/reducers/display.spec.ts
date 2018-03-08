/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  DisplayState,
  initialState,
  reducer,
} from './display';

import {
  StateLoad,
  StateLoadFailure,
  StateLoadSuccess,
  StateSave,
  StateSaveFailure,
  StateSaveSuccess,
} from './../actions/display';

import {
  rootSource,
} from './../shared/mocks';

describe('display reducer', () => {
  let displayState: DisplayState;

  beforeEach(() => {
    displayState = initialState;
  });

  it('handle default', () => {
    expect(displayState).toEqual(initialState);
  });

  it('handle StateLoad', () => {
    displayState = reducer(displayState, new StateLoad(rootSource));

    expect(displayState).toEqual({
      ...displayState,
      stateLoadPending: true,
    });
  });

  it('handle StateLoadFailure', () => {
    displayState = reducer(displayState, new StateLoadFailure());

    expect(displayState).toEqual({
      ...displayState,
      stateLoadPending: false,
    });
  });

  it('handle StateLoadSuccess', () => {
    displayState = reducer(displayState, new StateLoadSuccess());

    expect(displayState).toEqual({
      ...displayState,
      stateLoadPending: false,
    });
  });

  it('handle StateSave', () => {
    displayState = reducer(displayState, new StateSave('hello', rootSource));

    expect(displayState).toEqual({
      ...displayState,
      stateSavePending: true,
    });
  });

  it('handle StateSaveFailure', () => {
    displayState = reducer(displayState, new StateSaveFailure());

    expect(displayState).toEqual({
      ...displayState,
      stateSavePending: false,
    });
  });

  it('handle StateSaveSuccess', () => {
    displayState = reducer(displayState, new StateSaveSuccess());

    expect(displayState).toEqual({
      ...displayState,
      stateSavePending: false,
    });
  });
});
