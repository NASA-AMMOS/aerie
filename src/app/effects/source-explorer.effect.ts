/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';

import { Observable } from 'rxjs/Observable';

import { combineLatest } from 'rxjs/observable/combineLatest';
import { concat } from 'rxjs/observable/concat';
import { forkJoin } from 'rxjs/observable/forkJoin';
import { merge } from 'rxjs/observable/merge';
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
  ApplyLayout,
  ApplyState,
  CloseEvent,
  ExpandEvent,
  FetchInitialSources,
  FetchNewSources,
  ImportFile,
  OpenEvent,
  RemoveSourceEvent,
  SaveState,
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
  RavenDefaultSettings,
  RavenSource,
  RavenSubBand,
  RavenTimeRange,
  StringTMap,
} from './../shared/models';

@Injectable()
export class SourceExplorerEffects {
  @Effect()
  applyLayout$: Observable<Action> = this.actions$.pipe(
    ofType<ApplyLayout>(SourceExplorerActionTypes.ApplyLayout),
  );

  @Effect()
  applyState$: Observable<Action> = this.actions$.pipe(
    ofType<ApplyState>(SourceExplorerActionTypes.ApplyState),
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
    concatMap(({ action, state: { bands, labelWidth, maxTimeRange, viewTimeRange }, sources }) =>
      concat(
        ...this.load(bands, labelWidth, maxTimeRange, viewTimeRange, sources),
      ),
    ),
  );

