/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { commands } from '../../shared/mocks/commands';
import { mockCommandDictionaryList } from '../../shared/services/command-dictionary-mock.service';
import {
  FetchCommandDictionaryListSuccess,
  FetchCommandDictionarySuccess,
  SelectCommandDictionary,
} from '../actions/command-dictionary.actions';
import { keyCommandsByName } from '../util';
import {
  CommandDictionaryState,
  initialState,
  reducer,
} from './command-dictionary.reducer';

describe('Command Dictionary reducer', () => {
  it('handle default', () => {
    expect(initialState).toEqual(initialState);
  });

  it('should handle FetchCommandDictionarySuccess', () => {
    const result: CommandDictionaryState = reducer(
      initialState,
      new FetchCommandDictionarySuccess(commands),
    );

    expect(result).toEqual({
      ...initialState,
      commandsByName: keyCommandsByName(commands),
    });
  });

  it('should handle FetchCommandDictionaryListSuccess', () => {
    const result: CommandDictionaryState = reducer(
      initialState,
      new FetchCommandDictionaryListSuccess(mockCommandDictionaryList),
    );

    expect(result).toEqual({
      ...initialState,
      dictionaries: [...mockCommandDictionaryList],
    });
  });

  it('should handle SelectCommandDictionary', () => {
    const result: CommandDictionaryState = reducer(
      initialState,
      new SelectCommandDictionary('42'),
    );
    expect(result).toEqual({
      ...initialState,
      selectedDictionaryId: '42',
    });
  });
});
