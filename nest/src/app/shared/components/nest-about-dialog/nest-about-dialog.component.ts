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
import { NestModule } from '../../models';

@Component({
  selector: 'nest-about-dialog',
  styleUrls: ['./nest-about-dialog.component.css'],
  templateUrl: './nest-about-dialog.component.html',
})
export class NestAboutDialogComponent {
  copyright = `Copyright ${new Date().getFullYear()}, by the California Institute of Technology. ALL RIGHTS RESERVED.\nUnited States Government sponsorship acknowledged. Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.`;
  constructor(
    public dialogRef: MatDialogRef<NestAboutDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: { modules: NestModule[]; version: string },
  ) {}
}
