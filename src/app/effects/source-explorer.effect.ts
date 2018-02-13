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
import { Actions, Effect } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { of } from 'rxjs/observable/of';

import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/toArray';
import 'rxjs/add/operator/withLatestFrom';

import { AppState } from './../../app/store';

import { SourceExplorerActionTypes } from './../actions/source-explorer';
import * as sourceExplorerActions from './../actions/source-explorer';

import {
  removeBandsOrPoints,
  toRavenBandData,
  toRavenSources,
} from './../shared/util';

import {
  MpsServerGraphData,
  MpsServerSource,
  RavenBandData,
  RavenSource,
} from './../shared/models';

@Injectable()
export class SourceExplorerEffects {
  @Effect()
  fetchGraphData$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.FetchGraphData>(SourceExplorerActionTypes.FetchGraphData)
    .withLatestFrom(this.store$)
    .map(([action, state]) => ({ action, state }))
    .switchMap(({ state, action }) =>
      this.http.get<MpsServerGraphData>(action.source.url)
        .map((graphData: MpsServerGraphData) => toRavenBandData(action.source.id, graphData, state.timeline.bands))
        .map((bandData: RavenBandData) => new sourceExplorerActions.FetchGraphDataSuccess(action.source, bandData.bands, bandData.bandIdToName, bandData.bandIdToPoints))
        .catch(() => of(new sourceExplorerActions.FetchGraphDataFailure())),
    );

  @Effect()
  fetchInitialSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.FetchInitialSources>(SourceExplorerActionTypes.FetchInitialSources)
    .withLatestFrom(this.store$)
    .map(([action, state]) => state)
    .switchMap((state: AppState) =>
      this.http.get<MpsServerSource[]>(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`)
        .map((mpsServerSources: MpsServerSource[]) => toRavenSources('0', true, mpsServerSources))
        .map((sources: RavenSource[]) =>  new sourceExplorerActions.FetchInitialSourcesSuccess(sources))
        .catch(() => of(new sourceExplorerActions.FetchInitialSourcesFailure())),
    );

  @Effect()
  fetchSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.FetchSources>(SourceExplorerActionTypes.FetchSources)
    .switchMap(action =>
      this.http.get<MpsServerSource[]>(action.source.url)
        .map((mpsServerSources: MpsServerSource[]) => toRavenSources(action.source.id, false, mpsServerSources))
        .map((sources: RavenSource[]) => new sourceExplorerActions.FetchSourcesSuccess(action.source, sources))
        .catch(() => of(new sourceExplorerActions.FetchSourcesFailure())),
    );

  @Effect()
  sourceExplorerCloseEvent$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.SourceExplorerCloseEvent>(SourceExplorerActionTypes.SourceExplorerCloseEvent)
    .withLatestFrom(this.store$)
    .map(([action, state]) => ({ action, state }))
    .map(({ state, action }) => new sourceExplorerActions.RemoveBands(action.source, removeBandsOrPoints(action.source.id, state.timeline.bands)));

  constructor(
    private http: HttpClient,
    private actions$: Actions,
    private store$: Store<AppState>,
  ) {}
}
