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
  FormControl,
} from '@angular/forms';

import {
  MAT_DIALOG_DATA,
  MatDialogRef,
} from '@angular/material';

@Component({
  selector: 'raven-layout-apply-dialog',
  styleUrls: ['./raven-layout-apply-dialog.component.css'],
  templateUrl: './raven-layout-apply-dialog.component.html',
})
export class RavenLayoutApplyDialogComponent {
  sources = new FormControl();

  constructor(
    public dialogRef: MatDialogRef<RavenLayoutApplyDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {}

  onApply() {
    this.dialogRef.close({
      apply: true,
      sourceId: this.sources.value.id,
    });
  }

  onCancel() {
    this.dialogRef.close({ apply: false });
  }
}
