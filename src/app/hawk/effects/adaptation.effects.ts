/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { Observable, of } from 'rxjs';
import { catchError, concatMap, map, withLatestFrom } from 'rxjs/operators';
import { AdaptationService } from '../../shared/services/adaptation.service';
import { HawkAppState } from '../hawk-store';

import {
  AdaptationActionTypes,
  FetchActivityTypes,
  FetchActivityTypesFailure,
  FetchActivityTypesSuccess,
  FetchAdaptationList,
  FetchAdaptationListFailure,
  FetchAdaptationListSuccess,
} from '../actions/adaptation.actions';

@Injectable()
export class AdaptationEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<HawkAppState>,
    private adaptationService: AdaptationService,
  ) {}

  @Effect()
  fetchActivityTypes$: Observable<Action> = this.actions$.pipe(
    ofType<FetchActivityTypes>(AdaptationActionTypes.FetchActivityTypes),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      this.adaptationService
        .getActivityTypes(state.config.app.apiBaseUrl, action.adaptationId)
        .pipe(
          map(data => new FetchActivityTypesSuccess(data)),
          catchError((e: Error) => {
            console.error('AdaptationEffects - fetchActivityTypes$: ', e);
            return of(new FetchActivityTypesFailure(e));
          }),
        ),
    ),
  );

  @Effect()
  fetchAdaptationList$: Observable<Action> = this.actions$.pipe(
    ofType<FetchAdaptationList>(AdaptationActionTypes.FetchAdaptationList),
    withLatestFrom(this.store$),
    map(([_, state]) => state),
    concatMap((state: HawkAppState) =>
      this.adaptationService.getAdaptations(state.config.app.apiBaseUrl).pipe(
        map(data => new FetchAdaptationListSuccess(data)),
        catchError((e: Error) => {
          console.error('AdaptationEffects - fetchAdaptationList$: ', e);
          return of(new FetchAdaptationListFailure(e));
        }),
      ),
    ),
  );
}
