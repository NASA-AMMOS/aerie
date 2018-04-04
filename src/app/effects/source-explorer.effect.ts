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

import { combineLatest } from 'rxjs/observable/combineLatest';
import { concat } from 'rxjs/observable/concat';
import { forkJoin } from 'rxjs/observable/forkJoin';
import { of } from 'rxjs/observable/of';

import {
  catchError,
  concatMap,
  map,
  take,
  withLatestFrom,
} from 'rxjs/operators';

import { AppState } from './../../app/store';

import {
  CloseEvent,
  CollapseEvent,
  ExpandEvent,
  FetchInitialSources,
  LoadFromSource,
  OpenEvent,
  RemoveSourceEvent,
  SaveToSource,
  SourceExplorerActionTypes,
} from './../actions/source-explorer';

import * as sourceExplorerActions from './../actions/source-explorer';
import * as timelineActions from './../actions/timeline';

import * as fromSourceExplorer from './../reducers/source-explorer';
import * as fromTimeline from './../reducers/timeline';

import {
  getSourceIds,
  hasActivityByTypeBand,
  hasSourceId,
  isAddTo,
  isOverlay,
  toCompositeBand,
  toRavenBandData,
  toRavenSources,
} from './../shared/util';

import {
  MpsServerGraphData,
  MpsServerSource,
  RavenCompositeBand,
  RavenSource,
  RavenSubBand,
} from './../shared/models';

