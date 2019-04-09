/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Actions, Effect } from '@ngrx/effects';
import { switchMap } from 'rxjs/operators';
import { mapToParam, mapToParams, ofRoute } from '../../../../libs/ngrx-router';
import {
  FetchActivityTypes,
  FetchAdaptations,
} from '../actions/adaptation.actions';
import { CloseAllDrawers } from '../actions/layout.actions';
import {
  ClearSelectedActivity,
  FetchActivities,
  FetchPlans,
  SelectActivity,
  SelectPlan,
} from '../actions/plan.actions';

@Injectable()
export class NavEffects {
  constructor(private actions$: Actions) {}

  @Effect()
  navPlans$ = this.actions$.pipe(
    ofRoute('plans'),
    switchMap(_ => [
      new CloseAllDrawers(),
      new FetchPlans(),
      new FetchAdaptations(),
    ]),
  );

  @Effect()
  navPlansWithId$ = this.actions$.pipe(
    ofRoute('plans/:planId'),
    mapToParam<string>('planId'),
    switchMap(planId => [
      new CloseAllDrawers(),
      new FetchPlans(),
      new FetchAdaptations(),
      new FetchActivityTypes(planId),
      new FetchActivities(planId, null),
      new SelectPlan(planId),
    ]),
  );

  @Effect()
  navActivities$ = this.actions$.pipe(
    ofRoute('plans/:planId/activity'),
    mapToParam<string>('planId'),
    switchMap(planId => [
      new CloseAllDrawers(),
      new FetchPlans(),
      new FetchAdaptations(),
      new FetchActivityTypes(planId),
      new SelectPlan(planId),
      new ClearSelectedActivity(),
    ]),
  );

  @Effect()
  navActivitiesWithId$ = this.actions$.pipe(
    ofRoute('plans/:planId/activity/:activityId'),
    mapToParams(),
    switchMap(({ activityId, planId }) => [
      new CloseAllDrawers(),
      new FetchPlans(),
      new FetchAdaptations(),
      new FetchActivityTypes(planId),
      new FetchActivities(planId, activityId),
      new SelectPlan(planId),
      new SelectActivity(activityId),
    ]),
  );
}
