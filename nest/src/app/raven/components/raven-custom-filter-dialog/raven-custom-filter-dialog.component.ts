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
  selector: 'raven-custom-filter-dialog',
  styleUrls: ['./raven-custom-filter-dialog.component.css'],
  templateUrl: './raven-custom-filter-dialog.component.html',
})
export class RavenCustomFilterDialogComponent {
  filter: string;

  constructor(
    public dialogRef: MatDialogRef<RavenCustomFilterDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {
    this.filter = data.currentFilter;
  }

  /**
   * Cancels and closes the dialog.
   */
  onCancel() {
    this.dialogRef.close();
  }

  /**
   * Set Filter button click action.
   */
  onSetFilter() {
    this.dialogRef.close({
      filter: this.filter ? this.filter : '',
    });
  }
}
