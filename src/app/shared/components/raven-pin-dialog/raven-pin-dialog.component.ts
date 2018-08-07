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
  Validators,
} from '@angular/forms';

import {
  RavenPin,
} from '../../models';

@Component({
  selector: 'raven-pin-dialog',
  styleUrls: ['./raven-pin-dialog.component.css'],
  templateUrl: './raven-pin-dialog.component.html',
})
export class RavenPinDialogComponent {
  name: FormControl;

  constructor(
    public dialogRef: MatDialogRef<RavenPinDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {
    let currentName = '';

    if (this.data.pin) {
      currentName = this.data.pin.name;
    }

    this.name = new FormControl(currentName, [
      Validators.required,
      Validators.pattern('^([(a-zA-Z0-9\-\_\s)]*){1,30}$'),
    ]);
  }

  onAdd() {
    this.dialogRef.close({
      pin: {
        name: this.name.value,
        sourceId: this.data.source.id,
      } as RavenPin,
      pinAdd: true,
    });
  }

  onCancel() {
    this.dialogRef.close();
  }

  onRemove() {
    this.dialogRef.close({
      pinRemove: true,
      sourceId: this.data.source.id,
    });
  }

  onRename() {
    this.dialogRef.close({
      newName: this.name.value,
      pinRename: true,
      sourceId: this.data.source.id,
    });
  }
}
