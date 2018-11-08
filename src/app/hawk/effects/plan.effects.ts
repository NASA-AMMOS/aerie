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
import { Router } from '@angular/router';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { cloneDeep } from 'lodash';
import { merge, Observable, of, zip } from 'rxjs';
import {
  catchError,
  concatMap,
  exhaustMap,
  map,
  switchMap,
  withLatestFrom,
} from 'rxjs/operators';

import { RavenPlanFormDialogComponent } from '../../shared/components/components';
import { RavenPlanFormDialogData } from '../../shared/models/raven-plan-form-dialog-data';

import {
  FetchPlanDetail,
  FetchPlanDetailFailure,
  FetchPlanDetailSuccess,
  FetchPlanList,
  FetchPlanListFailure,
  FetchPlanListSuccess,
  OpenPlanFormDialog,
  PlanActionTypes,
  RemovePlan,
  RemovePlanSuccess,
  SaveActivity,
  SaveActivityDetail,
  SaveActivityDetailFailure,
  SaveActivityDetailSuccess,
  SaveActivityFailure,
  SaveActivitySuccess,
  SavePlanSuccess,
} from '../actions/plan.actions';

import {
  FetchAdaptationFailure,
  FetchAdaptationSuccess,
} from '../actions/adaptation.actions';

// TODO: Replace with a real service once an external service for plans becomes available
import { AdaptationMockService } from '../../shared/services/adaptation-mock.service';
import { PlanMockService } from '../../shared/services/plan-mock.service';
import { HawkAppState } from '../hawk-store';

@Injectable()
export class PlanEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<HawkAppState>,
    private planService: PlanMockService,
    private adaptationService: AdaptationMockService,
    private dialog: MatDialog,
    private router: Router,
  ) {}

  @Effect()
  fetchPlanList$: Observable<Action> = this.actions$.pipe(
    ofType<FetchPlanList>(PlanActionTypes.FetchPlanList),
    concatMap(_ => {
      return this.planService.getPlans().pipe(
        map(data => new FetchPlanListSuccess(data)),
        catchError((e: Error) => {
          console.error('PlanEffects - fetchPlanList$: ', e);
          return of(new FetchPlanListFailure(e));
        }),
      );
    }),
  );

  /**
   * Any time a new plan is requested, fetch the plan _and_ the adaptation.
   */
  @Effect()
  fetchPlanDetail$: Observable<Action> = this.actions$.pipe(
    ofType<FetchPlanDetail>(PlanActionTypes.FetchPlanDetail),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state }) => {
      const plan = state.hawk.plan.plans[action.id];

      if (!plan) {
        const err = new Error('UndefinedPlan');
        return of(new FetchPlanDetailFailure(err));
      }

      return merge(
        this.planService.getPlanDetail(plan.adaptationId, action.id).pipe(
          map(data => new FetchPlanDetailSuccess(data)),
          catchError((err: Error) => of(new FetchPlanDetailFailure(err))),
        ),
        this.adaptationService.getAdaptation(plan.adaptationId).pipe(
          map(data => new FetchAdaptationSuccess(data)),
          catchError((err: Error) => of(new FetchAdaptationFailure(err))),
        ),
      );
    }),
  );

  @Effect()
  removePlan$: Observable<Action> = this.actions$.pipe(
    ofType<RemovePlan>(PlanActionTypes.RemovePlan),
    concatMap(action =>
      this.planService
        .removePlan(action.id)
        .pipe(map(_ => new RemovePlanSuccess(action.id))),
    ),
  );

  @Effect()
  openUpdatePlanFormDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenPlanFormDialog>(PlanActionTypes.OpenPlanFormDialog),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    exhaustMap(({ action, state }) => {
      const data: RavenPlanFormDialogData = {
        adaptations: cloneDeep(state.hawk.adaptation.adaptations),
        selectedPlan: state.hawk.plan.plans[action.id as string] || null,
      };

      const componentDialog = this.dialog.open(RavenPlanFormDialogComponent, {
        data: data || {},
        width: '500px',
      });

      return zip(of(action), componentDialog.afterClosed());
    }),
    map(([action, result]) => ({ action, result })),
    exhaustMap(({ action, result }) => {
      if (result) {
        return of(
          // TODO: Dispatch SavePlan once we have a backend for it
          new SavePlanSuccess(result),
        );
      }
      return [];
    }),
  );

  @Effect()
  saveActivity$: Observable<Action> = this.actions$.pipe(
    ofType<SaveActivity>(PlanActionTypes.SaveActivity),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action }) =>
      this.planService.saveActivity(action.data).pipe(
        map(data => new SaveActivitySuccess(data)),
        catchError((e: Error) => of(new SaveActivityFailure(e))),
      ),
    ),
  );

  @Effect()
  saveActivityDetail$: Observable<Action> = this.actions$.pipe(
    ofType<SaveActivityDetail>(PlanActionTypes.SaveActivityDetail),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action }) => {
      return this.planService.saveActivity(action.data).pipe(
        map(data => {
          this.router.navigate(['/hawk'], {
            queryParams: {
              activityId: action.data.id,
            },
          });
          return new SaveActivityDetailSuccess(data);
        }),
        catchError((e: Error) => {
          console.error('PlanEffects - saveActivityDetail$: ', e);
          this.router.navigate(['/hawk'], {
            queryParams: {
              activityId: action.data.id,
              err: 'ActivityDetailSaveError',
            },
          });
          return of(new SaveActivityDetailFailure(e));
        }),
      );
    }),
  );
}
