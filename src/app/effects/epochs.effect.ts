/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import {
  Actions,
  Effect,
  ofType,
} from '@ngrx/effects';

import {
  Action,
  Store,
} from '@ngrx/store';

import { Observable } from 'rxjs/Observable';
import { of } from 'rxjs/observable/of';
import { AppState } from './../../app/store';

import {
  catchError,
  concatMap,
  map,
  withLatestFrom,
} from 'rxjs/operators';

import {
  EpochsActionTypes,
} from './../actions/epochs';

import {
  MpsServerEpoch,
  RavenEpoch,
} from './../shared/models';

import {
  toRavenEpochs,
} from './../shared/util';

import * as epochsActions from './../actions/epochs';

@Injectable()
export class EpochsEffects {
  @Effect()
  fetchEpochs$: Observable<Action> = this.actions$.pipe(
    ofType<epochsActions.FetchEpochs>(EpochsActionTypes.FetchEpochs),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({action}) =>
      this.http.get<MpsServerEpoch[]>(action.url).pipe (
      map(serverEpochs => toRavenEpochs(serverEpochs)),
      map((epochs: RavenEpoch[]) => new epochsActions.AddEpochs(epochs))),
    ),
    catchError((e) => {
      console.error('epoch fetch error:', e);
      return of(new epochsActions.AddEpochs([]));
    }),
  );


  constructor(
    private http: HttpClient,
    private actions$: Actions,
    private store$: Store<AppState>,
  ) { }
}
