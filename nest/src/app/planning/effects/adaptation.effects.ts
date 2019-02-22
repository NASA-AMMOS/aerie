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
import { catchError, map, switchMap, withLatestFrom } from 'rxjs/operators';
import { AdaptationService } from '../../shared/services/adaptation.service';
import {
  AdaptationActionTypes,
  FetchActivityTypes,
  FetchActivityTypesFailure,
  FetchActivityTypesSuccess,
  FetchAdaptations,
  FetchAdaptationsFailure,
  FetchAdaptationsSuccess,
} from '../actions/adaptation.actions';
import { PlanningAppState } from '../planning-store';
import { withLoadingBar } from './utils';

@Injectable()
export class AdaptationEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<PlanningAppState>,
    private adaptationService: AdaptationService,
  ) {}

  @Effect()
  fetchActivityTypes$: Observable<Action> = this.actions$.pipe(
    ofType<FetchActivityTypes>(AdaptationActionTypes.FetchActivityTypes),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state }) =>
      withLoadingBar([
        this.adaptationService
          .getActivityTypes(state.config.app.apiBaseUrl, action.adaptationId)
          .pipe(
            map(data => new FetchActivityTypesSuccess(data)),
            catchError((e: Error) => {
              console.error('AdaptationEffects - fetchActivityTypes$: ', e);
              return of(new FetchActivityTypesFailure(e));
            }),
          ),
      ]),
    ),
  );

  @Effect()
  fetchAdaptations$: Observable<Action> = this.actions$.pipe(
    ofType<FetchAdaptations>(AdaptationActionTypes.FetchAdaptations),
    withLatestFrom(this.store$),
    map(([_, state]) => state),
    switchMap((state: PlanningAppState) =>
      withLoadingBar([
        this.adaptationService.getAdaptations(state.config.app.apiBaseUrl).pipe(
          map(data => new FetchAdaptationsSuccess(data)),
          catchError((e: Error) => {
            console.error('AdaptationEffects - fetchAdaptations$: ', e);
            return of(new FetchAdaptationsFailure(e));
          }),
        ),
      ]),
    ),
  );
}
