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
  selector: 'raven-custom-graph-dialog',
  styleUrls: ['./raven-custom-graph-dialog.component.css'],
  templateUrl: './raven-custom-graph-dialog.component.html',
})

export class RavenCustomGraphDialogComponent {
  filter: FormControl;
  form: FormGroup;
  label: FormControl;

  constructor(
    public dialogRef: MatDialogRef<RavenCustomGraphDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {
    this.label = new FormControl('', [
      Validators.required,
      Validators.pattern('([(a-zA-Z0-9\-\_\.)]*)'),
    ]);
    if (data.source.arg === 'engine') {
      this.filter = new FormControl('', [
        Validators.required,
        Validators.pattern('^[1-9][0-9]?$|^100$'),
      ]);
    } else {
      this.filter = new FormControl('', [
        Validators.pattern('[a-zA-Z0-9\-\_\.\*\$]*'),
      ]);
    }
    this.form = new FormGroup(
      {
        filter: this.filter,
        label: this.label,
      },
    );
  }

  /**
   * Cancels and closes the dialog.
   */
  onCancel() {
    this.dialogRef.close({ import: false });
  }

  /**
   * Graph button click action.
   */
  onGraph() {
    this.dialogRef.close({
      filter: this.filter.value ? this.filter.value : '.*',
      label: this.label.value,
    });
  }
}
