import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';

@Component({
  selector: 'app-about-dialog',
  styles: [''],
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
export class AboutDialogComponent {
  copyright = [
    `Copyright ${new Date().getFullYear()}, by the California Institute of Technology.`,
    `ALL RIGHTS RESERVED.`,
    `United States Government sponsorship acknowledged.`,
    `Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.`,
  ];

  constructor(
    public dialogRef: MatDialogRef<AboutDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: { version: string },
  ) {}
}
