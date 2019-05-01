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
import { MpsCommand, StringTMap } from '../../../shared/models';
import {
  buildMpsHint,
  buildMpsLint,
  buildMpsMode,
} from '../../code-mirror-languages/mps';
import { SeqEditorService } from '../../services/seq-editor.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'seq-editor',
  styleUrls: ['./seq-editor.component.css'],
  templateUrl: `./seq-editor.component.html`,
})
export class SeqEditorComponent implements AfterViewInit, OnChanges {
  @Input()
  autofocus = true;

  @Input()
  commandsByName: StringTMap<MpsCommand> = {};

  @Input()
  commands: MpsCommand[] = [];

  @Input()
  extraKeys = {
    'Ctrl-Space': 'autocomplete',
  };

  @Input()
  filename = 'FileName.mps';

  @Input()
  gutters: string[] = ['CodeMirror-lint-markers'];

  @Input()
  lineNumbers = true;

  @Input()
  lineWrapping = true;

  @Input()
  mode = 'mps';

  @Input()
  theme = 'monokai';

  @Input()
  value = '';

  @Output()
  openHelpDialog: EventEmitter<null> = new EventEmitter<null>();

  @ViewChild('editor')
  editorMount: ElementRef;

  autocomplete = false;
  fullscreen = false;
  userTheme = 'dark';

  constructor(private seqEditorService: SeqEditorService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.commandsByName || changes.mode) {
      this.setMode();
    }

    if (this.seqEditorService.editor && changes.lineNumbers) {
      this.seqEditorService.editor.setOption('lineNumbers', this.lineNumbers);
    }

    if (this.seqEditorService.editor && changes.lineWrapping) {
      this.seqEditorService.editor.setOption('lineWrapping', this.lineWrapping);
    }

    if (this.seqEditorService.editor && changes.theme) {
      this.seqEditorService.editor.setOption('theme', this.theme);
    }

    if (this.seqEditorService.editor && changes.value) {
      this.seqEditorService.editor.getDoc().setValue(this.value);
    }
  }

  ngAfterViewInit() {
    this.seqEditorService.setEditor(this.editorMount, {
      autofocus: this.autofocus,
      extraKeys: {
        ...this.extraKeys,
        'Ctrl-R': this.redo.bind(this),
        'Ctrl-Z': this.undo.bind(this),
        Esc: this.toggleFullscreen.bind(this),
      },
      gutters: this.gutters,
      lineNumbers: this.lineNumbers,
      lineWrapping: this.lineWrapping,
      lint: true,
      mode: this.mode,
      theme: this.theme,
      value: this.value,
    });
    if (this.seqEditorService.editor) {
      // Event is casted to any because CodeMirror type defs are not up to date/do not have a type for it
      this.seqEditorService.editor.on(
        'keyup',
        (cm: CodeMirror.Editor, event: any) => {
          // Don't open autocomplete menu again if the menu is open
          // and if user is moving up/down options
          if (
            this.autocomplete &&
            this.seqEditorService.editor &&
            !cm.state.completeActive &&
            event.keyCode !== 13 &&
            event.keyCode !== 40 &&
            event.keyCode !== 38
          ) {
            this.seqEditorService.editor.execCommand('autocomplete');
          }
        },
      );
    }
  }

  /**
   * Set Code Mirror mode and addons.
   * Order matters here! Make sure we register the mode first with Code Mirror
   * before registering the hinter and linter.
   */
  private setMode() {
    if (this.seqEditorService.editor) {
      switch (this.mode) {
        case 'ait':
          // TODO.
          break;
        case 'mps':
        default:
          buildMpsMode(this.commandsByName);
          this.seqEditorService.editor.setOption('mode', 'mps');
          buildMpsHint(this.commandsByName);
          buildMpsLint(this.commandsByName);
          break;
      }
    }
  }

  undo() {
    if (this.seqEditorService.editor) {
      this.seqEditorService.editor.execCommand('undo');
      this.seqEditorService.editor.focus();
    }
  }

  redo() {
    if (this.seqEditorService.editor) {
      this.seqEditorService.editor.execCommand('redo');
      this.seqEditorService.editor.focus();
    }
  }

  toggleAutocomplete() {
    if (this.seqEditorService.editor) {
      this.autocomplete = !this.autocomplete;
      this.seqEditorService.editor.focus();
    }
  }

  toggleFullscreen() {
    if (this.seqEditorService.editor) {
      this.fullscreen = !this.fullscreen;
      this.seqEditorService.editor.setOption('fullScreen', this.fullscreen);
      this.seqEditorService.editor.focus();
    }
  }

  toggleTheme() {
    if (this.seqEditorService.editor) {
      switch (this.userTheme) {
        case 'dark':
          this.userTheme = 'light';
          this.seqEditorService.editor.setOption('theme', 'default');
          break;
        case 'light':
          this.userTheme = 'dark';
          this.seqEditorService.editor.setOption('theme', 'monokai');
          break;
        default:
          this.userTheme = 'dark';
          this.seqEditorService.editor.setOption('theme', 'monokai');
      }
      this.seqEditorService.editor.focus();
    }
  }
}
