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

import {
  MpsServerSource,
} from './../models';

@Injectable()
export class SourceExplorerEffects {
  @Effect()
  fetchInitialSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorer.FetchInitialSources>(sourceExplorer.SourceExplorerActionTypes.FetchInitialSources)
    .withLatestFrom(this.store$)
    .map(([action, state]) => state)
    .switchMap(state =>
      this.mpsServerApi.fetchSources(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`)
        .map(sources => new sourceExplorer.FetchInitialSourcesSuccess(fromSources('0', true, sources)))
        .catch(() => of(new sourceExplorer.FetchInitialSourcesFailure())),
    );

  @Effect()
  sourceExplorerExpandWithFetchSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorer.SourceExplorerExpandWithFetchSources>(sourceExplorer.SourceExplorerActionTypes.SourceExplorerExpandWithFetchSources)
    .switchMap(action =>
      this.mpsServerApi.fetchSources(action.source.url)
        .mergeMap((sources: MpsServerSource[]) => [
          new sourceExplorer.FetchSourcesSuccess(action.source, fromSources(action.source.id, false, sources)),
          new sourceExplorer.SourceExplorerExpand(action.source),
        ])
        .catch(() => [
          new sourceExplorer.FetchSourcesFailure(),
          new sourceExplorer.SourceExplorerCollapse(action.source),
        ]),
    );

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
    private mpsServerApi: MpsServerApiService,
  ) {}
}
