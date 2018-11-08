/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';

import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';

import { ActivatedRoute, Router } from '@angular/router';
import { select, Store } from '@ngrx/store';

import { RavenActivityDetail, RavenActivityType } from '../../../shared/models';

import * as fromStore from '../../hawk-store';

import { Observable } from 'rxjs';
import { SaveActivityDetail } from '../../actions/plan.actions';
import { getActivityTypes } from '../../selectors/adaptation.selector';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-activities',
  styleUrls: ['./activities.component.css'],
  templateUrl: './activities.component.html',
})
export class ActivitiesComponent {
  activityTypes: Observable<RavenActivityType[] | null>;

  activityTypeId: string;

  /**
   * Duration in the format `hh:mm`
   */
  duration: string;

  /**
   * ID of the activity instance. Empty if this is new.
   */
  id: string | null;

  /**
   * Intent of the activity instance
   */
  intent: string;

  name: string;
  parameters: any;
  sequenceId: string;
  start: string;

  form: FormGroup;

  /**
   * Whether this activity type is new or pre-existing
   */
  get isNew() {
    return !this.id;
  }

  constructor(
    private store: Store<fromStore.HawkAppState>,
    private router: Router,
    private route: ActivatedRoute,
    fb: FormBuilder,
  ) {
    const activityDetail: RavenActivityDetail | null =
      this.route.snapshot.data.activityDetail || null;

    if (activityDetail) {
      this.activityTypeId = activityDetail.activityTypeId;
      this.duration = activityDetail.duration;
      this.id = activityDetail.id;
      this.intent = activityDetail.intent;
      this.name = activityDetail.name;
      this.parameters = activityDetail.parameters;
      this.sequenceId = activityDetail.sequenceId;
      this.start = activityDetail.start;
    }

    this.activityTypes = this.store.pipe(select(getActivityTypes));

    this.form = fb.group({
      activityTypeId: new FormControl({
        readonly: !this.isNew,
        value: this.activityTypeId,
      }),
      duration: new FormControl(this.duration, [
        Validators.required,
        Validators.pattern('^dd:dd$'),
      ]),
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

  onSubmit(value: any) {
    if (this.form.valid) {
      this.store.dispatch(
        new SaveActivityDetail({
          ...value,
          id: this.id,
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
