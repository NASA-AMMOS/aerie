/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';

import * as CodeMirror from 'codemirror';

import 'codemirror/addon/hint/show-hint';
import 'codemirror/addon/lint/lint';
import 'codemirror/addon/mode/simple';

import { HbCommand, StringTMap } from '../../models';
import { CommandLintError, verify } from '../../util';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-seq-editor',
  styles: [
    `
      :host {
        display: block;
        height: 100%;
        width: 100%;
      }
    `,
  ],
  template: ``,
})
export class RavenSeqEditorComponent implements OnChanges, OnInit {
  @Input()
  commandsByName: StringTMap<HbCommand> = {};

  @Input()
  commands: HbCommand[] = [];

  @Input()
  lineNumbers = true;

  @Input()
  lineWrapping = true;

  @Input()
  theme = 'monokai';

  @Input()
  value = '';

  @Output()
  cursorLineChanged: EventEmitter<number> = new EventEmitter<number>();

  @Output()
  valueChanged: EventEmitter<string> = new EventEmitter<string>();

  public editor: CodeMirror.Editor | null = null;

  constructor(public elementRef: ElementRef) {}

  ngOnInit(): void {
    this.editor = CodeMirror(this.elementRef.nativeElement, {
      autofocus: true,
      extraKeys: {
        'Ctrl-Space': 'autocomplete',
      },
      gutters: ['CodeMirror-lint-markers'],
      lineNumbers: this.lineNumbers,
      lineWrapping: this.lineWrapping,
      lint: true,
      mode: 'command',
      theme: this.theme,
      value: this.value,
    });

    this.editor.on('change', (instance: CodeMirror.Editor) => {
      this.valueChanged.emit(instance.getValue());
    });

    this.editor.on('cursorActivity', (instance: CodeMirror.Editor) => {
      const cursor = instance.getDoc().getCursor();
      this.cursorLineChanged.emit(cursor.line);
    });

    this.editor.on('blur', () => {
      // Always show the cursor even when we are not focused on the editor
      // so we can visually see where new commands are going to be added.
      const cursors = document.querySelector(
        '.CodeMirror-cursors',
      ) as HTMLElement;

      if (cursors) {
        cursors.style.visibility = 'visible';
      }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.editor && changes.commandsByName) {
      const commandNames = Object.keys(this.commandsByName);

      // Build and register the "command" mode first since the hinter and linter reference it.
      this.buildMode(commandNames);
      this.editor.setOption('mode', 'command');

      this.buildHint(commandNames);
      this.buildLint(this.commandsByName);
    }

    if (this.editor && changes.lineNumbers) {
      this.editor.setOption('lineNumbers', this.lineNumbers);
    }

    if (this.editor && changes.lineWrapping) {
      this.editor.setOption('lineWrapping', this.lineWrapping);
    }

    if (this.editor && changes.theme) {
      this.editor.setOption('theme', this.theme);
    }

    if (this.editor && changes.value) {
      if (this.editor.getValue() !== this.value) {
        const doc = this.editor.getDoc();
        const cursorPosition = doc.getCursor();

        doc.setValue(this.value);

        // `setValue` resets the cursor. We need to set the previous cursor position
        // on the next frame so we dont get the reset cursor position on the `cursorActivity` event.
        requestAnimationFrame(() => doc.setCursor(cursorPosition));
      }
    }
  }

  /**
   * Register a hinter with Codemirror so the `command` mode can autocomplete commands.
   */
  private buildHint(commandNames: string[]): void {
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

    CodeMirror.registerHelper('hint', 'command', (editor: CodeMirror.Editor) =>
      commandHint(
        editor,
        commandNames,
        (e: CodeMirror.Editor, cur: CodeMirror.Position) => e.getTokenAt(cur),
      ),
    );
  }

  /**
   * Register a linter with Codemirror so the `command` mode can lint commands.
   */
  private buildLint(commandsByName: StringTMap<HbCommand>) {
    CodeMirror.registerHelper('lint', 'command', (text: string) => {
      const found: CodeMirror.Annotation[] = [];

      const parseError = (err: CommandLintError) => {
        const loc = err.lineNumber;

        found.push({
          from: CodeMirror.Pos(loc - 1, 0),
          message: err.message,
          severity: err.level,
          to: CodeMirror.Pos(loc, 0),
        });
      };

      try {
        const errors = verify(commandsByName, text);

        for (let i = 0, l = errors.length; i < l; ++i) {
          parseError(errors[i]);
        }
      } catch (e) {
        found.push({
          from: CodeMirror.Pos(e.location.first_line, 0),
          message: e.message,
          severity: 'error',
          to: CodeMirror.Pos(e.location.last_line, e.location.last_column),
        });
      }

      return found;
    });
  }

  /**
   * Builds and registers a custom `command` mode with the given command dictionary.
   */
  private buildMode(commandNames: string[]): void {
    const instructionRegex = `(${commandNames.join('|')})`;
    const instructionOnlyLine = new RegExp(`${instructionRegex}\\s*$`, 'i');
    const instructionWithArguments = new RegExp(
      `${instructionRegex}(\\s+)`,
      'i',
    );

    //@ts-ignore
    CodeMirror.defineSimpleMode('command', {
      arguments: [
        {
          next: 'start',
          regex: /$/,
          token: null,
        },
        {
          next: 'start',
          token: null,
        },
      ],
      meta: {},
      start: [
        // Highlight an instruction without any arguments (for convenience).
        {
          regex: instructionOnlyLine,
          token: 'variable-2',
        },
        // Highlight an instruction followed by arguments.
        {
          next: 'arguments',
          regex: instructionWithArguments,
          token: ['variable-2', null],
        },
        // Strings.
        {
          regex: /"(?:[^\\]|\\.)*?(?:"|$)/,
          token: 'string',
        },
        // Variables.
        {
          regex: /[A-Z|a-z$][\w$]*/,
          token: 'variable',
        },
        // Numbers.
        {
          regex: /0x[a-f\d]+|[-+]?(?:\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/i,
          token: 'number',
        },
      ],
    });

    //@ts-ignore
    CodeMirror.defineMIME('text/x-command', 'command');
  }
}
