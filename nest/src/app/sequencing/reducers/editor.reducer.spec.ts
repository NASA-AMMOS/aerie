/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { SetLine, SetText } from '../actions/editor.actions';
import { EditorState, initialState, reducer } from './editor.reducer';

describe('Editor reducer', () => {
  it('handle default', () => {
    expect(initialState).toEqual(initialState);
  });

  it('should handle SetLine', () => {
    const line = 42;
    const result: EditorState = reducer(initialState, new SetLine(line));

    expect(result).toEqual({
      ...initialState,
      line,
    });
  });

  it('should handle SetText', () => {
    const text = 'i like turtles';
    const result: EditorState = reducer(initialState, new SetText(text));

    expect(result).toEqual({
      ...initialState,
      text,
    });
  });
});
