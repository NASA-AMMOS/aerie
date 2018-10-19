/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { Observable, of, zip } from 'rxjs';
import {
  catchError,
  concatMap,
  exhaustMap,
  map,
  withLatestFrom,
} from 'rxjs/operators';

import { RavenPlanFormDialogComponent } from '../../shared/components/components';

import {
  FetchPlanList,
  FetchPlanListFailure,
  FetchPlanListSuccess,
  OpenPlanFormDialog,
  PlanActionTypes,
  RemovePlan,
  RemovePlanSuccess,
  SavePlanSuccess,
} from '../actions/plan.actions';

import { RavenPlan } from '../../shared/models/raven-plan';
// TODO: Replace with a real service once an external service for plans becomes available
import { PlanMockService } from '../../shared/services/plan-mock.service';
import { HawkAppState } from '../hawk-store';

@Injectable()
export class PlanEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<HawkAppState>,
    private planService: PlanMockService,
    private dialog: MatDialog,
  ) {}

  @Effect()
  fetchPlanList$: Observable<Action> = this.actions$.pipe(
    ofType<FetchPlanList>(PlanActionTypes.FetchPlanList),
    concatMap(action => {
      return this.planService.getPlans().pipe(
        map(data => new FetchPlanListSuccess(data)),
        catchError((e: Error) => {
          console.error('PlanEffects - fetchPlanList$: ', e);
          return of(new FetchPlanListFailure(e));
        }),
      );
    }),
  );

  @Effect()
  removePlan$: Observable<Action> = this.actions$.pipe(
    ofType<RemovePlan>(PlanActionTypes.RemovePlan),
    concatMap(action =>
      this.planService
        .removePlan(action.id)
        .pipe(map(data => new RemovePlanSuccess(action.id))),
    ),
  );

  @Effect()
  openUpdatePlanFormDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenPlanFormDialog>(PlanActionTypes.OpenPlanFormDialog),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    exhaustMap(({ action, state }) => {
      const data: RavenPlan | null =
        state.hawk.plan.plans.find(a => a.id === action.id) || null;

      const componentDialog = this.dialog.open(RavenPlanFormDialogComponent, {
        data: data || {},
      });

      return zip(of(action), componentDialog.afterClosed());
    }),
    map(([action, result]) => ({ action, result })),
    exhaustMap(({ action, result }) => {
      if (result) {
        return of(
          // TODO: Dispatch SavePlan once we have a backend for it
          new SavePlanSuccess(result, !action.id),
        );
      }
      return [];
    }),
  );
}
