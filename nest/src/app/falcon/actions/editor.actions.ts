/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';
import { CurrentLine } from '../models';

export const addText = createAction(
  '[falcon-editor] add_text',
  props<{ editorId: string; text: string }>(),
);

export const openEditorHelpDialog = createAction(
  '[falcon-editor] open_editor_help_dialog',
);

export const setCurrentLine = createAction(
  '[falcon-editor] set_current_line',
  props<{ currentLine: CurrentLine }>(),
);
