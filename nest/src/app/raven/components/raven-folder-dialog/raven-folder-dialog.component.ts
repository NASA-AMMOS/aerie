/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClient } from '@angular/common/http';
import { Component, Inject, OnDestroy } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';
import { combineLatest, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { MpsServerSource } from '../../models';
import { RavenExpandableSource } from '../../models';

@Component({
  selector: 'raven-folder-dialog',
  styleUrls: ['./raven-folder-dialog.component.css'],
  templateUrl: './raven-folder-dialog.component.html',
})
export class RavenFolderDialogComponent implements OnDestroy {
  name: FormControl;
  duplicateError: boolean;

  private subscriptions = new Subscription();

  constructor(
    public dialogRef: MatDialogRef<RavenFolderDialogComponent>,
    public http: HttpClient,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {
    this.name = new FormControl('', [
      Validators.required,
      Validators.pattern('^([(a-zA-Z0-9-_s)]*){1,30}$'),
    ]);
    this.duplicateError = false;

    this.subscriptions.add(
      combineLatest([this.name.valueChanges, this.http.get(data.source.url)])
        .pipe(map(([value, sources]) => ({ value, sources })))
        .subscribe(({ value, sources }) => {
          const children = (sources as MpsServerSource[]).map(
            source => source.name,
          );

          // If the current source has a child with the name we are trying to Add,
          // then display a proper overwrite warning.
          if (children.find(name => name === value)) {
            this.duplicateError = true;
          } else {
            this.duplicateError = false;
          }
        }),
    );
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  onAdd() {
    this.dialogRef.close({
      folder: {
        name: this.name.value,
        url: this.data.source.id,
      } as RavenExpandableSource,
      folderAdd: true,
    });
  }

  onCancel() {
    this.dialogRef.close({ folderAdd: false });
  }
}
