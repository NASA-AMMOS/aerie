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

import * as CodeMirror from 'codemirror';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hb-code-mirror',
  template: `<div #codeMirror></div>`,
})

export class HBCodeMirrorComponent implements AfterViewInit, OnChanges {
  @ViewChild('codeMirror') codeMirror: ElementRef;

  @Input() lineNumbers: boolean | undefined = true;
  @Input() mode = '';
  @Input() value = '';

  @Output() beforeChange: EventEmitter<null> = new EventEmitter<null>();
  @Output() change: EventEmitter<CodeMirror.Editor> = new EventEmitter<CodeMirror.Editor>();
  @Output() changes: EventEmitter<CodeMirror.Editor> = new EventEmitter<CodeMirror.Editor>();

  codeMirrorInstance: CodeMirror.Editor;

  ngAfterViewInit() {
    this.codeMirrorInstance = CodeMirror(this.codeMirror.nativeElement, {
      lineNumbers: this.lineNumbers,
      mode:  this.mode,
      value: this.value,
    });

    // Events.
    this.codeMirrorInstance.on('beforeChange', () => this.beforeChange.emit());
    this.codeMirrorInstance.on('change', (event: CodeMirror.Editor) => this.change.emit(event));
    this.codeMirrorInstance.on('changes', (event: CodeMirror.Editor) => this.changes.emit(event));
  }

  ngOnChanges(changes: SimpleChanges) {
    // Line Numbers.
    if (changes.lineNumbers && this.codeMirrorInstance) {
      this.codeMirrorInstance.setOption('lineNumbers', this.lineNumbers);
    }

    // Mode.
    if (changes.mode && this.codeMirrorInstance) {
      this.codeMirrorInstance.setOption('mode', this.mode);
    }

    // Value.
    if (changes.value && this.codeMirrorInstance) {
      this.codeMirrorInstance.setValue(this.value);
    }
  }
}
