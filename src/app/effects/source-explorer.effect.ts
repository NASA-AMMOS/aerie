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

import {
  combineLatest,
  concat,
  forkJoin,
  merge,
  Observable,
  of,
} from 'rxjs';

import {
  catchError,
  concatMap,
  map,
  mergeMap,
  switchMap,
  take,
  withLatestFrom,
} from 'rxjs/operators';

import { AppState } from './../../app/store';

import {
  AddCustomGraph,
  AddGraphableFilter,
  ApplyLayout,
  ApplyState,
  CloseEvent,
  ExpandEvent,
  FetchInitialSources,
  FetchNewSources,
  GraphCustomSource,
  ImportFile,
  OpenEvent,
  RemoveSourceEvent,
  SaveState,
  SourceExplorerActionTypes,
  UpdateGraphAfterFilterAdd,
  UpdateGraphAfterFilterRemove,
} from './../actions/source-explorer';

import * as configActions from './../actions/config';
import * as dialogActions from './../actions/dialog';
import * as layoutActions from './../actions/layout';
import * as sourceExplorerActions from './../actions/source-explorer';
import * as timelineActions from './../actions/timeline';

import * as fromSourceExplorer from './../reducers/source-explorer';
import * as fromTimeline from './../reducers/timeline';

import {
  getCustomFilterForLabel,
  getCustomFiltersBySourceId,
  getFormattedSourceUrl,
  getSourceIds,
  getState,
  hasActivityBand,
  hasActivityBandForFilterTarget,
  hasSourceId,
  importState,
  isAddTo,
  isOverlay,
  toCompositeBand,
  toRavenBandData,
  toRavenSources,
  updateSourceId,
} from './../shared/util';

import {
  MpsServerGraphData,
  MpsServerSource,
  RavenCompositeBand,
  RavenCustomFilter,
  RavenCustomFilterSource,
  RavenDefaultBandSettings,
  RavenFilterSource,
  RavenGraphableFilterSource,
  RavenSource,
  RavenState,
  RavenSubBand,
  StringTMap,
} from './../shared/models';

