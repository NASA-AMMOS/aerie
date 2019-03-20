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
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
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
  template: ``,
})
export class SeqEditorComponent implements OnChanges, OnInit {
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

  constructor(
    private elementRef: ElementRef,
    private seqEditorService: SeqEditorService,
  ) {}

  ngOnInit(): void {
    this.seqEditorService.setEditor(this.elementRef, {
      autofocus: this.autofocus,
      extraKeys: this.extraKeys,
      gutters: this.gutters,
      lineNumbers: this.lineNumbers,
      lineWrapping: this.lineWrapping,
      lint: true,
      mode: this.mode,
      theme: this.theme,
      value: this.value,
    });
  }

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
}
