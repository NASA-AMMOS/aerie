/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { EditorAction, EditorActionTypes } from '../actions/editor.actions';

export interface EditorState {
  line: number;
  text: string;
}

export const initialState: EditorState = {
  line: 0,
  text: '',
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: EditorState = initialState,
  action: EditorAction,
): EditorState {
  switch (action.type) {
    case EditorActionTypes.SetLine:
      return { ...state, line: action.line };
    case EditorActionTypes.SetText:
      return { ...state, text: action.text };
    default:
      return state;
  }
}
