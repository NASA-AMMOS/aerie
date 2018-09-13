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
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
} from '@angular/core';

import * as monaco from 'monaco-editor';
import { HBCommand } from '../../models/hb-command';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hb-command-editor',
  templateUrl: './hb-command-editor.component.html',
})
export class HbCommandEditorComponent implements OnInit, OnChanges {
  @Input()
  commands: HBCommand[];

  @Input()
  completionItems: monaco.languages.CompletionItem[];

  @Input()
  language = 'command-language';

  @Input()
  options: monaco.editor.IEditorConstructionOptions;

  @Input()
  theme = 'command-theme';

  @Input()
  themeData: monaco.editor.IStandaloneThemeData;

  @Input()
  tokens: monaco.languages.IMonarchLanguage;

  @Input()
  value = '';

  ngOnInit() {
    this.themeData = this.getTheme();
    this.tokens = this.getTokens();
  }

  ngOnChanges(changes: SimpleChanges) {
    // Commands.
    if (changes.commands && this.commands) {
      this.completionItems = this.getCompletionItems();
    }
  }

  /**
   * Helper. Generates the completion items for the editor.
   */
  getCompletionItems() {
    return this.commands.map(command => {
      return {
        insertText: {
          value: `${command.name} ${command.parameterDefs.map(
            param => `${param.name}`,
          )}`,
        },
        kind: monaco.languages.CompletionItemKind.Text,
        label: command.name,
      };
    });
  }

  /**
   * Helper. Generates the custom theme for the editor.
   *
   * TODO: If needed, generate dynamically theme rules.
   */
  getTheme(): monaco.editor.IStandaloneThemeData {
    return {
      base: 'vs',
      colors: {},
      inherit: false,
      rules: [
        { token: 'command', foreground: '0070a7', fontStyle: 'bold' },
        { token: 'string', foreground: '5ABA7D' },
        { token: 'boolean', foreground: '017ACD' },
      ],
    };
  }

  /**
   * Helper. Generates the language definition for the editor.
   *
   * TODO: If needed, generate dynamically the language definition.
   */
  getTokens(): monaco.languages.IMonarchLanguage {
    return {
      tokenizer: {
        root: [
          [/\b(TRUE)\b|\b(FALSE)\b/, 'boolean'],
          [/^([\w\-]+)/, 'command'],
          [/"(.*)"|'(.*)'/, 'string'],
        ],
      },
    };
  }

  /**
   * Event. Called when the editor has changes.
   *
   * TODO: Handle what to do when the content of the editor changes.
   */
  onEditorChanges(event: monaco.editor.IModelContentChangedEvent) {
    console.log('HbCommandEditorComponent::onChanges', event);
  }
}
