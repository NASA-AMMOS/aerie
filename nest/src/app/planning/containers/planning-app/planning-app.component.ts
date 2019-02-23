/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  Activity,
  ActivityType,
  Plan,
} from '../../../../../../schemas/types/ts';
import { ToggleNavigationDrawer } from '../../../shared/actions/config.actions';
import { RavenActivityUpdate, RavenTimeRange } from '../../../shared/models';
import { NgTemplateUtils, timestamp } from '../../../shared/util';
import {
  ToggleActivityTypesDrawer,
  ToggleEditActivityDrawer,
} from '../../actions/layout.actions';
import {
  ClearSelectedActivity,
  OpenPlanFormDialog,
  SelectActivity,
  UpdateActivity,
  UpdateViewTimeRange,
} from '../../actions/plan.actions';
import { PlanningAppState } from '../../planning-store';
import {
  getActivities,
  getActivityTypes,
  getMaxTimeRange,
  getPlans,
  getSelectedActivity,
  getSelectedPlan,
  getShowActivityTypesDrawer,
  getShowEditActivityDrawer,
  getShowLoadingBar,
  getViewTimeRange,
} from '../../selectors';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'planning-app',
  styleUrls: ['./planning-app.component.css'],
  templateUrl: './planning-app.component.html',
})
export class PlanningAppComponent implements OnDestroy {
  // Layout state.
  showActivityTypesDrawer$: Observable<boolean>;
  showEditActivityDrawer$: Observable<boolean>;
  showLoadingBar$: Observable<boolean>;

  // Plan state.
  activities$: Observable<Activity[] | null>;
  activityTypes$: Observable<ActivityType[]>;
  maxTimeRange$: Observable<RavenTimeRange>;
  plans$: Observable<Plan[]>;
  selectedActivity$: Observable<Activity | null>;
  selectedPlan$: Observable<Plan | null>;
  viewTimeRange$: Observable<RavenTimeRange>;

  // Local derived state.
  activities: Activity[] | null;
  displayedColumns: string[] = ['name', 'start', 'end', 'duration'];
  editActivityForm: FormGroup;
  selectedActivity: Activity | null = null;
  selectedPlan: Plan | null = null;

  // Helpers.
  ngTemplateUtils: NgTemplateUtils = new NgTemplateUtils();

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    public fb: FormBuilder,
    private store: Store<PlanningAppState>,
    private router: Router,
  ) {
    // Layout state.
    this.showActivityTypesDrawer$ = this.store.pipe(
      select(getShowActivityTypesDrawer),
    );
    this.showEditActivityDrawer$ = this.store.pipe(
      select(getShowEditActivityDrawer),
    );
    this.showLoadingBar$ = this.store.pipe(select(getShowLoadingBar));

    // Plan state.
    this.activities$ = this.store.pipe(select(getActivities));
    this.activityTypes$ = this.store.pipe(select(getActivityTypes));
    this.maxTimeRange$ = this.store.pipe(select(getMaxTimeRange));
    this.plans$ = this.store.pipe(select(getPlans));
    this.selectedActivity$ = this.store.pipe(select(getSelectedActivity));
    this.selectedPlan$ = this.store.pipe(select(getSelectedPlan));
    this.viewTimeRange$ = this.store.pipe(select(getViewTimeRange));

    this.editActivityForm = fb.group({
      activityType: new FormControl(''),
      duration: new FormControl(0, [Validators.required]),
      intent: new FormControl(''),
      name: new FormControl('', [Validators.required]),
      start: new FormControl(0, [Validators.required]),
    });

    this.activities$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((activities: Activity[] | null) => {
        this.activities = activities;
      });

    this.selectedActivity$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((selectedActivity: Activity | null) => {
        this.selectedActivity = selectedActivity;
        this.patchActivityForm();
      });

    this.selectedPlan$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((selectedPlan: Plan | null) => {
        this.selectedPlan = selectedPlan;
      });
  }

  ngOnDestroy(): void {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Patch activity form with the selected activity.
   */
  patchActivityForm() {
    if (this.selectedActivity) {
      this.editActivityForm.patchValue(this.selectedActivity);
    }
  }

  /**
   * Event. Called when the user clicks the `+ Activity` button to show the activity page.
   */
  onClickNewActivity() {
    if (this.selectedPlan) {
      const planId = this.selectedPlan.id;
      this.router.navigateByUrl(`/plans/${planId}/activities`);
    }
  }

  /**
   * Event. Set the selected activity and show the edit activity drawer.
   */
  onSelectActivity(id: string) {
    this.store.dispatch(new SelectActivity(id));
    this.store.dispatch(new ToggleEditActivityDrawer(true));
  }

  /**
   * Event. Dispatch an action to select a Plan
   */
  onSelectPlan(planId: string) {
    this.store.dispatch(new ClearSelectedActivity());
    this.router.navigateByUrl(`/plans/${planId}`);
  }

  /**
   * Event. Called when the user clicks the `Advanced` button to show the activity page.
   */
  onShowActivityPage() {
    if (this.selectedPlan && this.selectedActivity) {
      const planId = this.selectedPlan.id;
      const activityId = this.selectedActivity.activityId;
      this.router.navigateByUrl(`/plans/${planId}/activities/${activityId}`);
    }
  }

  /**
   * Event. Dispatch an action to display the Plan Form Dialog
   */
  onShowCreatePlanForm() {
    this.store.dispatch(new OpenPlanFormDialog(null));
  }

  /**
   * Event. Handle edit activity form submission.
   */
  onSubmit(value: any) {
    if (this.editActivityForm.valid && this.selectedActivity) {
      this.store.dispatch(
        new UpdateActivity(this.selectedActivity.activityId, { ...value }),
      );
    }
  }

  /**
   * Event. Called when a close event is fired from the activity types drawer.
   */
  onCloseActivityTypesDrawer() {
    this.store.dispatch(new ToggleActivityTypesDrawer(false));
  }

  /**
   * Event. Called when an open event is fired from the activity types drawer.
   */
  onOpenActivityTypesDrawer() {
    this.store.dispatch(new ToggleActivityTypesDrawer(true));
  }

  /**
   * Event. Called when a close event is fired from the edit activity drawer.
   */
  onCloseEditActivityDrawer() {
    this.store.dispatch(new ToggleEditActivityDrawer(false));
  }

  /**
   * Event. Called when an open event is fired from the edit activity drawer.
   */
  onOpenEditActivityDrawer() {
    this.store.dispatch(new ToggleEditActivityDrawer(true));
  }

  /**
   * Event. Called when we want to show the Nest navigation drawer.
   */
  onToggleNavigationDrawer() {
    this.store.dispatch(new ToggleNavigationDrawer());
  }

  /**
   * Event. Called when we need to update the selected activity.
   */
  onUpdateSelectedActivity(update: RavenActivityUpdate): void {
    if (this.selectedActivity) {
      this.store.dispatch(
        new UpdateActivity(this.selectedActivity.activityId, {
          end: update.end,
          endTimestamp: timestamp(update.end),
          start: update.start,
          startTimestamp: timestamp(update.start),
          y: update.y,
        }),
      );
    }
  }

  /**
   * Event. Called when we need to update our view time range for a selected plan.
   */
  onUpdateViewTimeRange(viewTimeRange: RavenTimeRange): void {
    this.store.dispatch(new UpdateViewTimeRange(viewTimeRange));
  }
}
