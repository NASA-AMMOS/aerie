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

import { SourceExplorerActionTypes } from './../actions/source-explorer';

import * as sourceExplorerActions from './../actions/source-explorer';

import {
  toRavenSources,
} from './../util/source';

import {
  toRavenBandData,
} from './../util/bands';

import {
  MpsServerGraphData,
  MpsServerSource,
  RavenBandData,
  RavenSource,
} from './../models';

@Injectable()
export class SourceExplorerEffects {
  @Effect()
  fetchGraphData$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.FetchGraphData>(SourceExplorerActionTypes.FetchGraphData)
    .withLatestFrom(this.store$)
    .map(([action, state]) => ({ action, state }))
    .switchMap(({ state, action }) =>
      this.mpsServerApi.fetchGraphData(action.source.url)
        .map((graphData: MpsServerGraphData) => toRavenBandData(action.source.id, graphData, state.timeline.bands))
        .map((bandData: RavenBandData) => new sourceExplorerActions.FetchGraphDataSuccess(action.source, bandData.bands, bandData.bandIdsToPoints))
        .catch(() => of(new sourceExplorerActions.FetchGraphDataFailure())),
    );

  @Effect()
  fetchInitialSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.FetchInitialSources>(SourceExplorerActionTypes.FetchInitialSources)
    .withLatestFrom(this.store$)
    .map(([action, state]) => state)
    .switchMap((state: AppState) =>
      this.mpsServerApi.fetchSources(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`)
        .map((mpsServerSources: MpsServerSource[]) => toRavenSources('0', true, mpsServerSources))
        .map((sources: RavenSource[]) =>  new sourceExplorerActions.FetchInitialSourcesSuccess(sources))
        .catch(() => of(new sourceExplorerActions.FetchInitialSourcesFailure())),
    );

  @Effect()
  fetchSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.FetchSources>(SourceExplorerActionTypes.FetchSources)
    .switchMap(action =>
      this.mpsServerApi.fetchSources(action.source.url)
        .map((mpsServerSources: MpsServerSource[]) => toRavenSources(action.source.id, false, mpsServerSources))
        .map((sources: RavenSource[]) => new sourceExplorerActions.FetchSourcesSuccess(action.source, sources))
        .catch(() => of(new sourceExplorerActions.FetchSourcesFailure())),
    );

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
    private mpsServerApi: MpsServerApiService,
  ) {}
}
