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
import { cloneDeep, omitBy } from 'lodash';
import { Observable, of, zip } from 'rxjs';
import { RavenPlanFormDialogComponent } from '../../shared/components/components';
import { RavenActivity } from '../../shared/models';
import { RavenPlanFormDialogData } from '../../shared/models/raven-plan-form-dialog-data';
import { PlanService } from '../../shared/services/plan.service';
import { timestamp } from '../../shared/util';
import { HawkAppState } from '../hawk-store';

import {
  catchError,
  concatMap,
  exhaustMap,
  map,
  switchMap,
  withLatestFrom,
} from 'rxjs/operators';

import {
  ClearSelectedActivity,
  CreateActivity,
  CreateActivityFailure,
  CreateActivitySuccess,
  CreatePlan,
  CreatePlanFailure,
  CreatePlanSuccess,
  DeleteActivity,
  DeleteActivityFailure,
  DeleteActivitySuccess,
  DeletePlan,
  DeletePlanFailure,
  DeletePlanSuccess,
  FetchPlanList,
  FetchPlanListFailure,
  FetchPlanListSuccess,
  OpenPlanFormDialog,
  PlanActionTypes,
  UpdateActivity,
  UpdateActivityFailure,
  UpdateActivitySuccess,
} from '../actions/plan.actions';

@Injectable()
export class PlanEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<HawkAppState>,
    private planService: PlanService,
    private dialog: MatDialog,
    private router: Router,
  ) {}

  @Effect()
  createActivity$: Observable<Action> = this.actions$.pipe(
    ofType<CreateActivity>(PlanActionTypes.CreateActivity),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) => {
      // TODO: Should we let the service handle the filling in of blank values here?

      const end = action.data.start + action.data.duration;

      return this.planService
        .createActivity(state.config.app.apiBaseUrl, action.planId, {
          ...action.data,
          color: '#ffffff',
          constraints: [],
          end,
          endTimestamp: timestamp(end),
          parameters: [],
          startTimestamp: timestamp(action.data.start),
          y: null,
        })
        .pipe(
          map(() => new CreateActivitySuccess(action.planId)),
          catchError((e: Error) => of(new CreateActivityFailure(e))),
        );
    }),
  );

  @Effect({ dispatch: false })
  createActivitySuccess$: Observable<Action> = this.actions$.pipe(
    ofType<CreateActivitySuccess>(PlanActionTypes.CreateActivitySuccess),
    switchMap(action => {
      this.router.navigate([`/plans/${action.planId}`]);
      return [];
    }),
  );

  @Effect()
  createPlan$: Observable<Action> = this.actions$.pipe(
    ofType<CreatePlan>(PlanActionTypes.CreatePlan),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      this.planService
        .createPlan(state.config.app.apiBaseUrl, action.plan)
        .pipe(
          // TODO: Strongly type. Back end should pass back id instead of _id.
          map(
            (plan: any) =>
              new CreatePlanSuccess({ ...plan, id: plan._id || plan.id }),
          ),
          catchError((e: Error) => of(new CreatePlanFailure(e))),
        ),
    ),
  );

  @Effect({ dispatch: false })
  createPlanSuccess$: Observable<Action> = this.actions$.pipe(
    ofType<CreatePlanSuccess>(PlanActionTypes.CreatePlanSuccess),
    switchMap(action => {
      this.router.navigate([`/plans/${action.plan.id}`]);
      return of(new ClearSelectedActivity());
    }),
  );

  @Effect()
  deleteActivity$: Observable<Action> = this.actions$.pipe(
    ofType<DeleteActivity>(PlanActionTypes.DeleteActivity),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      this.planService
        .deleteActivity(
          state.config.app.apiBaseUrl,
          action.planId,
          action.activityId,
        )
        .pipe(
          map(() => new DeleteActivitySuccess()),
          catchError((e: Error) => of(new DeleteActivityFailure(e))),
        ),
    ),
  );

  @Effect()
  deletePlan$: Observable<Action> = this.actions$.pipe(
    ofType<DeletePlan>(PlanActionTypes.DeletePlan),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      this.planService
        .deletePlan(state.config.app.apiBaseUrl, action.planId)
        .pipe(
          map(() => new DeletePlanSuccess()),
          catchError((e: Error) => of(new DeletePlanFailure(e))),
        ),
    ),
  );

  @Effect()
  fetchPlanList$: Observable<Action> = this.actions$.pipe(
    ofType<FetchPlanList>(PlanActionTypes.FetchPlanList),
    withLatestFrom(this.store$),
    map(([_, state]) => state),
    concatMap(state =>
      this.planService.getPlans(state.config.app.apiBaseUrl).pipe(
        map(data => new FetchPlanListSuccess(data)),
        catchError((e: Error) => of(new FetchPlanListFailure(e))),
      ),
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
    map(([_, result]) => result),
    exhaustMap(result => {
      if (result) {
        return of(new CreatePlan(result));
      }
      return [];
    }),
  );

  @Effect()
  updateActivity$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateActivity>(PlanActionTypes.UpdateActivity),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ state, action }) => {
      const { selectedPlan } = state.hawk.plan;
      const { apiBaseUrl } = state.config.app;

      if (!selectedPlan) {
        return of(
          new UpdateActivityFailure(
            new Error('UpdateActivity: UpdateActivityFailure: NoSelectedPlan'),
          ),
        );
      }

      const activityInstance =
        selectedPlan.activityInstances[action.activityId];

      if (!activityInstance) {
        return of(
          new UpdateActivityFailure(
            new Error(
              'UpdateActivity: UpdateActivityFailure: NoActivityInstanceFound',
            ),
          ),
        );
      }

      // Since we are sending a PATCH, make sure that we are only
      // sending value that changed.
      const changes = omitBy(
        action.update,
        (v, k) => activityInstance[k] === v,
      );

      return this.planService
        .updateActivityInstance(
          apiBaseUrl,
          selectedPlan.id,
          action.activityId,
          changes as RavenActivity,
        )
        .pipe(
          map(_ => new UpdateActivitySuccess(action.activityId, changes)),
          catchError((e: Error) => of(new UpdateActivityFailure(e))),
        );
    }),
  );

  @Effect({ dispatch: false })
  updateActivitySuccess$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateActivitySuccess>(PlanActionTypes.UpdateActivitySuccess),
    withLatestFrom(this.store$),
    map(([_, state]) => state),
    switchMap(state => {
      const { selectedPlan } = state.hawk.plan;

      if (selectedPlan) {
        this.router.navigate([`/plans/${selectedPlan.id}`]);
      }

      return [];
    }),
  );
}
