/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
} from '@angular/core';

import { select, Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { RavenActivityType } from '../../../shared/models/raven-activity-type';
import {
  FetchActivityTypeList,
  OpenActivityTypeFormDialog,
  RemoveActivityType,
} from '../../actions/activity-type.actions';
import { HawkAppState } from '../../hawk-store';

import * as fromActivityType from '../../reducers/activity-type.reducer';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hawk-app',
  styleUrls: ['./hawk-app.component.css'],
  templateUrl: './hawk-app.component.html',
})
export class HawkAppComponent implements OnDestroy {
  /**
   * List of activity types to display
   */
  activityTypes: RavenActivityType[] = [];

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<HawkAppState>,
  ) {
    this.store
      .pipe(
        select(fromActivityType.getActivityTypes),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(activityTypes => {
        this.activityTypes = activityTypes;
        this.markForCheck();
      });

    // TODO: Move to a route guard
    this.store.dispatch(new FetchActivityTypeList());
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Helper. Marks this component for change detection check,
   * and then detects changes on the next tick.
   *
   * @todo Find out how we can remove this.
   */
  markForCheck() {
    this.changeDetector.markForCheck();
    setTimeout(() => {
      if (!this.changeDetector['destroyed']) {
        this.changeDetector.detectChanges();
      }
    });
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
}
