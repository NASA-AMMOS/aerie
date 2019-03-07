/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import * as CodeMirror from 'codemirror';
import { MpsCommand } from '../../../../../../schemas/types/ts';
import { StringTMap } from '../../../shared/models';

/**
 * Register a custom `mps` hinter with Code Mirror.
 * Hinters are used for Code Mirror autocomplete.
 */
export function buildMpsHint(commandsByName: StringTMap<MpsCommand>): void {
  const commandNames = Object.keys(commandsByName);

  function commandHint(
    editor: CodeMirror.Editor,
    keywords: string[],
    getToken: (
      e: CodeMirror.Editor,
      cur: CodeMirror.Position,
    ) => CodeMirror.Token,
  ) {
    // @ts-ignore (@types/codemirror is incorrect here).
    const cur = editor.getCursor();
    const token = getToken(editor, cur);

    return {
      from: CodeMirror.Pos(cur.line, token.start),
      list: keywords.filter(word =>
        word.toLowerCase().startsWith(token.string.toLowerCase()),
      ),
      to: CodeMirror.Pos(cur.line, token.end),
    };
  }

  CodeMirror.registerHelper('hint', 'mps', (editor: CodeMirror.Editor) =>
    commandHint(
      editor,
      commandNames,
      (e: CodeMirror.Editor, cur: CodeMirror.Position) => e.getTokenAt(cur),
    ),
  );
}
