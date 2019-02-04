/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { MatSidenav } from '@angular/material';
import { select, Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ToggleNavigationDrawer } from '../../../shared/actions/config.actions';
import { HawkAppState } from '../../hawk-store';

import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  ViewChild,
} from '@angular/core';

import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';

import {
  RavenActivity,
  RavenActivityType,
  RavenActivityUpdate,
  RavenAdaptation,
  RavenPlan,
  RavenPlanDetail,
  RavenTimeRange,
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
  SelectActivity,
  UpdateSelectedActivity,
  UpdateViewTimeRange,
} from '../../actions/plan.actions';

import {
  getActivities,
  getActivityTypes,
  getMaxTimeRange,
  getPlans,
  getSelectedActivity,
  getSelectedAdaptation,
  getSelectedPlan,
  getViewTimeRange,
} from '../../selectors';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hawk-app',
  styleUrls: ['./hawk-app.component.css'],
  templateUrl: './hawk-app.component.html',
})
export class HawkAppComponent implements OnDestroy {
  @ViewChild('sideNav')
  sideNav: MatSidenav;

  activities$: Observable<RavenActivity[] | null>; // Instances.
  activityTypes$: Observable<RavenActivityType[] | null>;
  displayedColumns: string[] = ['id', 'name', 'start', 'end', 'duration'];
  form: FormGroup;
  maxTimeRange$: Observable<RavenTimeRange>;
  plans$: Observable<RavenPlan[]>;
  selectedActivity$: Observable<RavenActivity | null>;
  selectedAdaptation$: Observable<RavenAdaptation | null>;
  selectedPlan$: Observable<RavenPlanDetail | null>;
  viewTimeRange$: Observable<RavenTimeRange>;

  selectedActivity: RavenActivity | null = null;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(private store: Store<HawkAppState>, fb: FormBuilder) {
    this.activities$ = this.store.pipe(select(getActivities));
    this.activityTypes$ = this.store.pipe(select(getActivityTypes));
    this.maxTimeRange$ = this.store.pipe(select(getMaxTimeRange));
    this.plans$ = this.store.pipe(select(getPlans));
    this.selectedActivity$ = this.store.pipe(select(getSelectedActivity));
    this.selectedAdaptation$ = this.store.pipe(select(getSelectedAdaptation));
    this.selectedPlan$ = this.store.pipe(select(getSelectedPlan));
    this.viewTimeRange$ = this.store.pipe(select(getViewTimeRange));

    this.form = fb.group({
      duration: new FormControl(0, [Validators.required]),
      id: new FormControl({
        readonly: true,
        value: '',
      }),
      intent: new FormControl(''),
      name: new FormControl('', [Validators.required]),
      sequenceId: new FormControl(''),
      startTimestamp: new FormControl(0, [Validators.required]),
    });

    this.selectedActivity$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((selectedActivity: RavenActivity | null) => {
        this.selectedActivity = selectedActivity;
        this.patchActivityForm();
      });

    this.store.dispatch(new FetchAdaptationList());
    this.store.dispatch(new FetchPlanList());
  }

  ngOnDestroy(): void {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
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
   * Patch activity form with the selected activity.
   */
  patchActivityForm() {
    if (this.selectedActivity) {
      this.form.patchValue(this.selectedActivity);
    }
  }

  /**
   * Event. Set the selected activity and update the form's values
   *
   * TODO (!): Move side nav management to a reducer.
   * This will enable us to control the UI from actions (e.g. OpenSelectedActivityDrawer and Resize).
   */
  showActivityForm(activity: RavenActivity) {
    this.store.dispatch(new SelectActivity(activity.id));
    this.sideNav.open();
    setTimeout(() => dispatchEvent(new Event('resize')), 500);
  }

  /**
   * Event. Show the activity types list. The selectedActivity is set to null
   * because you can click the icon to show the activity types list while the
   * activity form is open.
   *
   * TODO (!): Move side nav management to a reducer.
   * This will enable us to control the UI from actions (e.g. OpenSelectedActivityDrawer and Resize).
   *
   */
  showActivityTypesList() {
    this.store.dispatch(new SelectActivity(null));
    this.sideNav.open();
    setTimeout(() => dispatchEvent(new Event('resize')), 500);
  }

  /**
   * Event. Handle form submission
   */
  onSubmit(value: any) {
    if (this.form.valid) {
      this.store.dispatch(new SaveActivity(value));
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
   *
   * TODO (!): Move side nav management to a reducer.
   * This will enable us to control the UI from actions (e.g. OpenSelectedActivityDrawer and Resize).
   */
  onCloseSideNav() {
    this.store.dispatch(new SelectActivity(null));
    this.sideNav.close();
    setTimeout(() => dispatchEvent(new Event('resize')), 500);
  }

  /**
   * Event. Dispatch to set the selected activity.
   */
  onSelectActivity(id: string): void {
    this.store.dispatch(new SelectActivity(id));
  }

  /**
   * Event. Called when we need to update the selected activity (e.g. after a drag).
   */
  onUpdateSelectedActivity(update: RavenActivityUpdate): void {
    this.store.dispatch(new UpdateSelectedActivity(update));
  }

  /**
   * Event. Called when we need to update our view time range for a selected plan.
   */
  onUpdateViewTimeRange(viewTimeRange: RavenTimeRange): void {
    this.store.dispatch(new UpdateViewTimeRange(viewTimeRange));
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
