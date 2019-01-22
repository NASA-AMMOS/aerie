/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { SaveActivityDetail } from '../../actions/plan.actions';
import { HawkAppState } from '../../hawk-store';
import { getActivityTypes } from '../../selectors/adaptation.selector';

import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';

import {
  RavenActivityConstraint,
  RavenActivityDetail,
  RavenActivityParameter,
  RavenActivityType,
} from '../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-activities',
  styleUrls: ['./activities.component.css'],
  templateUrl: './activities.component.html',
})
export class ActivitiesComponent {
  activityTypes: Observable<RavenActivityType[] | null>;
  activityTypeId: string;
  constraints: RavenActivityConstraint[] = [];
  duration: number; // Duration in seconds
  end: number;
  endTimestamp: string;
  id: string | null; // ID of the activity instance. Empty if this is new.
  intent: string; // Intent of the activity instance
  name: string;
  parameters: RavenActivityParameter[] = [];
  sequenceId: string;
  start: number;
  startTimestamp: string;
  activityForm: FormGroup;

  /**
   * Whether this activity type is new or pre-existing
   */
  get isNew() {
    return !this.id;
  }

  constructor(
    private store: Store<HawkAppState>,
    private router: Router,
    private route: ActivatedRoute,
    fb: FormBuilder,
  ) {
    this.activityTypes = this.store.pipe(select(getActivityTypes));

    const activityDetail: RavenActivityDetail | null =
      this.route.snapshot.data.activityDetail || null;

    if (activityDetail) {
      this.activityTypeId = activityDetail.activityTypeId;
      this.constraints = activityDetail.constraints;
      this.duration = activityDetail.duration;
      this.end = activityDetail.end;
      this.endTimestamp = activityDetail.endTimestamp;
      this.id = activityDetail.id;
      this.intent = activityDetail.intent;
      this.name = activityDetail.name;
      this.parameters = activityDetail.parameters;
      this.sequenceId = activityDetail.sequenceId;
      this.start = activityDetail.start;
      this.startTimestamp = activityDetail.startTimestamp;
    }

    this.activityForm = fb.group({
      activityTypeId: new FormControl({
        readonly: !this.isNew,
        value: this.activityTypeId,
      }),
      constraints: fb.array(
        this.constraints.map(constraint =>
          fb.group({
            default: new FormControl(constraint.default),
            locked: new FormControl(constraint.locked),
            name: new FormControl(constraint.name),
            type: new FormControl(constraint.type),
            value: new FormControl(constraint.value),
            values: new FormControl(constraint.values),
          }),
        ),
      ),
      duration: new FormControl(this.duration, [Validators.required]),
      intent: new FormControl(this.intent),
      name: new FormControl(this.name, [Validators.required]),
      sequenceId: new FormControl(this.sequenceId),
      start: new FormControl(this.start, [Validators.required]),
    });
  }

  onClickCancel() {
    this.router.navigate(['/hawk']);
  }

  onMenuClicked() {
    this.router.navigate(['/hawk']);
  }

  onSubmitActivityForm(value: RavenActivityDetail) {
    if (this.activityForm.valid) {
      this.store.dispatch(
        new SaveActivityDetail({
          ...value,
          id: this.id as string,
        }),
      );
    }
  }

  /**
   * Determine which select item to select
   * @see https://angular.io/api/forms/SelectControlValueAccessor#caveat-option-selection
   * @param a An object with an id property
   * @param b An object with an id property
   *
   * TODO: THIS IS DUPLICATED INSIDE hawk-app.component. CONSOLIDATE.
   */
  compareSelectValues(a: any, b: any) {
    return a.id === b.id;
  }
}
