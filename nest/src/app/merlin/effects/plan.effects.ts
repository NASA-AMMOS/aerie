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
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap, withLatestFrom } from 'rxjs/operators';
import { ToastActions } from '../../shared/actions';
import { NestConfirmDialogComponent } from '../../shared/components/nest-confirm-dialog/nest-confirm-dialog.component';
import { ActivityInstance } from '../../shared/models';
import { timestamp } from '../../shared/util';
import { PlanActions } from '../actions';
import { MerlinAppState } from '../merlin-store';
import { PlanService } from '../services/plan.service';
import { withLoadingBar } from './utils';

@Injectable()
export class PlanEffects {
  constructor(
    private actions: Actions,
    private dialog: MatDialog,
    private planService: PlanService,
    private router: Router,
    private store: Store<MerlinAppState>,
  ) {}

  createActivity = createEffect(() =>
    this.actions.pipe(
      ofType(PlanActions.createActivity),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      switchMap(({ action, state }) => {
        const end = action.data.start + action.data.duration;
        const activity: ActivityInstance = {
          activityId: action.data.activityId || '',
          activityType: action.data.activityType,
          backgroundColor: action.data.backgroundColor,
          constraints: [],
          duration: action.data.duration,
          end,
          endTimestamp: timestamp(end),
          intent: action.data.intent,
          listeners: [],
          name: action.data.name,
          parameters: [],
          start: action.data.start,
          startTimestamp: timestamp(action.data.start),
          textColor: action.data.textColor,
          y: 0,
        };

        return this.planService
          .createActivity(
            state.config.app.planServiceBaseUrl,
            action.planId,
            activity,
          )
          .pipe(
            switchMap((newActivity: ActivityInstance) => [
              PlanActions.createActivitySuccess({
                activity: newActivity,
                planId: action.planId,
              }),
              ToastActions.showToast({
                message:
                  'New activity has been successfully created and saved.',
                title: 'Create Activity Success',
                toastType: 'success',
              }),
            ]),
            catchError((error: Error) => [
              PlanActions.createActivityFailure({ error }),
              ToastActions.showToast({
                message: error.message,
                title: 'Create Activity Failed',
                toastType: 'error',
              }),
            ]),
          );
      }),
    ),
  );

  createActivitySuccess = createEffect(
    () =>
      this.actions.pipe(
        ofType(PlanActions.createActivitySuccess),
        switchMap(({ planId }) => {
          this.router.navigate([`/plans/${planId}`]);
          return [];
        }),
      ),
    { dispatch: false },
  );

  createPlan = createEffect(() =>
    this.actions.pipe(
      ofType(PlanActions.createPlan),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      switchMap(({ action, state }) =>
        this.planService
          .createPlan(state.config.app.planServiceBaseUrl, action.plan)
          .pipe(
            switchMap((plan: any) => [
              PlanActions.createPlanSuccess({ plan }),
              ToastActions.showToast({
                message: 'New plan has been successfully created and saved.',
                title: 'Create Plan Success',
                toastType: 'success',
              }),
            ]),
            catchError((error: Error) => [
              PlanActions.createPlanFailure({ error }),
              ToastActions.showToast({
                message: error.message,
                title: 'Create Plan Failure',
                toastType: 'error',
              }),
            ]),
          ),
      ),
    ),
  );

  deleteActivity = createEffect(() =>
    this.actions.pipe(
      ofType(PlanActions.deleteActivity),
      withLatestFrom(this.store),
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

        return forkJoin([
          of(action),
          of(state),
          deletePlanDialog.afterClosed(),
        ]);
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
                  PlanActions.deleteActivitySuccess({
                    activityId: action.activityId,
                  }),
                  ToastActions.showToast({
                    message: 'Activity has been successfully deleted.',
                    title: 'Delete Activity Success',
                    toastType: 'success',
                  }),
                ]),
                catchError((error: Error) => [
                  PlanActions.deleteActivityFailure({ error }),
                  ToastActions.showToast({
                    message: error.message,
                    title: 'Delete Activity Failure',
                    toastType: 'error',
                  }),
                ]),
              ),
          ]);
        }
        return [];
      }),
    ),
  );

  deletePlan = createEffect(() =>
    this.actions.pipe(
      ofType(PlanActions.deletePlan),
      withLatestFrom(this.store),
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

        return forkJoin([
          of(action),
          of(state),
          deletePlanDialog.afterClosed(),
        ]);
      }),
      map(([action, state, result]) => ({ action, state, result })),
      switchMap(({ action, state, result: { confirm } }) => {
        if (confirm) {
          return withLoadingBar([
            this.planService
              .deletePlan(state.config.app.planServiceBaseUrl, action.planId)
              .pipe(
                switchMap(() => [
                  PlanActions.deletePlanSuccess({
                    deletedPlanId: action.planId,
                  }),
                  ToastActions.showToast({
                    message: 'Plan has been successfully deleted.',
                    title: 'Delete Plan Success',
                    toastType: 'success',
                  }),
                ]),
                catchError((error: Error) => [
                  PlanActions.deletePlanFailure({ error }),
                  ToastActions.showToast({
                    message: error.message,
                    title: 'Delete Plan Failure',
                    toastType: 'error',
                  }),
                ]),
              ),
          ]);
        }
        return [];
      }),
    ),
  );

  updateActivity = createEffect(() =>
    this.actions.pipe(
      ofType(PlanActions.updateActivity),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      switchMap(({ state, action }) => {
        const { activities } = state.merlin.plan;
        const { planServiceBaseUrl } = state.config.app;

        if (!activities) {
          return of(
            PlanActions.updateActivityFailure({
              error: new Error(
                'UpdateActivity: UpdateActivityFailure: NoActivities',
              ),
            }),
          );
        }

        const activity = activities[action.activityId];

        if (!activity) {
          return of(
            PlanActions.updateActivityFailure({
              error: new Error(
                'UpdateActivity: UpdateActivityFailure: NoActivityFound',
              ),
            }),
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
                PlanActions.updateActivitySuccess({
                  activityId: action.activityId,
                  update: action.update,
                }),
                ToastActions.showToast({
                  message: 'Activity has been successfully updated.',
                  title: 'Update Activity Success',
                  toastType: 'success',
                }),
              ]),
              catchError((error: Error) => [
                PlanActions.updateActivityFailure({ error }),
                ToastActions.showToast({
                  message: error.message,
                  title: 'Update Activity Failure',
                  toastType: 'error',
                }),
              ]),
            ),
        ]);
      }),
    ),
  );
}
