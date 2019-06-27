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
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { CurrentLine } from '../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'seq-command-form-editor',
  styleUrls: ['./seq-command-form-editor.component.css'],
  templateUrl: './seq-command-form-editor.component.html',
})
export class SeqCommandFormEditorComponent implements OnChanges {
  @Input()
  currentLine: CurrentLine;

  @Output()
  setCurrentLine: EventEmitter<any> = new EventEmitter<any>();

  form: FormGroup = new FormGroup({});

  ngOnChanges(changes: SimpleChanges) {
    if (
      changes.currentLine &&
      this.currentLine &&
      this.currentLine.parameters
    ) {
      const forms = {};
      this.currentLine.parameters.forEach(parameter => {
        forms[parameter.name] = new FormControl(parameter.value);
      });

      this.form = new FormGroup(forms);
    }
  }

  /**
   * Formats the name to
   */
  formatParameterName(name: string) {
    return name.split('_').join(' ');
  }

  updateParameters() {
    const payload: CurrentLine = {
      commandName: this.currentLine.commandName,
      parameters: [],
    };

    const parameters = Object.keys(this.form.value);

    parameters.forEach((parameter, index) => {
      const { name, help, type, units } = this.currentLine.parameters[index];
      payload.parameters.push({
        help,
        name,
        type,
        units,
        value: this.form.value[parameter],
      });
    });
    this.setCurrentLine.emit(payload);
  }
}
