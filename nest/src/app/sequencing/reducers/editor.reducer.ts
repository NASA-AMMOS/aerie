/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  EditorActions,
  EditorActionTypes,
  SetCurrentLine,
} from '../actions/editor.actions';
import { CurrentLine } from '../models';

export interface EditorState {
  currentLine: CurrentLine | null;
}

export const initialState: EditorState = {
  currentLine: null,
};

export function reducer(
  state: EditorState = initialState,
  action: EditorActions,
) {
  switch (action.type) {
    case EditorActionTypes.SetCurrentLine:
      return setCurrentLine(state, action);
    default:
      return state;
  }
}

/**
 * Reduction helper.
 * Sets the current active line in an editor
 */
function setCurrentLine(state: EditorState, action: SetCurrentLine) {
  return {
    ...state,
    currentLine: action.currentLine,
  };
}
