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
  RavenFile,
} from './../../shared/models';

@Component({
  selector: 'raven-file-import-dialog',
  styleUrls: ['./raven-file-import-dialog.component.css'],
  templateUrl: './raven-file-import-dialog.component.html',
})
export class RavenFileImportDialogComponent {
  file: RavenFile = {
    data: '',
    mapping: '',
    name: '',
    type: 'epoch', // Initialize to epoch.
  };

  constructor(
    public dialogRef: MatDialogRef<RavenFileImportDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {}

  /**
   * Cancels and closes the dialog.
   */
  onCancel() {
    this.dialogRef.close({ import: false });
  }

  /**
   * Returns the file to the calling component.
   */
  onImport() {
    this.dialogRef.close({
      file: this.file,
      import: true,
    });
  }

  /**
   * Read a file via a FileReader and sets local file data based on the result.
   */
  readFile(file: File, mapping: boolean): void {
    const reader: FileReader = new FileReader();

    reader.onloadend = () => {
      if (mapping) {
        this.file.mapping = reader.result;
      } else {
        this.file.data = reader.result;
      }
    };

    reader.readAsText(file);
  }
}
