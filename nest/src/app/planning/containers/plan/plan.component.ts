/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { NavigateByUrl } from '../../../../../libs/ngrx-router';
import { ActivityInstance, ActivityType, Plan } from '../../../shared/models';
import {
  ToggleActivityTypesDrawer,
  ToggleAddActivityDrawer,
  ToggleEditActivityDrawer,
} from '../../actions/layout.actions';
import {
  CreateActivity,
  DeleteActivity,
  SelectActivity,
  UpdateActivity,
} from '../../actions/plan.actions';
import { PlanningAppState } from '../../planning-store';
import {
  getActivities,
  getActivityTypes,
  getSelectedActivity,
  getSelectedPlan,
  getShowActivityTypesDrawer,
  getShowAddActivityDrawer,
  getShowEditActivityDrawer,
} from '../../selectors';
import { PlanningService } from '../../services/planning.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'plan-app',
  styleUrls: ['./plan.component.css'],
  templateUrl: './plan.component.html',
})
export class PlanComponent {
  activities$: Observable<ActivityInstance[] | null>;
  activityTypes$: Observable<ActivityType[]>;
  selectedActivity$: Observable<ActivityInstance | null>;
  selectedPlan$: Observable<Plan | null>;
  showActivityTypesDrawer$: Observable<boolean>;
  showAddActivityDrawer$: Observable<boolean>;
  showEditActivityDrawer$: Observable<boolean>;

  constructor(
    private store: Store<PlanningAppState>,
    private route: ActivatedRoute,
    public planningService: PlanningService,
  ) {
    this.activities$ = this.store.pipe(select(getActivities));
    this.activityTypes$ = this.store.pipe(select(getActivityTypes));
    this.selectedActivity$ = this.store.pipe(select(getSelectedActivity));
    this.selectedPlan$ = this.store.pipe(select(getSelectedPlan));
    this.showActivityTypesDrawer$ = this.store.pipe(
      select(getShowActivityTypesDrawer),
    );
    this.showAddActivityDrawer$ = this.store.pipe(
      select(getShowAddActivityDrawer),
    );
    this.showEditActivityDrawer$ = this.store.pipe(
      select(getShowEditActivityDrawer),
    );
  }

  /**
   * Event. Called when the user clicks the 'Advanced' button in the New Activity form.
   */
  navigateToNewActivityPage() {
    const { planId } = this.route.snapshot.paramMap['params'];
    this.store.dispatch(new NavigateByUrl(`/plans/${planId}/activity`));
  }

  /**
   * Event. Called when the user clicks the 'Advanced' button in the Edit Activity form.
   */
  navigateToEditActivityPage(activityId: string): void {
    const { planId } = this.route.snapshot.paramMap['params'];
    this.store.dispatch(
      new NavigateByUrl(`/plans/${planId}/activity/${activityId}`),
    );
  }

  /**
   * Event. Called when we want to add a new activity.
   */
  onCreateActivity(activity: ActivityInstance): void {
    const { planId } = this.route.snapshot.paramMap['params'];
    this.store.dispatch(new CreateActivity(planId, activity));
  }

  /**
   * Event. Called when the user clicks to delete an activity.
   */
  onDeleteActivity(activityId: string): void {
    const { planId } = this.route.snapshot.paramMap['params'];
    this.store.dispatch(new DeleteActivity(planId, activityId));
  }

  /**
   * Event. Called when we want to nav back to the plans container.
   */
  onNavBack() {
    this.store.dispatch(new NavigateByUrl(`/plans`));
  }

  /**
   * Event. Set the selected activity and show the edit activity drawer.
   */
  onSelectActivity(id: string): void {
    this.store.dispatch(new SelectActivity(id));
    this.store.dispatch(new ToggleEditActivityDrawer(true));
  }

  /**
   * Event. Called when a toggle event is fired from the activity types drawer.
   */
  onToggleActivityTypesDrawer(opened?: boolean): void {
    this.store.dispatch(new ToggleActivityTypesDrawer(opened));
  }

  /**
   * Event. Called when a toggle event is fired from the add activity drawer.
   */
  onToggleAddActivityDrawer(opened?: boolean): void {
    this.store.dispatch(new ToggleAddActivityDrawer(opened));
  }

  /**
   * Event. Called when a toggle event is fired from the edit activity drawer.
   */
  onToggleEditActivityDrawer(opened?: boolean): void {
    this.store.dispatch(new ToggleEditActivityDrawer(opened));
  }

  /**
   * Event. Called when we need to update an activity instance.
   */
  onUpdateActivity(activity: ActivityInstance): void {
    const { planId } = this.route.snapshot.paramMap['params'];
    this.store.dispatch(
      new UpdateActivity(planId, activity.activityId, activity),
    );
  }
}
