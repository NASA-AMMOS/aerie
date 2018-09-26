/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import * as mpsServerMocks from '../../shared/mocks/mps-server';
import { FetchCommandDictionaryListSuccess } from '../actions/command-dictionary.actions';
import {
  CommandDictionaryState,
  initialState,
  reducer,
} from './command-dictionary.reducer';

describe('Command Dictionary reducer', () => {
  it('handle default', () => {
    expect(initialState).toEqual(initialState);
  });

  it('should handle FetchCommandDictionaryListSuccess', () => {
    const result: CommandDictionaryState = reducer(
      initialState,
      new FetchCommandDictionaryListSuccess(
        mpsServerMocks.commandDictionaryList,
      ),
    );
    expect(result).toEqual({
      commands: null,
      dictionaries: [...mpsServerMocks.commandDictionaryList],
      selectedDictionaryId: null,
    });
  });
});
