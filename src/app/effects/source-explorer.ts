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

import { AppState } from './../../app/store';
import { MpsServerApiService } from './../services/mps-server-api.service';

import { SourceExplorerActionTypes, SourceExplorerAction } from './../actions/source-explorer';
import { TimelineAction } from './../actions/timeline';

import * as sourceExplorerActions from './../actions/source-explorer';
import * as timelineActions from './../actions/timeline';

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

@Injectable()
export class SourceExplorerEffects {
  @Effect()
  fetchInitialSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.FetchInitialSources>(SourceExplorerActionTypes.FetchInitialSources)
    .withLatestFrom(this.store$)
    .map(([action, state]) => state)
    .switchMap(state =>
      this.mpsServerApi.fetchSources(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`)
        .switchMap((sources: MpsServerSource[]) => [
          new sourceExplorerActions.FetchInitialSourcesSuccess(toSources('0', true, sources)),
        ])
        .catch(() => [
          // TODO: Dispatch error action.
        ]),
    );

  @Effect()
  sourceExplorerExpandWithFetchSources$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.SourceExplorerExpandWithFetchSources>(SourceExplorerActionTypes.SourceExplorerExpandWithFetchSources)
    .switchMap(action =>
      this.mpsServerApi.fetchSources(action.source.url)
        .switchMap((sources: MpsServerSource[]) => [
          new sourceExplorerActions.FetchSourcesSuccess(action.source, toSources(action.source.id, false, sources)),
          new sourceExplorerActions.SourceExplorerExpand(action.source),
        ])
        .catch(() => [
          new sourceExplorerActions.SourceExplorerCollapse(action.source),
          // TODO: Dispatch error action.
        ]),
    );

  @Effect()
  sourceExplorerExpandWithLoadContent$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.SourceExplorerExpandWithLoadContent>(SourceExplorerActionTypes.SourceExplorerExpandWithLoadContent)
    .switchMap(action => [
      new sourceExplorerActions.SourceExplorerExpand(action.source),
    ]);

  @Effect()
  sourceExplorerOpenWithFetchGraphData$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.SourceExplorerOpenWithFetchGraphData>(SourceExplorerActionTypes.SourceExplorerOpenWithFetchGraphData)
    .withLatestFrom(this.store$)
    .map(([action, state]) => ({ action, state }))
    .switchMap(({ state, action }) =>
      this.mpsServerApi.fetchGraphData(action.source.url)
        .switchMap((graphData: MpsServerGraphData) => {
          const actions: (SourceExplorerAction | TimelineAction)[] = [];
          const potentialBands = graphDataToBands(action.source.id, graphData);
          const { bandIdsToPoints = {}, newBands = [] } = removeSameLegendActivityBands(state.timeline.bands, potentialBands);
          const bandIdsToPointsKeys = Object.keys(bandIdsToPoints);

          if (newBands.length > 0 || bandIdsToPointsKeys.length > 0) {
            if (newBands.length > 0) {
              actions.push(new timelineActions.AddBands(action.source.id, newBands));
            }

            if (bandIdsToPointsKeys.length > 0) {
              actions.push(new timelineActions.AddPointsToBands(action.source.id, bandIdsToPoints));
            }

            actions.push(new sourceExplorerActions.SourceExplorerOpen(action.source));
          }

          return actions;
        })
        .catch(() => [
          // TODO: Dispatch error action.
        ]),
    );

  @Effect()
  sourceExplorerCloseWithRemoveBands$: Observable<Action> = this.actions$
    .ofType<sourceExplorerActions.SourceExplorerCloseWithRemoveBands>(SourceExplorerActionTypes.SourceExplorerCloseWithRemoveBands)
    .withLatestFrom(this.store$)
    .map(([action, state]) => ({ action, state }))
    .switchMap(({ state, action }) => {
      const actions: (SourceExplorerAction | TimelineAction)[] = [];
      const { removeBandIds = [], removePointsBandIds = [] } = removeBandsOrPoints(action.source.id, state.timeline.bands);

      if (removeBandIds.length > 0 || removePointsBandIds.length > 0) {
        if (removeBandIds.length > 0) {
          actions.push(new timelineActions.RemoveBands(action.source.id, removeBandIds));
        }

        if (removePointsBandIds.length > 0) {
          actions.push(new timelineActions.RemovePointsFromBands(action.source.id, removePointsBandIds));
        }

        actions.push(new sourceExplorerActions.SourceExplorerClose(action.source));
      }

      return actions;
    })
    .catch(() => [
      // TODO: Dispatch error action.
    ]);

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
    private mpsServerApi: MpsServerApiService,
  ) {}
}
