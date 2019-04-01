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
import { ActivityInstance, ActivityType } from '../../../shared/models';
import { PlanningAppState } from '../../planning-store';
import { getSelectedActivity } from '../../selectors';
import { getActivityTypes } from '../../selectors';
import { PlanningService } from '../../services/planning.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'activity-app',
  styleUrls: ['./activity.component.css'],
  templateUrl: './activity.component.html',
})
export class ActivityComponent {
  activityTypes$: Observable<ActivityType[] | null>;
  selectedActivity$: Observable<ActivityInstance | null>;

  title: string;
  isNew = false;

  constructor(
    private store: Store<PlanningAppState>,
    private router: Router,
    private route: ActivatedRoute,
    public planningService: PlanningService,
  ) {
    this.activityTypes$ = this.store.pipe(select(getActivityTypes));
    this.selectedActivity$ = this.store.pipe(select(getSelectedActivity));

    // If the route params has an activityId then we can assume we have a selected activity.
    const { activityId } = this.route.snapshot.paramMap['params'];

    if (activityId) {
      this.title = 'Edit Activity Instance';
      this.isNew = false;
    } else {
      this.title = 'Add New Activity Instance';
      this.isNew = true;
    }
  }

  onNavBack() {
    const { planId } = this.route.snapshot.paramMap['params'];
    this.router.navigate([`/plans/${planId}`]);
  }
}
