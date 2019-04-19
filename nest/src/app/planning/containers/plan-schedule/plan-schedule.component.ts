/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ActivityInstance, TimeRange } from '../../../shared/models';
import { timestamp } from '../../../shared/util';
import {
  SelectActivity,
  UpdateActivity,
  UpdateViewTimeRange,
} from '../../actions/plan.actions';
import { ActivityInstanceUpdate } from '../../models';
import { PlanningAppState } from '../../planning-store';
import {
  getActivitiesAsList,
  getMaxTimeRange,
  getSelectedActivity,
  getViewTimeRange,
} from '../../selectors';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'plan-schedule',
  styles: [],
  templateUrl: './plan-schedule.component.html',
})
export class PlanScheduleComponent implements OnDestroy {
  activities$: Observable<ActivityInstance[] | null>;
  maxTimeRange$: Observable<TimeRange>;
  selectedActivity$: Observable<ActivityInstance | null>;
  viewTimeRange$: Observable<TimeRange>;

  selectedActivity: ActivityInstance | null = null;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private store: Store<PlanningAppState>,
    private route: ActivatedRoute,
  ) {
    this.activities$ = this.store.pipe(select(getActivitiesAsList));
    this.maxTimeRange$ = this.store.pipe(select(getMaxTimeRange));
    this.selectedActivity$ = this.store.pipe(select(getSelectedActivity));
    this.viewTimeRange$ = this.store.pipe(select(getViewTimeRange));

    this.selectedActivity$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((selectedActivity: ActivityInstance | null) => {
        this.selectedActivity = selectedActivity;
      });
  }

  ngOnDestroy(): void {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  onSelectActivity(id: string): void {
    this.store.dispatch(new SelectActivity(id));
  }

  onUpdateSelectedActivity(update: ActivityInstanceUpdate): void {
    if (this.selectedActivity) {
      const { planId } = this.route.snapshot.paramMap['params'];
      this.store.dispatch(
        new UpdateActivity(planId, this.selectedActivity.activityId, {
          duration: update.duration,
          end: update.end,
          endTimestamp: timestamp(update.end),
          start: update.start,
          startTimestamp: timestamp(update.start),
          y: update.y,
        }),
      );
    }
  }

  onUpdateViewTimeRange(viewTimeRange: TimeRange): void {
    this.store.dispatch(new UpdateViewTimeRange(viewTimeRange));
  }
}