  @Effect()
  closeEvent$: Observable<Action> = this.actions$.pipe(
    ofType<CloseEvent>(SourceExplorerActionTypes.CloseEvent),
    map(action => new timelineActions.RemoveBandsOrPointsForSource(action.sourceId)),
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
      ).pipe(
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - expandEvent$: ', e);
          return merge(
            of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
            of(new sourceExplorerActions.UpdateTreeSource(source.id, { expanded: false })),
          );
        }),
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
      ).pipe(
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - fetchInitialSources$: ', e);
          return of(new sourceExplorerActions.UpdateSourceExplorer({
            fetchPending: false,
            initialSourcesLoaded: false,
          }));
        }),
      ),
    ),
  );

  @Effect()
  fetchNewSources$: Observable<Action> = this.actions$.pipe(
    ofType<FetchNewSources>(SourceExplorerActionTypes.FetchNewSources),
    concatMap(action =>
      concat(
        this.fetchNewSources(action.sourceUrl, action.sourceId, false).pipe(
          map((sources: RavenSource[]) => new sourceExplorerActions.NewSources(action.sourceId, sources)),
        ),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
      ).pipe(
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - fetchNewSources$: ', e);
          return of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false }));
        }),
      ),
    ),
  );

  @Effect()
  importFile$: Observable<Action> = this.actions$.pipe(
    ofType<ImportFile>(SourceExplorerActionTypes.ImportFile),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) => {
      const headers = new HttpHeaders().set('Content-Type', 'text/csv');
      const url = `${action.source.url}/${action.file.name}?timeline_type=${action.file.type}`;

      return this.http.put(url, action.file.data, { headers: headers, responseType: 'text' }).pipe(
        concatMap(() => {
          if (action.file.mapping) {
            return this.importMappingFile(action.source.url, action.file.name, action.file.mapping).pipe(
              map(() => new sourceExplorerActions.ImportFileSuccess()),
            );
          } else {
            // No need to send mapping data.
            return of(new sourceExplorerActions.ImportFileSuccess());
          }
        }),
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - importFile$: ', e);
          return of(new sourceExplorerActions.ImportFileFailure());
        }),
      );
    }),
  );

  @Effect()
  openEvent$: Observable<Action> = this.actions$.pipe(
    ofType<OpenEvent>(SourceExplorerActionTypes.OpenEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) =>
      concat(
        this.open(state.sourceExplorer.treeBySourceId, action.sourceId, state.timeline.bands, state.timeline.selectedBandId, state.timeline.selectedSubBandId, state.timeline.defaultSettings),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
      ).pipe(
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - openEvent$: ', e);
          return merge(
            of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
            of(new sourceExplorerActions.UpdateTreeSource(action.sourceId, { opened: false })),
          );
        }),
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
  saveState$: Observable<Action> = this.actions$.pipe(
    ofType<SaveState>(SourceExplorerActionTypes.SaveState),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) =>
      concat(
        this.saveState(action.source.url, action.source.id, action.name, {
          name: `raven2-state-${action.name}`,
          state: {
            bands: state.timeline.bands.map(band => ({
              ...band,
              subBands: band.subBands.map(subBand => ({
                ...subBand,
                points: [],
              })),
            })),
            labelWidth: state.timeline.labelWidth,
            maxTimeRange: state.timeline.maxTimeRange,
            pins: state.sourceExplorer.pins,
            viewTimeRange: state.timeline.viewTimeRange,
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
    }

    return actions;
  }

  /**
   * Helper. Returns a stream of actions that need to occur when loading a state.
   */
  load(
    bands: RavenCompositeBand[],
    labelWidth: number,
    maxTimeRange: RavenTimeRange,
    viewTimeRange: RavenTimeRange,
    initialSources: RavenSource[],
  ): Observable<Action>[] {
    const { parentSourceIds, sourceIds } = getSourceIds(bands);

    return [
      of(new sourceExplorerActions.UpdateSourceExplorer({
        ...fromSourceExplorer.initialState,
        fetchPending: true,
      })),
      of(new timelineActions.UpdateTimeline({
        ...fromTimeline.initialState,
        bands,
        labelWidth,
        maxTimeRange,
        viewTimeRange,
      })),
      of(new sourceExplorerActions.NewSources('/', initialSources)),
      ...parentSourceIds.map((sourceId: string) =>
        combineLatest(this.store$, state => state.sourceExplorer.treeBySourceId[sourceId]).pipe(
          take(1),
          concatMap(source =>
            concat(
              ...this.expand(source),
              of(new sourceExplorerActions.UpdateTreeSource(source.id, { expanded: true })),
            ),
          ),
        ),
      ),
      ...sourceIds.map((sourceId: string) =>
        combineLatest(this.store$, state => state).pipe(
          take(1),
          concatMap(state =>
            concat(
              this.open(state.sourceExplorer.treeBySourceId, sourceId, bands, null, null, state.timeline.defaultSettings),
              of(new sourceExplorerActions.UpdateTreeSource(sourceId, { opened: true })),
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
  open(treeBySourceId: StringTMap<RavenSource>, sourceId: string, currentBands: RavenCompositeBand[], bandId: string | null, subBandId: string | null, defaultSettings: RavenDefaultSettings) {
    return this.fetchSubBands(treeBySourceId[sourceId].url, sourceId, defaultSettings).pipe(
      concatMap((newSubBands: RavenSubBand[]) => {
        const actions: Action[] = [];

        newSubBands.forEach((subBand: RavenSubBand) => {
          const activityByTypeBand = hasActivityByTypeBand(currentBands, subBand);
          const existingBand = hasSourceId(currentBands, sourceId);

          if (activityByTypeBand) {
            actions.push(
              new sourceExplorerActions.SubBandIdAdd(sourceId, activityByTypeBand.subBandId),
              new timelineActions.AddPointsToSubBand(sourceId, activityByTypeBand.bandId, activityByTypeBand.subBandId, subBand.points),
            );
          } else if (existingBand) {
            actions.push(
              new sourceExplorerActions.SubBandIdAdd(sourceId, existingBand.subBandId),
              new timelineActions.AddPointsToSubBand(sourceId, existingBand.bandId, existingBand.subBandId, subBand.points),
            );
          } else if (bandId && subBandId && isAddTo(currentBands, bandId, subBandId, subBand.type)) {
            actions.push(
              new sourceExplorerActions.SubBandIdAdd(sourceId, subBandId),
              new timelineActions.AddPointsToSubBand(sourceId, bandId, subBandId, subBand.points),
            );
          } else if (bandId && isOverlay(currentBands, bandId)) {
            actions.push(
              new sourceExplorerActions.SubBandIdAdd(sourceId, subBand.id),
              new timelineActions.AddSubBand(sourceId, bandId, subBand),
            );
          } else {
            actions.push(
              new sourceExplorerActions.SubBandIdAdd(sourceId, subBand.id),
              new timelineActions.AddBand(sourceId, toCompositeBand(subBand)),
            );
          }
        });

        return actions;
      }),
    );
  }

  /**
   * Fetch helper. Fetches graph data from MPS Server and maps it to Raven sub-band data.
   */
  fetchSubBands(sourceUrl: string, sourceId: string, defaultSettings: RavenDefaultSettings) {
    return this.http.get<MpsServerGraphData>(sourceUrl).pipe(
      map((graphData: MpsServerGraphData) => toRavenBandData(sourceId, graphData, defaultSettings)),
    );
  }

  /**
   * Fetch helper. Fetches sources from MPS Server and maps them to Raven sources.
   */
  fetchNewSources(url: string, parentId: string, isServer: boolean) {
    return this.http.get<MpsServerSource[]>(url).pipe(
      map((mpsServerSources: MpsServerSource[]) => toRavenSources(parentId, isServer, mpsServerSources)),
    );
  }

  /**
   * Fetch helper. Fetches saved state from MPS Server.
   */
  fetchSavedState(url: string) {
    return this.http.get(url).pipe(
      map(res => res[0].state),
    );
  }

  /**
   * Fetch helper. Deletes a source from MPS Server.
   */
  removeSource(sourceUrl: string, sourceId: string) {
    // TODO: Make this better so we don't have to change the URL.
    const url = sourceUrl.replace(/list_(generic|.*custom.*)-mongodb/i, 'fs-mongodb');

    return this.http.delete(url, { responseType: 'text' }).pipe(
      map(() => new sourceExplorerActions.RemoveSource(sourceId)),
    );
  }

  /**
   * Helper. Save state to an MPS Server source.
   * Fetches new sources and updates the source after the save.
   *
   * TODO: Replace 'any' with a concrete type.
   */
  saveState(sourceUrl: string, sourceId: string, name: string, data: any) {
    return this.http.put(`${sourceUrl}/${name}`, data).pipe(
      concatMap(() =>
        of(new sourceExplorerActions.FetchNewSources(sourceId, sourceUrl)),
      ),
    );
  }

  /**
   * Helper. Import mapping file into MPS Server for a given source URL.
   */
  importMappingFile(sourceUrl: string, name: string, mapping: string) {
    // TODO: Make this better so we don't have to change the URL.
    const url = sourceUrl.replace('fs-mongodb', 'metadata-mongodb');

    return this.http.post(`${url}/${name}`, mapping, { responseType: 'text' });
  }
}
