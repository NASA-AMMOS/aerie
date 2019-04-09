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
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, switchMap, withLatestFrom } from 'rxjs/operators';
import { ShowToast } from '../../shared/actions/toast.actions';
import { NestConfirmDialogComponent } from '../../shared/components/nest-confirm-dialog/nest-confirm-dialog.component';
import { ActivityInstance } from '../../shared/models';
import { timestamp } from '../../shared/util';
import {
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
  PlanActionTypes,
  UpdateActivity,
  UpdateActivityFailure,
  UpdateActivitySuccess,
} from '../actions/plan.actions';
import { PlanningAppState } from '../planning-store';
import { PlanService } from '../services/plan.service';
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
      const end = action.data.start + action.data.duration;
      const activity: ActivityInstance = {
        activityId: action.data.activityId,
        activityType: action.data.activityType,
        color: '#ffffff',
        constraints: [],
        duration: action.data.duration,
        end,
        endTimestamp: timestamp(end),
        intent: action.data.intent,
        name: action.data.name,
        parameters: [],
        start: action.data.start,
        startTimestamp: timestamp(action.data.start),
        y: null,
      };

      return this.planService
        .createActivity(
          state.config.app.planServiceBaseUrl,
          action.planId,
          activity,
        )
        .pipe(
          switchMap((newActivity: ActivityInstance) => [
            new CreateActivitySuccess(action.planId, newActivity),
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
        .createPlan(state.config.app.planServiceBaseUrl, action.plan)
        .pipe(
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

  @Effect()
  deleteActivity$: Observable<Action> = this.actions$.pipe(
    ofType<DeleteActivity>(PlanActionTypes.DeleteActivity),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state }) => {
      const deletePlanDialog = this.dialog.open(NestConfirmDialogComponent, {
        data: {
          cancelText: 'No',
          confirmText: 'Yes',
          message: `Are you sure you want to delete this activity?`,
        },
        width: '400px',
      });

      return forkJoin(of(action), of(state), deletePlanDialog.afterClosed());
    }),
    map(([action, state, result]) => ({ action, state, result })),
    switchMap(({ action, state, result: { confirm } }) => {
      if (confirm) {
        return withLoadingBar([
          this.planService
            .deleteActivity(
              state.config.app.planServiceBaseUrl,
              action.planId,
              action.activityId,
            )
            .pipe(
              switchMap(() => [
                new DeleteActivitySuccess(action.activityId),
                new ShowToast(
                  'success',
                  'Activity has been successfully deleted.',
                  'Delete Activity Success',
                ),
              ]),
              catchError((e: Error) => [
                new DeleteActivityFailure(e),
                new ShowToast('error', e.message, 'Delete Activity Failure'),
              ]),
            ),
        ]);
      }
      return [];
    }),
  );

  @Effect()
  deletePlan$: Observable<Action> = this.actions$.pipe(
    ofType<DeletePlan>(PlanActionTypes.DeletePlan),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state }) => {
      const deletePlanDialog = this.dialog.open(NestConfirmDialogComponent, {
        data: {
          cancelText: 'No',
          confirmText: 'Yes',
          message: `Are you sure you want to delete this plan?`,
        },
        width: '400px',
      });

      return forkJoin(of(action), of(state), deletePlanDialog.afterClosed());
    }),
    map(([action, state, result]) => ({ action, state, result })),
    switchMap(({ action, state, result: { confirm } }) => {
      if (confirm) {
        return withLoadingBar([
          this.planService
            .deletePlan(state.config.app.planServiceBaseUrl, action.planId)
            .pipe(
              switchMap(() => [
                new DeletePlanSuccess(action.planId),
                new ShowToast(
                  'success',
                  'Plan has been successfully deleted.',
                  'Delete Plan Success',
                ),
              ]),
              catchError((e: Error) => [
                new DeletePlanFailure(e),
                new ShowToast('error', e.message, 'Delete Plan Failure'),
              ]),
            ),
        ]);
      }
      return [];
    }),
  );

  @Effect()
  fetchActivities$: Observable<Action> = this.actions$.pipe(
    ofType<FetchActivities>(PlanActionTypes.FetchActivities),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state }) =>
      withLoadingBar([
        this.planService
          .getActivities(state.config.app.planServiceBaseUrl, action.planId)
          .pipe(
            map(
              (activities: ActivityInstance[]) =>
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
        this.planService.getPlans(state.config.app.planServiceBaseUrl).pipe(
          map(data => new FetchPlansSuccess(data)),
          catchError((e: Error) => of(new FetchPlansFailure(e))),
        ),
      ]),
    ),
  );

  @Effect()
  updateActivity$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateActivity>(PlanActionTypes.UpdateActivity),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ state, action }) => {
      const { activities } = state.planning.plan;
      const { planServiceBaseUrl } = state.config.app;

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

      return withLoadingBar([
        this.planService
          .updateActivity(
            planServiceBaseUrl,
            action.planId,
            action.activityId,
            action.update as ActivityInstance,
          )
          .pipe(
            switchMap(_ => [
              new UpdateActivitySuccess(action.activityId, action.update),
              new ShowToast(
                'success',
                'Activity has been successfully updated.',
                'Update Activity Success',
              ),
            ]),
            catchError((e: Error) => [
              new UpdateActivityFailure(e),
              new ShowToast('error', e.message, 'Update Activity Failure'),
            ]),
          ),
      ]);
    }),
  );
}
