/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';

import * as monaco from 'monaco-editor';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hb-monaco',
  styles: [
    `
      .editor {
        height: 100%;
      }
    `,
  ],
  template: `<div class="editor" #editor></div>`,
})
export class HbMonacoComponent implements AfterViewInit, OnChanges {
  @ViewChild('editor')
  editor: ElementRef;

  @Input()
  completionItems: monaco.languages.CompletionItem[];

  @Input()
  language: string;

  @Input()
  options: monaco.editor.IEditorConstructionOptions;

  @Input()
  theme = 'vs';

  @Input()
  themeData: monaco.editor.IStandaloneThemeData;

  @Input()
  tokens: monaco.languages.IMonarchLanguage;

  @Input()
  value = '';

  @Output()
  changes: EventEmitter<
    monaco.editor.IModelContentChangedEvent
  > = new EventEmitter<monaco.editor.IModelContentChangedEvent>();

  MonacoInstance: monaco.editor.IStandaloneCodeEditor;

  ngAfterViewInit() {
    // If tokens are defined, register a new language in monaco.
    if (this.tokens) {
      monaco.languages.register({ id: this.language });
      monaco.languages.setMonarchTokensProvider(this.language, this.tokens);
    }

    // If the theme is customized, define it in monaco.
    if (this.themeData) {
      monaco.editor.defineTheme(this.theme, this.themeData);
    }

    // Create Monaco Editor Instance.
    this.MonacoInstance = monaco.editor.create(this.editor.nativeElement, {
      ...this.options,
      language: this.language,
      theme: this.theme,
      value: this.value,
    });

    /**
     * Event. Called when the content of the editor changes.
     */
    this.MonacoInstance.onDidChangeModelContent(
      (event: monaco.editor.IModelContentChangedEvent) =>
        this.changes.emit(event),
    );
  }

  ngOnChanges(changes: SimpleChanges) {
    // CompletionItems.
    if (changes.completionItems && this.MonacoInstance) {
      monaco.languages.registerCompletionItemProvider(this.language, {
        provideCompletionItems: () => this.completionItems,
      });
    }

    // Options.
    if (changes.options && this.MonacoInstance) {
      this.MonacoInstance.updateOptions(this.options);
    }

    // Tokens.
    if (changes.tokens && this.MonacoInstance) {
      monaco.languages.setMonarchTokensProvider(this.language, this.tokens);
    }

    // Value.
    if (changes.value && this.MonacoInstance) {
      this.MonacoInstance.setValue(this.value);
    }
  }
}
