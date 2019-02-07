/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { MatSidenav } from '@angular/material';
import { ActivatedRoute, Data, NavigationEnd, Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { ToggleNavigationDrawer } from '../../../shared/actions/config.actions';
import { timestamp } from '../../../shared/util';
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
  RavenPlan,
  RavenPlanDetail,
  RavenTimeRange,
} from '../../../shared/models';

import {
  FetchActivityTypes,
  FetchAdaptationListFailure,
  FetchAdaptationListSuccess,
} from '../../actions/adaptation.actions';

import {
  ClearSelectedActivity,
  ClearSelectedPlan,
  FetchPlanDetailSuccess,
  FetchPlanListFailure,
  FetchPlanListSuccess,
  OpenPlanFormDialog,
  SelectActivity,
  UpdateActivity,
  UpdateViewTimeRange,
} from '../../actions/plan.actions';

import {
  getActivityInstances,
  getActivityTypes,
  getMaxTimeRange,
  getPlans,
  getSelectedActivity,
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

  activityInstances$: Observable<RavenActivity[] | null>;
  activityTypes$: Observable<RavenActivityType[]>;
  maxTimeRange$: Observable<RavenTimeRange>;
  plans$: Observable<RavenPlan[]>;
  selectedActivity$: Observable<RavenActivity | null>;
  selectedPlan$: Observable<RavenPlanDetail | null>;
  viewTimeRange$: Observable<RavenTimeRange>;

  activityInstances: RavenActivity[] | null;
  displayedColumns: string[] = ['name', 'start', 'end', 'duration'];
  form: FormGroup;
  selectedActivity: RavenActivity | null = null;
  selectedPlan: RavenPlan | null = null;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    public fb: FormBuilder,
    private store: Store<HawkAppState>,
    private router: Router,
    private route: ActivatedRoute,
  ) {
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(() => {
        this.resolveActions(this.route.snapshot.data);
      });

    this.activityInstances$ = this.store.pipe(select(getActivityInstances));
    this.activityTypes$ = this.store.pipe(select(getActivityTypes));
    this.maxTimeRange$ = this.store.pipe(select(getMaxTimeRange));
    this.plans$ = this.store.pipe(select(getPlans));
    this.selectedActivity$ = this.store.pipe(select(getSelectedActivity));
    this.selectedPlan$ = this.store.pipe(select(getSelectedPlan));
    this.viewTimeRange$ = this.store.pipe(select(getViewTimeRange));

    this.form = fb.group({
      activityType: new FormControl(''),
      duration: new FormControl(0, [Validators.required]),
      intent: new FormControl(''),
      name: new FormControl('', [Validators.required]),
      start: new FormControl(0, [Validators.required]),
    });

    this.activityInstances$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((instances: RavenActivity[] | null) => {
        this.activityInstances = instances;
      });

    this.selectedActivity$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((selectedActivity: RavenActivity | null) => {
        this.selectedActivity = selectedActivity;
        this.patchActivityForm();
      });

    this.selectedPlan$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((selPlan: RavenPlan | null) => {
        this.selectedPlan = selPlan;
      });
  }

  ngOnDestroy(): void {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Emit actions based on router snapshot data from a resolver.
   * This is so we can easily sync URL state,
   * HTTP responses (i.e. data) from a resolver, and the store.
   */
  resolveActions(data: Data): void {
    const { adaptations, plans, selectedActivity, selectedPlan } = data;

    if (adaptations) {
      this.store.dispatch(new FetchAdaptationListSuccess(adaptations));
    } else {
      this.store.dispatch(
        new FetchAdaptationListFailure(
          new Error('Failed to fetch adaptations list.'),
        ),
      );
    }

    if (plans) {
      this.store.dispatch(new FetchPlanListSuccess(plans));
    } else {
      this.store.dispatch(
        new FetchPlanListFailure(new Error('Failed to fetch plan list.')),
      );
    }

    if (selectedPlan) {
      this.store.dispatch(new FetchPlanDetailSuccess(selectedPlan));
      this.store.dispatch(new FetchActivityTypes(selectedPlan.adaptationId));
    } else {
      this.store.dispatch(new ClearSelectedPlan());
    }

    if (selectedActivity) {
      this.store.dispatch(new SelectActivity(selectedActivity.id));
    }
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
   * Event. Dispatch an action to select a Plan
   */
  selectPlan(planId: string) {
    this.store.dispatch(new ClearSelectedActivity());
    this.router.navigateByUrl(`/plans/${planId}`);
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
    this.store.dispatch(new SelectActivity(activity.activityId));
    this.sideNav.open();
    setTimeout(() => dispatchEvent(new Event('resize')), 500);
  }

  /**
   * Called when the user clicks the `+ Activity` button to show the activity instance page.
   */
  onClickNewActivity() {
    if (this.selectedPlan) {
      const planId = this.selectedPlan.id;
      this.router.navigateByUrl(`/plans/${planId}/activities`);
    }
  }

  /**
   * Called when the user clicks the `Advanced` button to show the activity instance page.
   */
  onShowActivityInstancePage() {
    if (this.selectedPlan && this.selectedActivity) {
      const planId = this.selectedPlan.id;
      const activityId = this.selectedActivity.activityId;
      this.router.navigateByUrl(`/plans/${planId}/activities/${activityId}`);
    }
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
    if (this.form.valid && this.selectedActivity) {
      this.store.dispatch(
        new UpdateActivity(this.selectedActivity.activityId, { ...value }),
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
