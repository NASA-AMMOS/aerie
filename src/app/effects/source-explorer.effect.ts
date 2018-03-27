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
import { Action, Store } from '@ngrx/store';

import { Observable } from 'rxjs/Observable';

import { forkJoin } from 'rxjs/observable/forkJoin';
import { of } from 'rxjs/observable/of';

import {
  catchError,
  concatMap,
  map,
  withLatestFrom,
} from 'rxjs/operators';

import { AppState } from './../../app/store';

import {
  FetchInitialSources,
  SourceExplorerActionTypes,
  SourceExplorerCloseEvent,
  SourceExplorerCollapseEvent,
  SourceExplorerExpandEvent,
  SourceExplorerOpenEvent,
} from './../actions/source-explorer';

import * as sourceExplorerActions from './../actions/source-explorer';
import * as timelineActions from './../actions/timeline';

import {
  hasActivityByTypeBand,
  isAddTo,
  isOverlay,
  toCompositeBand,
  toRavenBandData,
  toRavenSources,
} from './../shared/util';

import {
  MpsServerGraphData,
  MpsServerSource,
  RavenSource,
  RavenSubBand,
} from './../shared/models';

@Injectable()
export class SourceExplorerEffects {
  @Effect()
  fetchInitialSources$: Observable<Action> = this.actions$.pipe(
    ofType<FetchInitialSources>(SourceExplorerActionTypes.FetchInitialSources),
    withLatestFrom(this.store$),
    map(([action, state]) => state),
    concatMap((state: AppState) => [
      this.fetchSources(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`, '/', true),
      of(new sourceExplorerActions.UpdateSourceExplorer({
        fetchPending: false,
        initialSourcesLoaded: true,
      })),
    ]),
    concatMap(actions => actions),
  );

  @Effect()
  sourceExplorerCloseEvent$: Observable<Action> = this.actions$.pipe(
    ofType<SourceExplorerCloseEvent>(SourceExplorerActionTypes.SourceExplorerCloseEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) => {
      return [
        new timelineActions.RemoveBandsOrPointsForSource(action.sourceId),
        new sourceExplorerActions.UpdateTreeSource(action.sourceId, 'subBandIds', {}),
        new sourceExplorerActions.UpdateTreeSource(action.sourceId, 'opened', false),
      ];
    }),
  );

  @Effect()
  sourceExplorerCollapseEvent$: Observable<Action> = this.actions$.pipe(
    ofType<SourceExplorerCollapseEvent>(SourceExplorerActionTypes.SourceExplorerCollapseEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => action),
    map(action => new sourceExplorerActions.UpdateTreeSource(action.sourceId, 'expanded', false)),
  );

  @Effect()
  sourceExplorerExpandEvent$: Observable<Action> = this.actions$.pipe(
    ofType<SourceExplorerExpandEvent>(SourceExplorerActionTypes.SourceExplorerExpandEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) =>
      this.sourceExplorerExpand(state, action),
    ),
    concatMap(actions => actions),
  );

  @Effect()
  sourceExplorerOpenEvent$: Observable<Action> = this.actions$.pipe(
    ofType<SourceExplorerOpenEvent>(SourceExplorerActionTypes.SourceExplorerOpenEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) =>
      forkJoin([
        of(state),
        of(action),
        this.fetchBands(state.sourceExplorer.treeBySourceId[action.sourceId].url, action.sourceId),
      ]),
    ),
    map(([state, action, bands]) => ({ state, action, bands })),
    concatMap(({ state, action, bands }) =>
      this.sourceExplorerOpen(state, action, bands),
    ),
  );

  constructor(
    private http: HttpClient,
    private actions$: Actions,
    private store$: Store<AppState>,
  ) {}

  /**
   * Helper. Returns a stream of actions that need to occur when expanding a source explorer source.
   */
  sourceExplorerExpand(state: AppState, action: SourceExplorerExpandEvent): Observable<Action>[] {
    const sourceId = action.sourceId;
    const source = state.sourceExplorer.treeBySourceId[sourceId];
    const actions: Observable<Action>[] = [];

    if (!source.childIds.length) {
      if (source.content.length > 0) {
        actions.push(
          of(new sourceExplorerActions.NewSources(sourceId, toRavenSources(sourceId, false, source.content))),
        );
      } else {
        actions.push(
          this.fetchSources(source.url, sourceId, false),
        );
      }
    }

    actions.push(
      of(new sourceExplorerActions.UpdateTreeSource(sourceId, 'expanded', true)),
      of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
    );

    return actions;
  }

  /**
   * Helper. Returns a stream of actions that need to occur when opening a source explorer source.
   */
  sourceExplorerOpen(state: AppState, action: SourceExplorerOpenEvent, bands: RavenSubBand[]): Action[] {
    const actions: Action[] = [];

    bands.forEach((subBand: RavenSubBand) => {
      const activityByTypeBand = hasActivityByTypeBand(state.timeline.bands, subBand);

      if (activityByTypeBand) {
        actions.push(
          new sourceExplorerActions.SubBandIdAdd(action.sourceId, activityByTypeBand.subBandId),
          new timelineActions.AddPointsToSubBand(action.sourceId, activityByTypeBand.bandId, activityByTypeBand.subBandId, subBand.points),
        );
      } else if (isAddTo(state.timeline.bands, state.timeline.selectedBandId, state.timeline.selectedSubBandId, subBand.type)) {
        actions.push(
          new sourceExplorerActions.SubBandIdAdd(action.sourceId, state.timeline.selectedSubBandId),
          new timelineActions.AddPointsToSubBand(action.sourceId, state.timeline.selectedBandId, state.timeline.selectedSubBandId, subBand.points),
        );
      } else if (isOverlay(state.timeline.bands, state.timeline.selectedBandId)) {
        actions.push(
          new sourceExplorerActions.SubBandIdAdd(action.sourceId, subBand.id),
          new timelineActions.AddSubBand(action.sourceId, state.timeline.selectedBandId, subBand),
        );
      } else {
        actions.push(
          new sourceExplorerActions.SubBandIdAdd(action.sourceId, subBand.id),
          new timelineActions.AddBand(action.sourceId, toCompositeBand(action.sourceId, subBand)),
        );
      }
    });

    actions.push(
      new sourceExplorerActions.UpdateTreeSource(action.sourceId, 'opened', true),
      new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false }),
    );

    return actions;
  }

  /**
   * Fetch helper. Fetches graph data from MPS Server and maps it to Raven band data.
   */
  fetchBands(sourceUrl: string, sourceId: string) {
    return this.http.get<MpsServerGraphData>(sourceUrl).pipe(
      map((graphData: MpsServerGraphData) => toRavenBandData(sourceId, graphData)),
      catchError(e => {
        console.error('SourceExplorerEffects - fetchGraphData error: ', e);
        return of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false }));
      }),
    );
  }

  /**
   * Fetch helper. Fetches sources from MPS Server and maps them to Raven sources.
   */
  fetchSources(url: string, sourceId: string, isServer: boolean) {
    return this.http.get<MpsServerSource[]>(url).pipe(
      map((mpsServerSources: MpsServerSource[]) => toRavenSources(sourceId, isServer, mpsServerSources)),
      map((sources: RavenSource[]) => new sourceExplorerActions.NewSources(sourceId, sources)),
      catchError(e => {
        console.error('SourceExplorerEffects - fetchSources error: ', e);
        return of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false }));
      }),
    );
  }
}
