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
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs';
import { catchError, concatMap, map } from 'rxjs/operators';
import { MpsServerEpoch, RavenEpoch } from '../../shared/models';
import { toRavenEpochs } from '../../shared/util';
import { EpochsActionTypes, FetchEpochs } from '../actions/epochs.actions';

import * as epochsActions from '../actions/epochs.actions';
import * as toastActions from '../actions/toast.actions';

@Injectable()
export class EpochsEffects {
  @Effect()
  fetchEpochs$: Observable<Action> = this.actions$.pipe(
    ofType<FetchEpochs>(EpochsActionTypes.FetchEpochs),
    concatMap(action =>
      this.http.get(action.url).pipe(
        map((mpsServerEpochs: MpsServerEpoch[]) =>
          toRavenEpochs(mpsServerEpochs),
        ),
        map(
          (ravenEpochs: RavenEpoch[]) =>
            new epochsActions.AddEpochs(ravenEpochs),
        ),
      ),
    ),
    catchError((e: Error) => {
      console.error('EpochsEffects - fetchEpochs$: ', e);
      return [
        new toastActions.ShowToast('warning', 'Failed to fetch epochs', ''),
        new epochsActions.AddEpochs([]),
      ];
    }),
  );

  constructor(private http: HttpClient, private actions$: Actions) {}
}