@Injectable()
export class SourceExplorerEffects {
  @Effect()
  closeEvent$: Observable<Action> = this.actions$.pipe(
    ofType<CloseEvent>(SourceExplorerActionTypes.CloseEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) => {
      return [
        new timelineActions.RemoveBandsOrPointsForSource(action.sourceId),
        new sourceExplorerActions.UpdateTreeSource(action.sourceId, {
          opened: false,
          subBandIds: {},
        }),
      ];
    }),
  );

  @Effect()
  collapseEvent$: Observable<Action> = this.actions$.pipe(
    ofType<CollapseEvent>(SourceExplorerActionTypes.CollapseEvent),
    map(action => new sourceExplorerActions.UpdateTreeSource(action.sourceId, { expanded: false })),
  );

  @Effect()
  expandEvent$: Observable<Action> = this.actions$.pipe(
    ofType<ExpandEvent>(SourceExplorerActionTypes.ExpandEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => state.sourceExplorer.treeBySourceId[action.sourceId]),
    concatMap(source =>
      concat(
        ...this.expand(source),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
      ),
    ),
  );

  @Effect()
  fetchInitialSources$: Observable<Action> = this.actions$.pipe(
    ofType<FetchInitialSources>(SourceExplorerActionTypes.FetchInitialSources),
    withLatestFrom(this.store$),
    map(([action, state]) => state),
    concatMap((state: AppState) =>
      concat(
        this.fetchNewSources(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`, '/', true).pipe(
          map((sources: RavenSource[]) => new sourceExplorerActions.NewSources('/', sources) as Action),
        ),
        of(new sourceExplorerActions.UpdateSourceExplorer({
          fetchPending: false,
          initialSourcesLoaded: true,
        })),
      ),
    ),
  );

  @Effect()
  loadFromSource$: Observable<Action> = this.actions$.pipe(
    ofType<LoadFromSource>(SourceExplorerActionTypes.LoadFromSource),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      forkJoin([
        of(action),
        this.fetchSavedState(action.sourceUrl),
        this.fetchNewSources(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`, '/', true),
      ]),
    ),
    map(([action, state, sources]) => ({ action, state, sources })),
    concatMap(({ action, state: { bands }, sources }) =>
      concat(
        ...this.load(bands, sources),
      ),
    ),
  );

  @Effect()
  openEvent$: Observable<Action> = this.actions$.pipe(
    ofType<OpenEvent>(SourceExplorerActionTypes.OpenEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) =>
      forkJoin([
        of(state),
        of(action),
        this.fetchBands(state.sourceExplorer.treeBySourceId[action.sourceId].url, action.sourceId),
      ]),
    ),
    map(([state, action, newSubBands]) => ({ state, action, newSubBands })),
    concatMap(({ state: { timeline }, action: { sourceId }, newSubBands }) =>
      concat(
        ...this.open(sourceId, timeline.bands, timeline.selectedBandId, timeline.selectedSubBandId, newSubBands),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
      ),
    ),
  );

  @Effect()
  removeSourceEvent$: Observable<Action> = this.actions$.pipe(
    ofType<RemoveSourceEvent>(SourceExplorerActionTypes.RemoveSourceEvent),
    concatMap(action =>
      concat(
        this.removeSource(action.source.url, action.source.id),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
      ),
    ),
  );

  @Effect()
  saveToSource$: Observable<Action> = this.actions$.pipe(
    ofType<SaveToSource>(SourceExplorerActionTypes.SaveToSource),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) =>
      concat(
        this.saveToSource(action.source.url, action.source.id, action.name, {
          name: `raven2-state-${action.name}`,
          state: {
            bands: state.timeline.bands.map(band => ({
              ...band,
              subBands: band.subBands.map(subBand => ({
                ...subBand,
                points: [],
              })),
            })),
          },
        }),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
      ),
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
  expand(source: RavenSource): Observable<Action>[] {
    const actions: Observable<Action>[] = [];

    if (source) {
      if (!source.childIds.length) {
        if (source.content.length > 0) {
          actions.push(
            of(new sourceExplorerActions.NewSources(source.id, toRavenSources(source.id, false, source.content))),
          );
        } else {
          actions.push(
            this.fetchNewSources(source.url, source.id, false).pipe(
              map((sources: RavenSource[]) => new sourceExplorerActions.NewSources(source.id, sources)),
            ),
          );
        }
      }

      actions.push(
        of(new sourceExplorerActions.UpdateTreeSource(source.id, { expanded: true })),
      );
    }

    return actions;
  }

  /**
   * Helper. Returns a stream of actions that need to occur when loading a state.
   */
  load(bands: RavenCompositeBand[], initialSources: RavenSource[]): Observable<Action>[] {
    const { parentSourceIds, sourceIds } = getSourceIds(bands);

    return [
      of(new sourceExplorerActions.UpdateSourceExplorer({
        ...fromSourceExplorer.initialState,
        fetchPending: true,
      })),
      of(new timelineActions.UpdateTimeline({
        ...fromTimeline.initialState,
        bands,
      })),
      of(new sourceExplorerActions.NewSources('/', initialSources)),
      ...parentSourceIds.map((sourceId: string) =>
        combineLatest(this.store$, state => state.sourceExplorer.treeBySourceId[sourceId]).pipe(
          take(1),
          concatMap(source =>
            concat(
              ...this.expand(source),
            ),
          ),
        ),
      ),
      ...sourceIds.map((sourceId: string) =>
        combineLatest(this.store$, state => state).pipe(
          take(1),
          concatMap(state =>
            forkJoin([
              of(state),
              this.fetchBands(state.sourceExplorer.treeBySourceId[sourceId].url, sourceId),
            ]),
          ),
          map(([state, newBands]) => ({ state, newBands })),
          concatMap(({ state: { timeline }, newBands }) =>
            concat(
              ...this.open(sourceId, bands, null, null, newBands),
            ),
          ),
        ),
      ),
      of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
    ];
  }

  /**
   * Helper. Returns a stream of actions that need to occur when opening a source explorer source.
   * The order of the cases in this function are very important. Do not change the order.
   */
  open(sourceId: string, currentBands: RavenCompositeBand[], bandId: string | null, subBandId: string | null, newSubBands: RavenSubBand[]): Observable<Action>[] {
    const actions: Observable<Action>[] = [];

    newSubBands.forEach((subBand: RavenSubBand) => {
      const activityByTypeBand = hasActivityByTypeBand(currentBands, subBand);
      const existingBand = hasSourceId(currentBands, sourceId);

      if (activityByTypeBand) {
        actions.push(
          of(new sourceExplorerActions.SubBandIdAdd(sourceId, activityByTypeBand.subBandId)),
          of(new timelineActions.AddPointsToSubBand(sourceId, activityByTypeBand.bandId, activityByTypeBand.subBandId, subBand.points)),
        );
      } else if (existingBand) {
        actions.push(
          of(new sourceExplorerActions.SubBandIdAdd(sourceId, existingBand.subBandId)),
          of(new timelineActions.AddPointsToSubBand(sourceId, existingBand.bandId, existingBand.subBandId, subBand.points)),
        );
      } else if (bandId && subBandId && isAddTo(currentBands, bandId, subBandId, subBand.type)) {
        actions.push(
          of(new sourceExplorerActions.SubBandIdAdd(sourceId, subBandId)),
          of(new timelineActions.AddPointsToSubBand(sourceId, bandId, subBandId, subBand.points)),
        );
      } else if (bandId && isOverlay(currentBands, bandId)) {
        actions.push(
          of(new sourceExplorerActions.SubBandIdAdd(sourceId, subBand.id)),
          of(new timelineActions.AddSubBand(sourceId, bandId, subBand)),
        );
      } else {
        actions.push(
          of(new sourceExplorerActions.SubBandIdAdd(sourceId, subBand.id)),
          of(new timelineActions.AddBand(sourceId, toCompositeBand(sourceId, subBand))),
        );
      }
    });

    actions.push(
      of(new sourceExplorerActions.UpdateTreeSource(sourceId, { opened: true })),
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
        console.error('SourceExplorerEffects - fetchBands error: ', e);
        return of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false }));
      }),
    );
  }

  /**
   * Fetch helper. Fetches sources from MPS Server and maps them to Raven sources.
   */
  fetchNewSources(url: string, parentId: string, isServer: boolean) {
    return this.http.get<MpsServerSource[]>(url).pipe(
      map((mpsServerSources: MpsServerSource[]) => toRavenSources(parentId, isServer, mpsServerSources)),
      catchError(e => {
        console.error('SourceExplorerEffects - fetchNewSources error: ', e);
        return of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false }));
      }),
    );
  }

  /**
   * Fetch helper. Fetches saved state from MPS Server.
   */
  fetchSavedState(url: string) {
    return this.http.get(url).pipe(
      map(res => res[0].state),
      catchError(e => {
        console.error('SourceExplorerEffects - fetchSavedState error: ', e);
        return of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false }));
      }),
    );
  }

  /**
   * Fetch helper. Deletes a source from MPS Server.
   */
  removeSource(sourceUrl: string, sourceId: string) {
    // TODO: Make this better so we don't have to change the URL.
    const url = sourceUrl.replace(/(list_)?generic-mongodb/i, 'fs-mongodb');

    return this.http.delete(url, { responseType: 'text' }).pipe(
      map(() => new sourceExplorerActions.RemoveSource(sourceId)),
      catchError(e => {
        console.error('SourceExplorerEffects - removeSource error: ', e);
        return of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false }));
      }),
    );
  }

  /**
   * Helper. Save some data to an MPS Server source.
   *
   * TODO: Replace 'any' with a concrete type.
   */
  saveToSource(sourceUrl: string, sourceId: string, name: string, data: any) {
    return this.http.put(`${sourceUrl}/${name}`, data).pipe(
      concatMap(() =>
        this.fetchNewSources(sourceUrl, sourceId, false).pipe(
          map((sources: RavenSource[]) => new sourceExplorerActions.NewSources(sourceId, sources)),
        ),
      ),
      catchError(e => {
        console.error('SourceExplorerEffects - saveToSource error: ', e);
        return of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false }));
      }),
    );
  }
}
