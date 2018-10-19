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

import { RavenActivityTypeFormDialogComponent } from '../../shared/components/components';

import {
  ActivityTypeActionTypes,
  FetchActivityTypeList,
  FetchActivityTypeListFailure,
  FetchActivityTypeListSuccess,
  OpenActivityTypeFormDialog,
  RemoveActivityType,
  RemoveActivityTypeSuccess,
  SaveActivityTypeSuccess,
} from '../actions/activity-type.actions';

import { RavenActivityType } from '../../shared/models/raven-activity-type';
// TODO: Replace with a real service once an external service for activity types becomes available
import { ActivityTypeMockService } from '../../shared/services/activity-type-mock.service';
import { HawkAppState } from '../hawk-store';
// import * as fromActivityType from '../reducers/activity-type.reducer';

@Injectable()
export class ActivityTypeEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<HawkAppState>,
    private activityTypeService: ActivityTypeMockService,
    private dialog: MatDialog,
  ) {}

  @Effect()
  fetchActivityTypeList$: Observable<Action> = this.actions$.pipe(
    ofType<FetchActivityTypeList>(
      ActivityTypeActionTypes.FetchActivityTypeList,
    ),
    concatMap(action => {
      return this.activityTypeService.getActivityTypes().pipe(
        map(data => new FetchActivityTypeListSuccess(data)),
        catchError((e: Error) => {
          console.error('ActivityTypeEffects - fetchActivityTypeList$: ', e);
          return of(new FetchActivityTypeListFailure(e));
        }),
      );
    }),
  );

  @Effect()
  removeActivityType$: Observable<Action> = this.actions$.pipe(
    ofType<RemoveActivityType>(ActivityTypeActionTypes.RemoveActivityType),
    concatMap(action => {
      return this.activityTypeService
        .removeActivityType(action.id)
        .pipe(map(data => new RemoveActivityTypeSuccess(action.id)));
    }),
  );

  @Effect()
  openUpdateActivityTypeFormDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenActivityTypeFormDialog>(
      ActivityTypeActionTypes.OpenActivityTypeFormDialog,
    ),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    exhaustMap(({ action, state }) => {
      const data: RavenActivityType | null =
        state.hawk.activityType.activityTypes.find(a => a.id === action.id) ||
        null;

      const componentDialog = this.dialog.open(
        RavenActivityTypeFormDialogComponent,
        { data: data || {} },
      );

      return zip(of(action), componentDialog.afterClosed());
    }),
    map(([action, result]) => ({ action, result })),
    exhaustMap(({ action, result }) => {
      if (result) {
        return of(
          // TODO: Dispatch SaveActivityType once we have a backend for it
          new SaveActivityTypeSuccess(result, !action.id),
        );
      }
      return [];
    }),
  );
}
