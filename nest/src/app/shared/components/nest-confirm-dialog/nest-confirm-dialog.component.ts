/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';

@Component({
  selector: 'nest-confirm-dialog',
  styleUrls: ['./nest-confirm-dialog.component.css'],
  templateUrl: './nest-confirm-dialog.component.html',
})
export class NestConfirmDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<NestConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {}

  onCancel() {
    this.dialogRef.close({ confirm: false });
  }

  onConfirm() {
    this.dialogRef.close({ confirm: true });
  }
}
