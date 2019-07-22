/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { SituationalAwarenessActions } from '../actions';
import {
  initialState,
  reducer,
  SituationalAwarenessState,
} from './situational-awareness.reducer';

describe('situationalAwareness reducer', () => {
  let situationalAwarenessState: SituationalAwarenessState;

  beforeEach(() => {
    situationalAwarenessState = initialState;
  });

  it('handle default', () => {
    expect(situationalAwarenessState).toEqual(initialState);
  });

  it('handle UpdateSituationalAwarenessSettings', () => {
    situationalAwarenessState = reducer(
      situationalAwarenessState,
      SituationalAwarenessActions.updateSituationalAwarenessSettings({
        update: {
          nowMinus: 104800,
          nowPlus: 5184000,
          pageDuration: 604800,
          pefEntries: [],
          situationalAware: true,
          startTime: null,
          useNow: true,
        },
      }),
    );
    expect(situationalAwarenessState).toEqual({
      ...initialState,
      nowMinus: 104800,
      nowPlus: 5184000,
      pageDuration: 604800,
      pefEntries: [],
      situationalAware: true,
      startTime: null,
      useNow: true,
    });
  });
});
