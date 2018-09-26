/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-plan-form-dialog',
  styleUrls: ['./raven-plan-form-dialog.component.css'],
  templateUrl: './raven-plan-form-dialog.component.html',
})
export class RavenPlanFormDialogComponent {
  modeText = 'Create';
  isNew = false;

  form: FormGroup;

  constructor(
    fb: FormBuilder,
    public dialogRef: MatDialogRef<RavenPlanFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {
    if (data.id) {
      this.isNew = true;
      this.modeText = 'Edit';
    }

    this.form = fb.group({
      end: new FormControl(data.end, [Validators.required]),
      hsoc: new FormControl(data.hsoc, [Validators.required]),
      id: new FormControl({ value: data.id, disabled: !!data.id }, [
        Validators.required,
        Validators.pattern('^([(a-zA-Z0-9-_)]*){1,30}$'),
      ]),
      mpow: new FormControl(data.mpow, [Validators.required]),
      msoc: new FormControl(data.msoc, [Validators.required]),
      name: new FormControl(data.name, [Validators.required]),
      start: new FormControl(data.start, [Validators.required]),
    });
  }

  onSubmit(value: any) {
    if (this.form.valid) {
      this.dialogRef.close({ ...value, id: this.form.controls['id'].value });
    }
  }
}
