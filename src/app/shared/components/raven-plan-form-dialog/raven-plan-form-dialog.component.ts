/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';
import { RavenAdaptation } from '../../models/raven-adaptation';
import { RavenPlan } from '../../models/raven-plan';
import { RavenPlanFormDialogData } from '../../models/raven-plan-form-dialog-data';

import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-plan-form-dialog',
  styleUrls: ['./raven-plan-form-dialog.component.css'],
  templateUrl: './raven-plan-form-dialog.component.html',
})
export class RavenPlanFormDialogComponent {
  adaptations: RavenAdaptation[] = [];
  form: FormGroup;
  isNew = false;
  modeText = 'Create New';

  constructor(
    fb: FormBuilder,
    public dialogRef: MatDialogRef<RavenPlanFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RavenPlanFormDialogData,
  ) {
    this.adaptations = data.adaptations;

    const plan: RavenPlan = {
      adaptationId: '',
      endTimestamp: '',
      id: '',
      name: '',
      startTimestamp: '',
    };

    this.form = fb.group({
      adaptationId: new FormControl(plan.adaptationId, [Validators.required]),
      endTimestamp: new FormControl(plan.endTimestamp, [Validators.required]),
      name: new FormControl(plan.name, [Validators.required]),
      startTimestamp: new FormControl(plan.startTimestamp, [
        Validators.required,
      ]),
    });
  }

  onSubmit(value: any) {
    if (this.form.valid) {
      this.dialogRef.close({ ...value });
    }
  }
}
