/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, ViewChild } from '@angular/core';

import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';

import { MatSidenav } from '@angular/material';

import { select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';

import {
  RavenActivity,
  RavenActivityType,
  RavenAdaptation,
  RavenPlan,
  RavenPlanDetail,
} from '../../../shared/models';

import {
  FetchAdaptationList,
  OpenActivityTypeFormDialog,
  RemoveActivityType,
} from '../../actions/adaptation.actions';

import {
  FetchPlanDetail,
  FetchPlanList,
  OpenPlanFormDialog,
  RemovePlan,
  SaveActivity,
} from '../../actions/plan.actions';

import {
  getActivities,
  getActivityTypes,
  getPlans,
  getSelectedAdaptation,
  getSelectedPlan,
} from '../../selectors';

import { HawkAppState } from '../../hawk-store';

import { ToggleNavigationDrawer } from '../../../shared/actions/config.actions';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hawk-app',
  styleUrls: ['./hawk-app.component.css'],
  templateUrl: './hawk-app.component.html',
})
export class HawkAppComponent {
  @ViewChild('sideNav')
  sideNav: MatSidenav;

  /**
   * Activity *instances*
   */
  activities$: Observable<RavenActivity[] | null>;

  /**
   * List of activity types to display
   */
  activityTypes$: Observable<RavenActivityType[] | null>;

  /**
   * Columns to display in the table
   */
  displayedColumns: string[] = ['name', 'start', 'duration', 'sequenceId'];

  /**
   * Activity form
   */
  form: FormGroup;

  /**
   * The selected adaptation
   */
  selectedAdaptation$: Observable<RavenAdaptation | null>;

  /**
   * List of plans to display
   */
  plans$: Observable<RavenPlan[]>;

  /**
   * Selected plan
   */
  selectedPlan$: Observable<RavenPlanDetail | null>;

  /**
   * Activity that is currently selected in the plan
   */
  selectedActivity: RavenActivity | null;

  constructor(private store: Store<HawkAppState>, fb: FormBuilder) {
    this.activities$ = this.store.pipe(select(getActivities));
    this.activityTypes$ = this.store.pipe(select(getActivityTypes));
    this.plans$ = this.store.pipe(select(getPlans));
    this.selectedAdaptation$ = this.store.pipe(select(getSelectedAdaptation));
    this.selectedPlan$ = this.store.pipe(select(getSelectedPlan));

    let data: RavenActivity = {
      activityTypeId: '',
      duration: '',
      id: '',
      intent: '',
      name: '',
      sequenceId: '',
      start: '',
    };

    if (this.selectedActivity) {
      data = this.selectedActivity;
    }

    this.form = fb.group({
      activityTypeId: new FormControl({
        readonly: true,
        value: data.activityTypeId,
      }),
      duration: new FormControl(data.duration, [Validators.required]),
      intent: new FormControl(data.intent),
      name: new FormControl(data.name, [Validators.required]),
      sequenceId: new FormControl(data.sequenceId),
      start: new FormControl(data.start, [Validators.required]),
    });

    this.store.dispatch(new FetchAdaptationList());
    this.store.dispatch(new FetchPlanList());
  }

  /**
   * Event. Dispatch an action to display the Activity Type Form Dialog
   */
  showCreateActivityTypeForm() {
    this.store.dispatch(new OpenActivityTypeFormDialog(null));
  }

  /**
   * Event. Dispatch an action to display the Activity Type Form Dialog
   */
  showUpdateActivityTypeForm(id: string) {
    this.store.dispatch(new OpenActivityTypeFormDialog(id));
  }

  /**
   * Event. Dispatch an action to delete an Activity Type
   */
  deleteActivityType(id: string) {
    this.store.dispatch(new RemoveActivityType(id));
  }

  /**
   * Event. Dispatch an action to display the Plan Form Dialog
   */
  showCreatePlanForm() {
    this.store.dispatch(new OpenPlanFormDialog(null));
  }

  /**
   * Event. Dispatch an action to display the Plan Form Dialog
   */
  showUpdatePlanForm(id: string) {
    this.store.dispatch(new OpenPlanFormDialog(id));
  }

  /**
   * Event. Dispatch an action to delete a Plan
   */
  deletePlan(id: string) {
    this.store.dispatch(new RemovePlan(id));
  }

  /**
   * Event. Dispatch an action to select a Plan
   */
  selectPlan(id: string) {
    this.store.dispatch(new FetchPlanDetail(id));
  }

  /**
   * Event. Set the selected activity and update the form's values
   */
  showActivityForm(activity: RavenActivity) {
    this.selectedActivity = activity;
    this.form.patchValue(this.selectedActivity);
    this.sideNav.open();
  }

  /**
   * Event. Show the activity types list. The selectedActivity is set to null
   * because you can click the icon to show the activity types list while the
   * activity form is open.
   */
  showActivityTypesList() {
    this.selectedActivity = null;
    this.sideNav.open();
  }

  /**
   * Event. Handle form submission
   */
  onSubmit(value: any) {
    if (!this.selectedActivity) {
      throw new Error('NoSelectedActivity');
    }

    if (this.form.valid) {
      this.store.dispatch(
        new SaveActivity({
          ...value,
          id: this.selectedActivity.id,
        }),
      );
    }
  }

  /**
   * Event. The hamburger menu was clicked
   */
  onMenuClicked() {
    this.store.dispatch(new ToggleNavigationDrawer());
  }

  /**
   * Event. Set the selected activity to null and close the side nav
   */
  onCloseSideNav() {
    this.selectedActivity = null;
    this.sideNav.close();
  }

  /**
   * Determine which select item to select
   * @see https://angular.io/api/forms/SelectControlValueAccessor#caveat-option-selection
   * @param a An object with an id property
   * @param b An object with an id property
   */
  compareSelectValues(a: any, b: any) {
    return a.id === b.id;
  }
}
