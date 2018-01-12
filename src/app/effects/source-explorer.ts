/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/operator/toArray';
import 'rxjs/add/operator/withLatestFrom';
import { Injectable } from '@angular/core';
import { Action, Store } from '@ngrx/store';
import { Effect, Actions } from '@ngrx/effects';
import { Observable } from 'rxjs/Observable';
import { of } from 'rxjs/observable/of';

import { AppState } from './../../app/store';
import { MpsServerApiService } from './../services/mps-server-api.service';
import * as sourceExplorer from '../actions/source-explorer';

import { fromSources } from './../util/source';

@Injectable()
export class SourceExplorerEffects {
  @Effect()
  fetchInitialSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorer.FetchInitialSources>(sourceExplorer.SourceExplorerActionTypes.FetchInitialSources)
    .withLatestFrom(this.store$)
    .map(([action, state]) => state)
    .mergeMap(state =>
      this.mpsServerApi.fetchSources(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`)
        .map(sources => new sourceExplorer.FetchInitialSourcesSuccess(fromSources('0', true, sources)))
        .catch(() => of(new sourceExplorer.FetchInitialSourcesFailure())),
    );

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
    private mpsServerApi: MpsServerApiService,
  ) {}
}
