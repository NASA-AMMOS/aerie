/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  Component,
  Inject,
} from '@angular/core';

import {
  MAT_DIALOG_DATA,
  MatDialogRef,
} from '@angular/material';

import {
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';

@Component({
  selector: 'raven-state-save-dialog',
  styleUrls: ['./raven-state-save-dialog.component.css'],
  templateUrl: './raven-state-save-dialog.component.html',
})
export class RavenStateSaveDialogComponent {
  saveStateForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<RavenStateSaveDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {
    this.saveStateForm = new FormGroup({
      name: new FormControl('', [
        Validators.required,
        Validators.pattern('^([(a-zA-Z0-9\-\_\s)]*){1,30}$'),
      ]),
    });
  }

  onCancel() {
    this.dialogRef.close({ save: false });
  }

  onSave() {
    this.dialogRef.close({ name: this.saveStateForm.controls.name.value, save: true });
  }
}
