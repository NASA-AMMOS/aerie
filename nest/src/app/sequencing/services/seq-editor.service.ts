/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ElementRef, Injectable } from '@angular/core';
import * as CodeMirror from 'codemirror';
import { StringTMap } from '../../shared/models';

/**
 * This service stores the Code Mirror editor instance used
 * in the <seq-editor> component.
 * This is because we cant store the editor in the NgRx store, and we
 * still need a way to easily update the editor in the application.
 */
@Injectable({
  providedIn: 'root',
})
export class SeqEditorService {
  editor: CodeMirror.Editor | null = null;
  editors: StringTMap<CodeMirror.Editor | null> = {};

  /**
   * Focus the Code Mirror editor instance.
   */
  focusEditor(id: string): void {
    const editor = this.editors[id];
    if (editor) {
      editor.focus();
    }
  }

  /**
   * Initialize a new Code Mirror editor instance if one does not exist yet.
   */
  setEditor(
    elementRef: ElementRef,
    id: string,
    options?: CodeMirror.EditorConfiguration,
  ): void {
    let editor = this.editors[id];

    if (!editor) {
      editor = CodeMirror(elementRef.nativeElement, options);
      editor.on('blur', () => {
        // Always show the cursor even when we are not focused on the editor
        // so we can visually see where new commands are going to be added.
        const cursors = document.querySelector(
          '.CodeMirror-cursors',
        ) as HTMLElement;
        if (cursors) {
          cursors.style.visibility = 'visible';
        }
      });

      this.editors[id] = editor;
    }
  }

  /**
   * Replaces the entire document selection if one exists,
   * or adds text to the document on a new line after the cursor.
   */
  addText(text: string, id: string): void {
    const editor = this.editors[id];

    if (editor) {
      const doc = editor.getDoc();
      const cursor = doc.getCursor();
      const line = doc.getLine(cursor.line);
      const selection = doc.getSelection();

      if (selection !== '') {
        // If there is a selection, replace the entire selection with the given text.
        doc.replaceSelection(text);
      } else {
        // Create a new object to avoid mutation of the original selection.
        const pos: CodeMirror.Position = {
          ch: line.length,
          line: cursor.line,
        };

        if (!line.length) {
          // If the cursor is on an empty line, just add the text to that line.
          doc.replaceRange(text, pos);
        } else {
          // Otherwise add the text on a new line below the current cursor line.
          doc.replaceRange(`\n${text}`, pos);
        }
      }
    }
  }
}
