/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { EditorActions } from '../actions';
import { CurrentLine } from '../models';
import { initialState, reducer } from './editor.reducer';

const mockLine: CurrentLine = {
  commandName: 'CONSUME_BOBA',
  parameters: [
    {
      help: 'Everyone likes boba',
      name: 'Boba',
      type: 'string',
      units: 'Boba Balls',
      value: 'Milk Tea',
    },
  ],
};

describe('Editor reducer', () => {
  it('should handle SetCurrentLine', () => {
    const result = reducer(
      initialState,
      EditorActions.setCurrentLine({ currentLine: mockLine }),
    );

    expect(result).toEqual({ currentLine: mockLine });
  });
});
