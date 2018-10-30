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

import {
  AdaptationActionTypes,
  FetchAdaptationList,
  FetchAdaptationListFailure,
  FetchAdaptationListSuccess,
  OpenActivityTypeFormDialog,
  RemoveActivityType,
  RemoveActivityTypeSuccess,
  SaveActivityTypeFailure,
  SaveActivityTypeSuccess,
} from '../actions/adaptation.actions';

import { RavenActivityTypeFormDialogComponent } from '../../shared/components/components';
import { RavenActivityType } from '../../shared/models/raven-activity-type';
// TODO: Replace with a real service once an external service for activity types becomes available
import { AdaptationMockService } from '../../shared/services/adaptation-mock.service';
import { HawkAppState } from '../hawk-store';

@Injectable()
export class AdaptationEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<HawkAppState>,
    private adaptationService: AdaptationMockService,
    private dialog: MatDialog,
  ) {}

  @Effect()
  fetchAdaptationList$: Observable<Action> = this.actions$.pipe(
    ofType<FetchAdaptationList>(AdaptationActionTypes.FetchAdaptationList),
    concatMap(action => {
      return this.adaptationService.getAdaptations().pipe(
        map(data => new FetchAdaptationListSuccess(data)),
        catchError((e: Error) => {
          console.error('AdaptationEffects - fetchAdaptationList$: ', e);
          return of(new FetchAdaptationListFailure(e));
        }),
      );
    }),
  );

  @Effect()
  removeActivityType$: Observable<Action> = this.actions$.pipe(
    ofType<RemoveActivityType>(AdaptationActionTypes.RemoveActivityType),
    concatMap(action =>
      this.adaptationService
        .removeActivityType(action.id)
        .pipe(map(() => new RemoveActivityTypeSuccess(action.id))),
    ),
  );

  @Effect()
  openUpdateActivityTypeFormDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenActivityTypeFormDialog>(
      AdaptationActionTypes.OpenActivityTypeFormDialog,
    ),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    exhaustMap(({ action, state }) => {
      if (!state.hawk.adaptation.selectedAdaptation) {
        return of(
          new SaveActivityTypeFailure(new Error('NoSelectedAdaptation')),
        );
      }

      let data: RavenActivityType | null = null;
      if (action.id) {
        data =
          state.hawk.adaptation.selectedAdaptation.activityTypes[action.id] ||
          null;
      }

      const componentDialog = this.dialog.open(
        RavenActivityTypeFormDialogComponent,
        { data: data || {} },
      );

      return zip(of(action), componentDialog.afterClosed()).pipe(
        map(([resultAction, result]) => ({ resultAction, result })),
        exhaustMap(({ resultAction, result }) => {
          if (result) {
            return of(
              // TODO: Dispatch SaveActivityType once we have a backend for it
              new SaveActivityTypeSuccess(result, !resultAction.id),
            );
          }
          return of();
        }),
      );
    }),
  );
}
