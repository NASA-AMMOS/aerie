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
import { catchError, map, switchMap, withLatestFrom } from 'rxjs/operators';
import { Activity } from '../../../../libs/schemas/types/ts';
import { ShowToast } from '../../shared/actions/toast.actions';
import { RavenPlanFormDialogComponent } from '../../shared/components/components';
import { RavenPlanFormDialogData } from '../../shared/models/raven-plan-form-dialog-data';
import { PlanService } from '../../shared/services/plan.service';
import { timestamp } from '../../shared/util';
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
  FetchActivities,
  FetchActivitiesFailure,
  FetchActivitiesSuccess,
  FetchPlans,
  FetchPlansFailure,
  FetchPlansSuccess,
  OpenPlanFormDialog,
  PlanActionTypes,
  UpdateActivity,
  UpdateActivityFailure,
  UpdateActivitySuccess,
} from '../actions/plan.actions';
import { PlanningAppState } from '../planning-store';
import { withLoadingBar } from './utils';

@Injectable()
export class PlanEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<PlanningAppState>,
    private planService: PlanService,
    private dialog: MatDialog,
    private router: Router,
  ) {}

  @Effect()
  createActivity$: Observable<Action> = this.actions$.pipe(
    ofType<CreateActivity>(PlanActionTypes.CreateActivity),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state }) => {
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
          switchMap(() => [
            new CreateActivitySuccess(action.planId),
            new ShowToast(
              'success',
              'New activity has been successfully created and saved.',
              'Create Activity Success',
            ),
          ]),
          catchError((e: Error) => [
            new CreateActivityFailure(e),
            new ShowToast('error', e.message, 'Create Activity Failed'),
          ]),
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
    switchMap(({ action, state }) =>
      this.planService
        .createPlan(state.config.app.apiBaseUrl, action.plan)
        .pipe(
          // TODO: Strongly type. Back end should pass back id instead of _id.
          switchMap((plan: any) => [
            new CreatePlanSuccess({ ...plan, id: plan._id || plan.id }),
            new ShowToast(
              'success',
              'New plan has been successfully created and saved.',
              'Create Plan Success',
            ),
          ]),
          catchError((e: Error) => [
            new CreatePlanFailure(e),
            new ShowToast('error', e.message, 'Create Plan Failure'),
          ]),
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
    switchMap(({ action, state }) =>
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
    switchMap(({ action, state }) =>
      this.planService
        .deletePlan(state.config.app.apiBaseUrl, action.planId)
        .pipe(
          map(() => new DeletePlanSuccess()),
          catchError((e: Error) => of(new DeletePlanFailure(e))),
        ),
    ),
  );

  @Effect()
  fetchActivities$: Observable<Action> = this.actions$.pipe(
    ofType<FetchActivities>(PlanActionTypes.FetchActivities),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state }) =>
      withLoadingBar([
        this.planService
          .getActivities(state.config.app.apiBaseUrl, action.planId)
          .pipe(
            map(
              (activities: Activity[]) =>
                new FetchActivitiesSuccess(
                  action.planId,
                  action.activityId,
                  activities,
                ),
            ),
            catchError((e: Error) => of(new FetchActivitiesFailure(e))),
          ),
      ]),
    ),
  );

  @Effect()
  fetchPlans$: Observable<Action> = this.actions$.pipe(
    ofType<FetchPlans>(PlanActionTypes.FetchPlans),
    withLatestFrom(this.store$),
    map(([_, state]) => state),
    switchMap(state =>
      withLoadingBar([
        this.planService.getPlans(state.config.app.apiBaseUrl).pipe(
          map(data => new FetchPlansSuccess(data)),
          catchError((e: Error) => of(new FetchPlansFailure(e))),
        ),
      ]),
    ),
  );

  @Effect()
  openUpdatePlanFormDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenPlanFormDialog>(PlanActionTypes.OpenPlanFormDialog),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state }) => {
      const data: RavenPlanFormDialogData = {
        adaptations: cloneDeep(state.planning.adaptation.adaptations),
        selectedPlan: state.planning.plan.plans[action.id as string] || null,
      };

      const componentDialog = this.dialog.open(RavenPlanFormDialogComponent, {
        data: data || {},
        width: '500px',
      });

      return zip(of(action), componentDialog.afterClosed());
    }),
    map(([_, result]) => result),
    switchMap(result => {
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
      const { activities, selectedPlan } = state.planning.plan;
      const { apiBaseUrl } = state.config.app;

      if (!selectedPlan) {
        return of(
          new UpdateActivityFailure(
            new Error('UpdateActivity: UpdateActivityFailure: NoSelectedPlan'),
          ),
        );
      }

      if (!activities) {
        return of(
          new UpdateActivityFailure(
            new Error('UpdateActivity: UpdateActivityFailure: NoActivities'),
          ),
        );
      }

      const activity = activities[action.activityId];

      if (!activity) {
        return of(
          new UpdateActivityFailure(
            new Error('UpdateActivity: UpdateActivityFailure: NoActivityFound'),
          ),
        );
      }

      // Since we are sending a PATCH, make sure that we are only
      // sending value that changed.
      const changes = omitBy(action.update, (v, k) => activity[k] === v);

      return this.planService
        .updateActivity(
          apiBaseUrl,
          selectedPlan.id,
          action.activityId,
          changes as Activity,
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
      const { selectedPlan } = state.planning.plan;

      if (selectedPlan) {
        this.router.navigate([`/plans/${selectedPlan.id}`]);
      } else {
        console.error('UpdateActivitySuccess: NoSelectedPlan');
      }

      return [];
    }),
  );
}
