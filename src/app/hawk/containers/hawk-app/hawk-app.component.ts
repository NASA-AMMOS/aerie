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
import { RavenAdaptation } from '../../../shared/models/raven-adaptation';
import { RavenPlan } from '../../../shared/models/raven-plan';

import * as configActions from '../../../shared/actions/config.actions';

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
} from '../../actions/plan.actions';

import * as fromConfig from '../../../shared/reducers/config.reducer';
import * as fromAdaptation from '../../reducers/adaptation.reducer';
import * as fromPlan from '../../reducers/plan.reducer';

import { HawkAppState } from '../../hawk-store';

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

  /**
   * List of adaptations to display
   */
  adaptations: RavenAdaptation[] = [];

  /**
   * The selected adaptation
   */
  selectedAdaptation: RavenAdaptation | null = null;

  /**
   * List of plans to display
   */
  plans: RavenPlan[] = [];

  /**
   * Selected plan
   */
  selectedPlan: RavenPlan | null = null;

  /**
   * Current state of the navigation drawer
   */
  navigationDrawerState: configActions.NavigationDrawerStates;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<HawkAppState>,
  ) {
    this.store
      .pipe(
        select(fromAdaptation.getAdaptations),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(adaptations => {
        this.adaptations = adaptations;
        this.markForCheck();
      });

    this.store
      .pipe(
        select(fromAdaptation.getSelectedAdaptation),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(selectedAdaptation => {
        this.selectedAdaptation = selectedAdaptation;
        if (selectedAdaptation) {
          this.activityTypes = Object.values(selectedAdaptation.activityTypes);
        }
        this.markForCheck();
      });

    this.store
      .pipe(
        select(fromPlan.getPlans),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(plans => {
        this.plans = plans;
        this.markForCheck();
      });

    this.store
      .pipe(
        select(fromPlan.getSelectedPlan),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(plan => {
        this.selectedPlan = plan || null;
        this.markForCheck();
      });

    // Navigation drawer state
    this.store
      .pipe(
        select(fromConfig.getNavigationDrawerState),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(state => {
        this.navigationDrawerState = state;
        this.markForCheck();
      });

    // TODO: Move to a route guard
    this.store.dispatch(new FetchAdaptationList());
    this.store.dispatch(new FetchPlanList());
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
   * The hamburger menu was clicked
   */
  onMenuClicked() {
    this.store.dispatch(new configActions.ToggleNavigationDrawer());
  }
}
