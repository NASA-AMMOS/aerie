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

import * as sourceExplorer from './../actions/source-explorer';
import * as timeline from './../actions/timeline';

import {
  toSources,
} from './../util/source';

import {
  graphDataToBands,
  removeBandsOrPoints,
  removeSameLegendActivityBands,
} from './../util/bands';

import {
  MpsServerGraphData,
  MpsServerSource,
} from './../models';

import { SourceExplorerAction } from './../actions/source-explorer';
import { TimelineAction } from './../actions/timeline';

@Injectable()
export class SourceExplorerEffects {
  @Effect()
  fetchInitialSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorer.FetchInitialSources>(sourceExplorer.SourceExplorerActionTypes.FetchInitialSources)
    .withLatestFrom(this.store$)
    .map(([action, state]) => state)
    .switchMap(state =>
      this.mpsServerApi.fetchSources(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`)
        .map(sources => new sourceExplorer.FetchInitialSourcesSuccess(toSources('0', true, sources)))
        .catch(() => of(new sourceExplorer.FetchInitialSourcesFailure())),
    );

  @Effect()
  sourceExplorerExpandWithFetchSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorer.SourceExplorerExpandWithFetchSources>(sourceExplorer.SourceExplorerActionTypes.SourceExplorerExpandWithFetchSources)
    .switchMap(action =>
      this.mpsServerApi.fetchSources(action.source.url)
        .switchMap((sources: MpsServerSource[]) => [
          new sourceExplorer.FetchSourcesSuccess(action.source, toSources(action.source.id, false, sources)),
          new sourceExplorer.SourceExplorerExpand(action.source),
        ])
        .catch(() => [
          new sourceExplorer.FetchSourcesFailure(),
          new sourceExplorer.SourceExplorerCollapse(action.source),
        ]),
    );

  @Effect()
  sourceExplorerExpandWithLoadContent$: Observable<Action> = this.actions$
    .ofType<sourceExplorer.SourceExplorerExpandWithLoadContent>(sourceExplorer.SourceExplorerActionTypes.SourceExplorerExpandWithLoadContent)
    .switchMap(action => [
      new sourceExplorer.SourceExplorerExpand(action.source),
    ]);

  @Effect()
  sourceExplorerOpenWithFetchGraphData$: Observable<Action> = this.actions$
    .ofType<sourceExplorer.SourceExplorerOpenWithFetchGraphData>(sourceExplorer.SourceExplorerActionTypes.SourceExplorerOpenWithFetchGraphData)
    .withLatestFrom(this.store$)
    .map(([action, state]) => ({ action, state }))
    .switchMap(context => {
      const { state, action } = context;

      return this.mpsServerApi.fetchGraphData(action.source.url)
        .switchMap((graphData: MpsServerGraphData) => {
          const actions: (SourceExplorerAction | TimelineAction)[] = [];
          const potentialBands = graphDataToBands(action.source.id, graphData);
          const { bandIdsToPoints = {}, newBands = [] } = removeSameLegendActivityBands(state.timeline.bands, potentialBands);
          const bandIdsToPointsKeys = Object.keys(bandIdsToPoints);

          if (newBands.length > 0 || bandIdsToPointsKeys.length > 0) {
            if (newBands.length > 0) {
              actions.push(new timeline.AddBands(action.source.id, newBands));
            }

            if (bandIdsToPointsKeys.length > 0) {
              actions.push(new timeline.AddPointsToBands(action.source.id, bandIdsToPoints));
            }

            actions.push(new sourceExplorer.SourceExplorerOpen(action.source));
          }

          return actions;
        });
    });

  @Effect()
  sourceExplorerCloseWithRemoveBands$: Observable<Action> = this.actions$
    .ofType<sourceExplorer.SourceExplorerCloseWithRemoveBands>(sourceExplorer.SourceExplorerActionTypes.SourceExplorerCloseWithRemoveBands)
    .withLatestFrom(this.store$)
    .map(([action, state]) => ({ action, state }))
    .switchMap(context => {
      const actions: (SourceExplorerAction | TimelineAction)[] = [];
      const { state, action } = context;
      const { removeBandIds = [], removePointsBandIds = [] } = removeBandsOrPoints(action.source.id, state.timeline.bands);

      if (removeBandIds.length > 0 || removePointsBandIds.length > 0) {
        if (removeBandIds.length > 0) {
          actions.push(new timeline.RemoveBands(action.source.id, removeBandIds));
        }

        if (removePointsBandIds.length > 0) {
          actions.push(new timeline.RemovePointsFromBands(action.source.id, removePointsBandIds));
        }

        actions.push(new sourceExplorer.SourceExplorerClose(action.source));
      }

      return actions;
    });

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
    private mpsServerApi: MpsServerApiService,
  ) {}
}