@Injectable()
export class SourceExplorerEffects {
  /**
   * Effect for AddCustomGraph.
   */
  @Effect()
  addCustomGraph$: Observable<Action> = this.actions$.pipe(
    ofType<AddCustomGraph>(SourceExplorerActionTypes.AddCustomGraph),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state: { config, sourceExplorer, timeline }, action: { label, sourceId } }) =>
      concat(
        this.open(
          getCustomFilterForLabel(
            label,
            sourceExplorer.customFiltersBySourceId[sourceId],
          ),
          sourceExplorer.filtersByTarget,
          sourceExplorer.treeBySourceId,
          sourceId,
          timeline.bands,
          timeline.selectedBandId,
          timeline.selectedSubBandId,
          config.defaultBandSettings,
        ),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
      ),
    ),
  );

  /**
   * Effect for AddGraphableFilter.
   */
  @Effect()
  addGraphableFilter$: Observable<Action> = this.actions$.pipe(
    ofType<AddGraphableFilter>(SourceExplorerActionTypes.AddGraphableFilter),
    concatMap(action =>
      concat(
        of(new sourceExplorerActions.AddFilter(action.source)),
        of(new sourceExplorerActions.UpdateGraphAfterFilterAdd(action.source.id)),
      ),
    ),
  );

  /**
   * Effect for ApplyLayout.
   */
  @Effect()
  applyLayout$: Observable<Action> = this.actions$.pipe(
    ofType<ApplyLayout>(SourceExplorerActionTypes.ApplyLayout),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      forkJoin([
        of(action),
        this.fetchState(action.sourceUrl),
        this.fetchNewSources(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`, '/', true),
      ]),
    ),
    map(([action, savedState, initialSources]) => ({ action, savedState, initialSources })),
    concatMap(({ action, savedState, initialSources }) => {
      const updatedBands = savedState.bands.map((band: RavenCompositeBand) => ({
        ...band,
        subBands: band.subBands.map((subBand: RavenSubBand) => ({
          ...subBand,
          sourceIds: subBand.sourceIds.map(sourceId => updateSourceId(sourceId, action.targetSourceId)),
        })),
      }));

      return concat(
        ...this.loadLayout(
          updatedBands,
          initialSources,
          savedState,
        ),
      );
    }),
  );

  /**
   * Effect for ApplyState.
   */
  @Effect()
  applyState$: Observable<Action> = this.actions$.pipe(
    ofType<ApplyState>(SourceExplorerActionTypes.ApplyState),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      forkJoin([
        this.fetchState(action.sourceUrl),
        this.fetchNewSources(`${state.config.baseUrl}/${state.config.baseSourcesUrl}`, '/', true),
      ]),
    ),
    map(([savedState, initialSources]) => ({ savedState, initialSources })),
    concatMap(({ savedState, initialSources }) =>
      concat(
        ...this.loadState(initialSources, savedState),
      ),
    ),
  );

  /**
   * Effect for CloseEvent.
   */
  @Effect()
  closeEvent$: Observable<Action> = this.actions$.pipe(
    ofType<CloseEvent>(SourceExplorerActionTypes.CloseEvent),
    switchMap(action => [
      new timelineActions.RemoveBandsOrPointsForSource(action.sourceId),
      new sourceExplorerActions.UpdateTreeSource(action.sourceId, { opened: false }),
      new layoutActions.Resize(), // Resize bands when we `close` to make sure they are all resized properly.
    ]),
  );

  /**
   * Effect for ExpandEvent.
   */
  @Effect()
  expandEvent$: Observable<Action> = this.actions$.pipe(
    ofType<ExpandEvent>(SourceExplorerActionTypes.ExpandEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => state.sourceExplorer.treeBySourceId[action.sourceId]),
    switchMap(source =>
      concat(
        ...this.expand(source),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
        of(new sourceExplorerActions.UpdateTreeSource(source.id, { expanded: true })),
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

  /**
   * Effect for FetchInitialSources.
   */
  @Effect()
  fetchInitialSources$: Observable<Action> = this.actions$.pipe(
    ofType<FetchInitialSources>(SourceExplorerActionTypes.FetchInitialSources),
    withLatestFrom(this.store$),
    map(([, state]) => state),
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

  /**
   * Effect for FetchInitialSources.
   */
  @Effect()
  fetchNewSources$: Observable<Action> = this.actions$.pipe(
    ofType<FetchNewSources>(SourceExplorerActionTypes.FetchNewSources),
    concatMap(action =>
      concat(
        this.fetchNewSources(action.sourceUrl, action.sourceId, false).pipe(
          concatMap((sources: RavenSource[]) => [
            new sourceExplorerActions.NewSources(action.sourceId, sources),
          ]),
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

  /**
   * Effect for GraphCustomSource.
   */
  @Effect()
  graphCustomSource$: Observable<Action> = this.actions$.pipe(
    ofType<GraphCustomSource>(SourceExplorerActionTypes.GraphCustomSource),
    concatMap(action =>
      concat(
        of(new sourceExplorerActions.AddCustomFilter(action.sourceId, action.label, action.filter)),
        of(new sourceExplorerActions.AddCustomGraph(action.sourceId, action.label, action.filter)),
      ),
    ),
  );

  /**
   * Effect for ImportFile.
   */
  @Effect()
  importFile$: Observable<Action> = this.actions$.pipe(
    ofType<ImportFile>(SourceExplorerActionTypes.ImportFile),
    concatMap(action => {
      const headers = new HttpHeaders().set('Content-Type', `${action.file.type === 'pef' ? 'application/json' : 'text/csv'}`);
      const url = `${action.source.url}/${action.file.name}?timeline_type=${action.file.type}`;

      return this.http.put(url, action.file.data, { headers: headers, responseType: 'text' }).pipe(
        concatMap(() => {
          if (action.file.mapping) {
            return this.importMappingFile(action.source.url, action.file.name, action.file.mapping).pipe(
              map(() => new sourceExplorerActions.ImportFileSuccess()),
            );
          } else {
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

  /**
   * Effect for OpenEvent.
   */
  @Effect()
  openEvent$: Observable<Action> = this.actions$.pipe(
    ofType<OpenEvent>(SourceExplorerActionTypes.OpenEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    mergeMap(({ state, action }) =>
      concat(
        this.open(
          null,
          state.sourceExplorer.filtersByTarget,
          state.sourceExplorer.treeBySourceId,
          action.sourceId,
          state.timeline.bands,
          state.timeline.selectedBandId,
          state.timeline.selectedSubBandId,
          state.config.defaultBandSettings,
        ),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
        of(new sourceExplorerActions.UpdateTreeSource(action.sourceId, { opened: true })),
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

  /**
   * Effect for RemoveGraphableFilter.
   */
  @Effect()
  removeGraphableFilter$: Observable<Action> = this.actions$.pipe(
    ofType<sourceExplorerActions.RemoveGraphableFilter>(SourceExplorerActionTypes.RemoveGraphableFilter),
    concatMap(action =>
      concat(
        of(new sourceExplorerActions.RemoveFilter(action.source as RavenFilterSource)),
        of(new sourceExplorerActions.SubBandIdRemove([action.source.id], action.source.subBandIds[0])),
        of(new sourceExplorerActions.UpdateGraphAfterFilterRemove(action.source.id)),
      ),
    ),
  );

  /**
   * Effect for RemoveSourceEvent.
   */
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

  /**
   * Effect for SaveState.
   */
  @Effect()
  saveState$: Observable<Action> = this.actions$.pipe(
    ofType<SaveState>(SourceExplorerActionTypes.SaveState),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) =>
      concat(
        this.saveState(action.source.url, action.source.id, action.name, state),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
      ),
    ),
  );

  /**
   * Effect for SaveState.
   */
  @Effect()
  updateGraphAfterFilterAdd$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateGraphAfterFilterAdd>(SourceExplorerActionTypes.UpdateGraphAfterFilterAdd),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state: { config, sourceExplorer, timeline: { bands } }, action }) =>
      concat(
        this.fetchSubBands(sourceExplorer.treeBySourceId, action.sourceId, config.defaultBandSettings, null, sourceExplorer.filtersByTarget).pipe(
          withLatestFrom(this.store$),
          map(([newSubBands, state]) => ({ newSubBands, state })),
          concatMap(({ newSubBands, state: { timeline } }) => {
            const actions: Action[] = [];
            const filterTarget = (sourceExplorer.treeBySourceId[action.sourceId] as RavenFilterSource).filterTarget;

            newSubBands.forEach((subBand: RavenSubBand) => {
              const activityBand = hasActivityBandForFilterTarget(timeline.bands, filterTarget);
              if (activityBand) {
                actions.push(
                  // Add filterSource id to existing band.
                  new timelineActions.SourceIdAdd(action.sourceId, activityBand.subBandId),
                  // Replace points in band.
                  new timelineActions.SetPointsForSubBand(activityBand.bandId, activityBand.subBandId, subBand.points),
                );
              } else {
                actions.push(
                  new sourceExplorerActions.SubBandIdAdd(action.sourceId, subBand.id),
                  new timelineActions.AddBand(action.sourceId, toCompositeBand(subBand), { filterTarget: (sourceExplorer.treeBySourceId[action.sourceId] as RavenFilterSource).filterTarget }),
                );
              }
            });

            return actions;
          }),
        ),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
      ),
    ),
  );

  /**
   * Effect for UpdateGraphAfterFilterRemove.
   */
  @Effect()
  updateGraphAfterFilterRemove$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateGraphAfterFilterRemove>(SourceExplorerActionTypes.UpdateGraphAfterFilterRemove),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state: { config, sourceExplorer, timeline: { bands } }, action }) =>
      concat(
        this.fetchSubBands(sourceExplorer.treeBySourceId, action.sourceId, config.defaultBandSettings, null, sourceExplorer.filtersByTarget).pipe(
          concatMap((newSubBands: RavenSubBand[]) => {
            const actions: Action[] = [];
            const target = (sourceExplorer.treeBySourceId[action.sourceId] as RavenFilterSource).filterTarget;

            if (newSubBands.length) {
              newSubBands.forEach((subBand: RavenSubBand) => {
                const activityBand = hasActivityBandForFilterTarget(bands, target);

                if (activityBand) {
                  actions.push(
                    // Remove sourceId from band.
                    new timelineActions.RemoveSourceIdFromSubBands(action.sourceId),
                    // Replace band points.
                    new timelineActions.SetPointsForSubBand(activityBand.bandId, activityBand.subBandId, subBand.points),
                  );
                }
              });
            } else { // When the last graphable filter source is unselected.
              const activityBand = hasActivityBandForFilterTarget(bands, target);

              if (activityBand) {
                actions.push(
                  // Remove sourceId from band.
                  new timelineActions.RemoveSourceIdFromSubBands(action.sourceId),
                  // Set empty data points.
                  new timelineActions.SetPointsForSubBand(activityBand.bandId, activityBand.subBandId, []),
                );
              }
            }

            return actions;
          }),
        ),
        of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
      ),
    ),
  );

  constructor(
    private actions$: Actions,
    private http: HttpClient,
    private store$: Store<AppState>,
  ) { }

  /**
   * Helper. Returns a stream of actions that need to occur when expanding a source explorer source.
   */
  expand(source: RavenSource): Observable<Action>[] {
    const actions: Observable<Action>[] = [];

    if (source && !source.childIds.length) {
      if (source.content) {
        actions.push(
          of(new sourceExplorerActions.NewSources(source.id, toRavenSources(source.id, false, source.content))),
        );
      } else {
        actions.push(
          this.fetchNewSources(source.url, source.id, false).pipe(
            concatMap((sources: RavenSource[]) => [
              new sourceExplorerActions.NewSources(source.id, sources), // Add new sources to the source-explorer.
            ]),
          ),
        );
      }
    }

    return actions;
  }

  /**
   * Fetch helper. Fetches graph data from MPS Server and maps it to Raven sub-band data.
   */
  fetchSubBands(
    treeBySourceId: StringTMap<RavenSource>,
    sourceId: string,
    defaultBandSettings: RavenDefaultBandSettings,
    customFilter: RavenCustomFilter | null,
    filtersByTarget: StringTMap<StringTMap<string[]>>,
  ) {
    const source = treeBySourceId[sourceId];
    return this.http.get<MpsServerGraphData>(getFormattedSourceUrl(treeBySourceId, source, customFilter, filtersByTarget)).pipe(
      map((graphData: MpsServerGraphData) => toRavenBandData(sourceId, source.name, graphData, defaultBandSettings, customFilter, treeBySourceId)),
    );
  }

  /**
   * Helper. Returns a stream of actions that need to occur when loading a layout.
   */
  loadLayout(
    bands: RavenCompositeBand[],
    initialSources: RavenSource[],
    savedState: RavenState,
  ) {
    return [
      of(new sourceExplorerActions.UpdateSourceExplorer({
        ...fromSourceExplorer.initialState,
        fetchPending: true,
      })),
      of(new timelineActions.UpdateTimeline({
        ...fromTimeline.initialState,
        bands: bands.map(band => ({
          ...band,
          subBands: band.subBands.map((subBand: RavenSubBand) => ({
            ...subBand,
            sourceIds: [],
          })),
        })),
      })),
      of(new configActions.UpdateDefaultBandSettings({
        ...savedState.defaultBandSettings,
      })),
      ...this.load(bands, initialSources),
      ...savedState.pins.map(pin => of(new sourceExplorerActions.PinAdd(pin))), // TODO: Update layouts to apply pins correctly.
      of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
    ];
  }

  /**
   * Helper. Returns a stream of actions that need to occur when loading a state.
   */
  loadState(
    initialSources: RavenSource[],
    savedState: RavenState,
  ): Observable<Action>[] {
    return [
      of(new sourceExplorerActions.UpdateSourceExplorer({
        ...fromSourceExplorer.initialState,
        fetchPending: true,
      })),
      of(new timelineActions.UpdateTimeline({
        ...fromTimeline.initialState,
        bands: savedState.bands.map(band => ({
          ...band,
          subBands: band.subBands.map((subBand: RavenSubBand) => ({
            ...subBand,
            sourceIds: [],
          })),
        })),
        maxTimeRange: savedState.maxTimeRange,
        viewTimeRange: savedState.viewTimeRange,
      })),
      of(new configActions.UpdateDefaultBandSettings({
        ...savedState.defaultBandSettings,
      })),
      ...this.load(savedState.bands, initialSources),
      ...savedState.pins.map(pin => of(new sourceExplorerActions.PinAdd(pin))),
      of(new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false })),
    ];
  }

  /**
   * Helper. Returns a stream of actions that need to occur when loading a state or layout.
   * This first expands all parent source ids in the source explorer. Then it opens the actual leaf sources for data.
   */
  load(
    bands: RavenCompositeBand[],
    initialSources: RavenSource[],
  ): Observable<Action>[] {
    const { parentSourceIds, sourceIds } = getSourceIds(bands);
    return [
      of(new sourceExplorerActions.NewSources('/', initialSources)),
      ...parentSourceIds.map((sourceId: string) =>
        combineLatest(this.store$).pipe(
          take(1),
          map((state: AppState[]) => state[0].sourceExplorer.treeBySourceId[sourceId]),
          concatMap(source =>
            concat(
              ...this.expand(source),
              of(new sourceExplorerActions.UpdateTreeSource(source.id, { expanded: true })),
            ),
          ),
        ),
      ),
      combineLatest(this.store$).pipe(
        take(1),
        map((state: AppState[]) => state[0]),
        concatMap((state: AppState) =>
          // Restore filters after the explorer tree has been restored since we need to know the source type.
          concat(
            of(new sourceExplorerActions.UpdateSourceExplorer({
              customFiltersBySourceId: getCustomFiltersBySourceId(bands, state.sourceExplorer.treeBySourceId),
            })),
            this.restoreFilters(bands, state.sourceExplorer.treeBySourceId),
          ),
        )),
      ...sourceIds.map((sourceId: string) =>
        combineLatest(this.store$).pipe(
          take(1),
          map((state: AppState[]) => state[0]),
          concatMap((state: AppState) =>
            concat(
              ...this.openAllInstancesForSource(
                state.sourceExplorer.customFiltersBySourceId[sourceId],
                state.sourceExplorer.filtersByTarget,
                state.sourceExplorer.treeBySourceId,
                sourceId,
                bands,
                state.config.defaultBandSettings,
              ),
              of(new sourceExplorerActions.UpdateTreeSource(sourceId, { opened: true })),
            ),
          ),
        ),
      ),
      of(new layoutActions.Resize()),
    ];
  }

  /**
   * Helper. Returns a stream of actions that need to occur when opening a source explorer source.
   * The order of the cases in this function are very important. Do not change the order.
   */
  open(
    customFilter: RavenCustomFilter | null,
    filtersByTarget: StringTMap<StringTMap<string[]>>,
    treeBySourceId: StringTMap<RavenSource>,
    sourceId: string,
    currentBands: RavenCompositeBand[],
    bandId: string | null,
    subBandId: string | null,
    defaultBandSettings: RavenDefaultBandSettings,
  ) {
    return this.fetchSubBands(treeBySourceId, sourceId, defaultBandSettings, customFilter, filtersByTarget).pipe(
      concatMap((newSubBands: RavenSubBand[]) => {
        const actions: Action[] = [];

        if (treeBySourceId[sourceId].type === 'graphableFilter') {
          // Clear existing points regardless if fetch returns any data.
          actions.push(new timelineActions.RemoveAllPointsInSubBandWithParentSource(treeBySourceId[sourceId].parentId));
        }

        if (newSubBands.length > 0) {
          newSubBands.forEach((subBand: RavenSubBand) => {
            const activityBand = hasActivityBand(currentBands, subBand);
            const existingBand = treeBySourceId[sourceId].type === 'customGraphable' ? false : hasSourceId(currentBands, sourceId);

            if (activityBand) {
              actions.push(
                new sourceExplorerActions.SubBandIdAdd(sourceId, activityBand.subBandId),
                new timelineActions.AddPointsToSubBand(sourceId, activityBand.bandId, activityBand.subBandId, subBand.points),
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

            // Make sure that if we are dealing with a custom graphable source that we update the custom filters with the new sub-band id.
            if (treeBySourceId[sourceId].type === 'customGraphable') {
              actions.push(new sourceExplorerActions.SetCustomFilterSubBandId(sourceId, customFilter ? customFilter.label : '', subBand.id));
            }
          });

          // Resize bands when we `open` to make sure they are all resized properly.
          actions.push(new layoutActions.Resize());
        } else {
          // Notify user no bands will be drawn.
          actions.push(new dialogActions.OpenConfirmDialog('OK', 'Data set empty. Timeline will not de drawn.', '350px'));
        }

        return actions;
      }),
    );
  }

  /**
   * Helper. Graph one or more bands for a single source. A band for each filter if custom source.
   */
  openAllInstancesForSource(
    customFilters: RavenCustomFilter[] | null = null,
    filtersByTarget: StringTMap<StringTMap<string[]>>,
    treeBySourceId: StringTMap<RavenSource>,
    sourceId: string,
    currentBands: RavenCompositeBand[],
    defaultBandSettings: RavenDefaultBandSettings,
  ): Observable<Action>[] {
    if (customFilters) {
      return customFilters.map(customFilter =>
        this.open(
          customFilter,
          filtersByTarget,
          treeBySourceId,
          sourceId,
          currentBands,
          null,
          null,
          defaultBandSettings,
        ),
      );
    }

    if (treeBySourceId[sourceId].type === 'customFilter' || treeBySourceId[sourceId].type === 'filter') {
      // No drawing for customFilters or filters. TODO: Why?
      return [];
    }

    if (treeBySourceId[sourceId].type === 'graphableFilter') {
      return [of(new sourceExplorerActions.AddGraphableFilter(treeBySourceId[sourceId] as RavenGraphableFilterSource))];
    } else {
      return [
        this.open(
          null,
          filtersByTarget,
          treeBySourceId,
          sourceId,
          currentBands,
          null,
          null,
          defaultBandSettings,
        ),
      ];
    }
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
   * Fetch helper. Deletes a source from MPS Server.
   */
  removeSource(sourceUrl: string, sourceId: string) {
    const url = sourceUrl.replace(/list_(generic|.*custom.*)-mongodb/i, 'fs-mongodb');
    return this.http.delete(url, { responseType: 'text' }).pipe(
      map(() => new sourceExplorerActions.RemoveSource(sourceId)),
    );
  }

  /**
   * Helper. Returns a stream of Add/Set filter actions to restore filters and custom filters.
   */
  restoreFilters(bands: RavenCompositeBand[], treeBySourceId: StringTMap<RavenSource>) {
    const actions: Action[] = [];

    bands.forEach((band: RavenCompositeBand) => {
      band.subBands.forEach((subBand: RavenSubBand) => {
        subBand.sourceIds.forEach(sourceId => {
          if (treeBySourceId[sourceId] && treeBySourceId[sourceId].type === 'filter') {
            actions.push(new sourceExplorerActions.AddFilter(treeBySourceId[sourceId] as RavenFilterSource));
          } else {
            const hasQueryString = sourceId.match(new RegExp('(.*)\\?(.*)'));

            if (hasQueryString) {
              const [, id, args] = hasQueryString;
              const source = treeBySourceId[id] as RavenCustomFilterSource;

              if (source && source.type === 'customFilter') {
                const hasQueryStringArgs = args.match(new RegExp('(.*)=(.*)'));

                if (hasQueryStringArgs) {
                  actions.push(new sourceExplorerActions.SetCustomFilter(source, hasQueryStringArgs[2]));
                }
              }
            }
          }
        });
      });
    });

    return actions;
  }

  /**
   * Fetch helper. Fetches saved state from MPS Server.
   * Imports state after fetching.
   */
  fetchState(url: string) {
    return this.http.get(url).pipe(
      map(res => importState(res[0])),
    );
  }

  /**
   * Helper. Save state to an MPS Server source.
   * Exports state before saving.
   * Fetches new sources and updates the source state after the save.
   */
  saveState(sourceUrl: string, sourceId: string, name: string, state: AppState) {
    return this.http.put(`${sourceUrl}/${name}?timeline_type=state`, getState(name, state)).pipe(
      map(() => new sourceExplorerActions.FetchNewSources(sourceId, sourceUrl)),
    );
  }

  /**
   * Helper. Import mapping file into MPS Server for a given source URL.
   */
  importMappingFile(sourceUrl: string, name: string, mapping: string) {
    const url = sourceUrl.replace('fs-mongodb', 'metadata-mongodb');
    return this.http.post(`${url}/${name}`, mapping, { responseType: 'text' });
  }
}
