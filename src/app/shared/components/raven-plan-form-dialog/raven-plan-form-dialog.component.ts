/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';

import { RavenAdaptation } from '../../models/raven-adaptation';
import { RavenPlan } from '../../models/raven-plan';
import { RavenPlanFormDialogData } from '../../models/raven-plan-form-dialog-data';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-plan-form-dialog',
  styleUrls: ['./raven-plan-form-dialog.component.css'],
  templateUrl: './raven-plan-form-dialog.component.html',
})
export class RavenPlanFormDialogComponent {
  /**
   * The text to display as the modal title
   */
  modeText = 'Create New';

  /**
   * Whether this is a new or existing plan
   */
  isNew = false;

  /**
   * Adaptations
   */
  adaptations: RavenAdaptation[] = [];

  /**
   * The main form
   */
  form: FormGroup;

  constructor(
    fb: FormBuilder,
    public dialogRef: MatDialogRef<RavenPlanFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RavenPlanFormDialogData,
  ) {
    this.adaptations = data.adaptations;

    const plan: RavenPlan = data.selectedPlan || {
      adaptationId: '',
      end: '',
      id: '',
      name: '',
      start: '',
    };

    if (data.selectedPlan) {
      this.isNew = false;
      this.modeText = 'Edit Existing';
    }

    this.form = fb.group({
      adaptationId: new FormControl(plan.adaptationId, [Validators.required]),
      end: new FormControl(plan.end, [Validators.required]),
      id: new FormControl({ value: plan.id, disabled: !!plan.id }, [
        Validators.required,
        Validators.pattern('^([(a-zA-Z0-9-_)]*){1,30}$'),
      ]),
      name: new FormControl(plan.name, [Validators.required]),
      start: new FormControl(plan.start, [Validators.required]),
    });
  }

  onSubmit(value: any) {
    if (this.form.valid) {
      this.dialogRef.close({ ...value, id: this.form.controls['id'].value });
    }
  }
}
