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
  selector: 'raven-about-dialog',
  styles: [],
  template: `
    <h1 mat-dialog-title>About</h1>

    <div mat-dialog-content>
      <div *ngFor="let line of copyright">
        {{ line }}
      </div>
      <p>
        {{ data.version }}
      </p>
    </div>

    <div mat-dialog-actions>
      <button mat-button color="accent" matDialogClose>
        Close
      </button>
    </div>
  `,
})
export class RavenAboutDialogComponent {
  copyright = [
    `Copyright ${new Date().getFullYear()}, by the California Institute of Technology.`,
    `ALL RIGHTS RESERVED.`,
    `United States Government sponsorship acknowledged.`,
    `Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.`,
  ];

  constructor(
    public dialogRef: MatDialogRef<RavenAboutDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: { version: string },
  ) {}
}
