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
import { Action } from '@ngrx/store';
import { Observable, of } from 'rxjs';
import { catchError, concatMap, map } from 'rxjs/operators';

import {
  AdaptationActionTypes,
  FetchAdaptationList,
  FetchAdaptationListFailure,
  FetchAdaptationListSuccess,
} from '../actions/adaptation.actions';

// TODO: Replace with a real service once an external service for activity types becomes available
import { AdaptationMockService } from '../../shared/services/adaptation-mock.service';

@Injectable()
export class AdaptationEffects {
  constructor(
    private actions$: Actions,
    private adaptationService: AdaptationMockService,
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
}
