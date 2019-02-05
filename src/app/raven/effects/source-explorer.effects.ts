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
import { flatten, uniqueId } from 'lodash';
import { MpsServerService } from '../../shared/services/mps-server.service';
import { RavenAppState } from '../raven-store';

import { combineLatest, concat, forkJoin, Observable, of } from 'rxjs';

import {
  catchError,
  concatMap,
  map,
  mergeMap,
  switchMap,
  take,
  timeout,
  withLatestFrom,
} from 'rxjs/operators';

import {
  AddCustomGraph,
  AddGraphableFilter,
  ApplyCurrentState,
  ApplyLayout,
  ApplyLayoutWithPins,
  ApplyState,
  ApplyStateOrLayoutSuccess,
  CloseEvent,
  ExpandEvent,
  FetchInitialSources,
  FetchNewSources,
  FolderAdd,
  GraphAgainEvent,
  GraphCustomSource,
  ImportFile,
  LoadErrorsDisplay,
  OpenEvent,
  RemoveSourceEvent,
  SaveState,
  SourceExplorerActionTypes,
  UpdateCurrentState,
  UpdateGraphAfterFilterAdd,
  UpdateGraphAfterFilterRemove,
} from '../actions/source-explorer.actions';

import {
  activityBandsWithLegend,
  getActivityPointInBand,
  getBandsWithSourceId,
  getCustomFilterForLabel,
  getCustomFiltersBySourceId,
  getFormattedSourceUrl,
  getPinLabel,
  getRavenState,
  getSituationalAwarenessPageDuration,
  getSituationalAwarenessStartTime,
  getSourceIds,
  getSourceNameFromId,
  getState,
  getTargetFilters,
  hasActivityBandForFilterTarget,
  isAddTo,
  isOverlay,
  toCompositeBand,
  toRavenBandData,
  toRavenSources,
  updateSourceId,
  utc,
} from '../../shared/util';

import {
  MpsServerGraphData,
  MpsServerSource,
  RavenCompositeBand,
  RavenCustomFilter,
  RavenCustomFilterSource,
  RavenDefaultBandSettings,
  RavenFilterSource,
  RavenGraphableFilterSource,
  RavenPin,
  RavenSource,
  RavenState,
  RavenSubBand,
  StringTMap,
} from '../../shared/models';

import * as configActions from '../../shared/actions/config.actions';
import * as dialogActions from '../actions/dialog.actions';
import * as layoutActions from '../actions/layout.actions';
import * as sourceExplorerActions from '../actions/source-explorer.actions';
import * as timelineActions from '../actions/timeline.actions';
import * as toastActions from '../actions/toast.actions';

import * as fromSourceExplorer from '../reducers/source-explorer.reducer';
import * as fromTimeline from '../reducers/timeline.reducer';

@Injectable()
export class SourceExplorerEffects {
  constructor(
    private actions$: Actions,
    private http: HttpClient,
    private mpsServerService: MpsServerService,
    private store$: Store<RavenAppState>,
  ) {}

  /**
   * Effect for AddCustomGraph.
   */
  @Effect()
  addCustomGraph$: Observable<Action> = this.actions$.pipe(
    ofType<AddCustomGraph>(SourceExplorerActionTypes.AddCustomGraph),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(
      ({
        state: {
          config,
          raven: { sourceExplorer, timeline, situationalAwareness },
        },
        action: { label, sourceId },
      }) =>
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
            config.raven.defaultBandSettings,
            sourceExplorer.pins,
            situationalAwareness.situationalAware,
            getSituationalAwarenessStartTime(situationalAwareness),
            getSituationalAwarenessPageDuration(situationalAwareness),
            true,
          ),
          of(new sourceExplorerActions.LoadErrorsDisplay()),
          of(
            new sourceExplorerActions.UpdateSourceExplorer({
              fetchPending: false,
            }),
          ),
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
        of(
          new sourceExplorerActions.UpdateGraphAfterFilterAdd(action.source.id),
        ),
      ),
    ),
  );

  /**
   * Effect for ApplyState.
   */
  @Effect()
  applyCurrentState$: Observable<Action> = this.actions$.pipe(
    ofType<ApplyCurrentState>(SourceExplorerActionTypes.ApplyCurrentState),
    withLatestFrom(this.store$),
    map(([, state]) => ({ state })),
    concatMap(({ state }) =>
      forkJoin([
        of(state),
        this.mpsServerService.fetchNewSources(
          `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}`,
          '/',
          true,
        ),
      ]),
    ),
    map(([state, initialSources]) => ({
      initialSources,
      state,
    })),
    concatMap(({ state, initialSources }) =>
      concat(
        ...this.loadState(
          state,
          initialSources,
          state.raven.sourceExplorer.currentState,
          state.raven.sourceExplorer.currentStateId,
        ),
      ),
    ),
  );

  /**
   * Effect for ApplyLayout.
   * Note that right now we are only applying layouts to the `fs_file` type.
   */
  @Effect()
  applyLayout$: Observable<Action> = this.actions$.pipe(
    ofType<ApplyLayout>(SourceExplorerActionTypes.ApplyLayout),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      forkJoin([
        of(action),
        of(state),
        this.mpsServerService.fetchNewSources(
          `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}`,
          '/',
          true,
        ),
      ]),
    ),
    map(([action, state, initialSources]) => ({
      action,
      initialSources,
      state,
    })),
    concatMap(({ action, state, initialSources }) => {
      const savedState = state.raven.sourceExplorer.currentState as RavenState;

      return forkJoin([
        of(action),
        of(state),
        of(savedState),
        of(initialSources),
        this.fetchSourcesByType(
          state,
          getSourceIds(savedState.bands).parentSourceIds,
        ),
      ]);
    }),
    map(([action, state, savedState, initialSources, sourceTypes]) => ({
      action,
      initialSources,
      savedState,
      sourceTypes,
      state,
    })),
    concatMap(({ action, state, savedState, initialSources, sourceTypes }) => {
      const updatedBands: RavenCompositeBand[] = [];

      action.update.targetSourceIds.forEach((targetSourceId: string) => {
        const bands = savedState.bands.map((band: RavenCompositeBand) => {
          const parentId = uniqueId();
          const targetSource =
            state.raven.sourceExplorer.treeBySourceId[targetSourceId];

          return {
            ...band,
            id: parentId,
            subBands: band.subBands.map((subBand: RavenSubBand) => ({
              ...subBand,
              id: uniqueId(),
              parentUniqueId: parentId,
              sourceIds: subBand.sourceIds.map(sourceId =>
                updateSourceId(
                  sourceId,
                  targetSourceId,
                  sourceTypes,
                  targetSource.type,
                ),
              ),
            })),
          };
        });

        updatedBands.push(...bands);
      });

      return concat(
        ...this.loadLayout(
          state,
          updatedBands,
          initialSources,
          savedState,
          state.raven.sourceExplorer.currentStateId,
          Object.values(action.update.pins),
        ),
      );
    }),
  );

  /**
   * Effect for ApplyLayoutWithPins.
   */
  @Effect()
  applyLayoutWithPins$: Observable<Action> = this.actions$.pipe(
    ofType<ApplyLayoutWithPins>(SourceExplorerActionTypes.ApplyLayoutWithPins),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      forkJoin([
        of(action),
        of(state),
        this.mpsServerService.fetchNewSources(
          `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}`,
          '/',
          true,
        ),
      ]),
    ),
    map(([action, state, initialSources]) => ({
      action,
      initialSources,
      state,
    })),
    concatMap(({ action, state, initialSources }) => {
      const savedState = state.raven.sourceExplorer.currentState as RavenState;

      return forkJoin([
        of(action),
        of(state),
        of(savedState),
        of(initialSources),
        this.fetchSourcesByType(
          state,
          getSourceIds(savedState.bands).parentSourceIds,
        ),
      ]);
    }),
    map(([action, state, savedState, initialSources, sourceTypes]) => ({
      action,
      initialSources,
      savedState,
      sourceTypes,
      state,
    })),
    concatMap(({ action, state, savedState, initialSources, sourceTypes }) => {
      const bands = savedState.bands.map((band: RavenCompositeBand) => {
        const parentId = uniqueId();

        return {
          ...band,
          id: parentId,
          subBands: band.subBands.map((subBand: RavenSubBand) => {
            const labelPin = subBand.labelPin;
            const pin = action.update.pins[labelPin];
            const targetSource =
              state.raven.sourceExplorer.treeBySourceId[pin.sourceId];

            return {
              ...subBand,
              id: uniqueId(),
              parentUniqueId: parentId,
              sourceIds: subBand.sourceIds.map(sourceId =>
                updateSourceId(
                  sourceId,
                  targetSource.id,
                  sourceTypes,
                  targetSource.type,
                ),
              ),
            };
          }),
        };
      });

      return concat(
        ...this.loadLayout(
          state,
          bands,
          initialSources,
          savedState,
          state.raven.sourceExplorer.currentStateId,
          Object.values(action.update.pins),
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
        of(action),
        of(state),
        this.mpsServerService.fetchState(action.sourceUrl),
        this.mpsServerService.fetchNewSources(
          `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}`,
          '/',
          true,
        ),
      ]),
    ),
    map(([action, state, savedState, initialSources]) => ({
      action,
      initialSources,
      savedState,
      state,
    })),
    concatMap(({ action, initialSources, savedState, state }) =>
      concat(
        ...this.loadState(state, initialSources, savedState, action.sourceId),
      ),
    ),
  );

  /**
   * Effect for ApplyStateOrLayoutSuccess.
   */
  @Effect()
  applyStateOrLayoutSuccess$: Observable<Action> = this.actions$.pipe(
    ofType<ApplyStateOrLayoutSuccess>(
      SourceExplorerActionTypes.ApplyStateOrLayoutSuccess,
    ),
    withLatestFrom(this.store$),
    map(([, state]) => state),
    concatMap(state =>
      concat(
        this.restoreExpansion(
          state.raven.timeline.bands,
          state.raven.timeline.expansionByActivityId,
        ),
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
      new sourceExplorerActions.UpdateTreeSource(action.sourceId, {
        opened: false,
      }),
      new layoutActions.Resize(), // Resize bands when we `close` to make sure they are all resized properly.
    ]),
  );

  @Effect()
  createFolder$: Observable<Action> = this.actions$.pipe(
    ofType<FolderAdd>(SourceExplorerActionTypes.FolderAdd),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      concat(
        this.withLoadingBar(this.createFolder(action, state)),
        of(new sourceExplorerActions.ExpandEvent(action.folder.url)),
      ),
    ),
  );

  /**
   * Effect for ExpandEvent.
   */
  @Effect()
  expandEvent$: Observable<Action> = this.actions$.pipe(
    ofType<ExpandEvent>(SourceExplorerActionTypes.ExpandEvent),
    withLatestFrom(this.store$),
    map(
      ([action, state]) =>
        state.raven.sourceExplorer.treeBySourceId[action.sourceId],
    ),
    switchMap(source =>
      concat(
        ...this.expand(source),
        of(
          new sourceExplorerActions.UpdateSourceExplorer({
            fetchPending: false,
          }),
        ),
        of(
          new sourceExplorerActions.UpdateTreeSource(source.id, {
            expanded: true,
          }),
        ),
      ).pipe(
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - expandEvent$: ', e);
          return [
            new toastActions.ShowToast(
              'warning',
              `Failed To Expand Source "${source.name}"`,
              '',
            ),
            new sourceExplorerActions.UpdateSourceExplorer({
              fetchPending: false,
            }),
            new sourceExplorerActions.UpdateTreeSource(source.id, {
              expanded: false,
            }),
          ];
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
    concatMap((state: RavenAppState) =>
      concat(
        this.mpsServerService
          .fetchNewSources(
            `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}`,
            '/',
            true,
          )
          .pipe(
            map(
              (sources: RavenSource[]) =>
                new sourceExplorerActions.NewSources('/', sources) as Action,
            ),
          ),
        of(
          new sourceExplorerActions.UpdateSourceExplorer({
            fetchPending: false,
            initialSourcesLoaded: true,
          }),
        ),
      ).pipe(
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - fetchInitialSources$: ', e);
          return [
            new toastActions.ShowToast(
              'warning',
              'Failed To Fetch Initial Sources',
              '',
            ),
            new sourceExplorerActions.UpdateSourceExplorer({
              fetchPending: false,
              initialSourcesLoaded: false,
            }),
          ];
        }),
      ),
    ),
  );

  /**
   * Effect for FetchNewSources.
   */
  @Effect()
  fetchNewSources$: Observable<Action> = this.actions$.pipe(
    ofType<FetchNewSources>(SourceExplorerActionTypes.FetchNewSources),
    concatMap(action =>
      concat(
        this.mpsServerService
          .fetchNewSources(action.sourceUrl, action.sourceId, false)
          .pipe(
            concatMap((sources: RavenSource[]) => [
              new sourceExplorerActions.NewSources(action.sourceId, sources),
            ]),
          ),
        of(
          new sourceExplorerActions.UpdateSourceExplorer({
            fetchPending: false,
          }),
        ),
      ).pipe(
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - fetchNewSources$: ', e);
          return [
            new toastActions.ShowToast(
              'warning',
              'Failed To Fetch Sources',
              '',
            ),
            new sourceExplorerActions.UpdateSourceExplorer({
              fetchPending: false,
            }),
          ];
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
        of(
          new sourceExplorerActions.AddCustomFilter(
            action.sourceId,
            action.label,
            action.filter,
          ),
        ),
        of(
          new sourceExplorerActions.AddCustomGraph(
            action.sourceId,
            action.label,
            action.filter,
          ),
        ),
      ),
    ),
  );

  @Effect()
  graphAgainEvent$: Observable<Action> = this.actions$.pipe(
    ofType<GraphAgainEvent>(SourceExplorerActionTypes.GraphAgainEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    mergeMap(({ state: { config, raven }, action }) =>
      this.withLoadingBar(
        this.open(
          null,
          raven.sourceExplorer.filtersByTarget,
          raven.sourceExplorer.treeBySourceId,
          action.sourceId,
          raven.timeline.bands,
          raven.timeline.selectedBandId,
          raven.timeline.selectedSubBandId,
          config.raven.defaultBandSettings,
          raven.sourceExplorer.pins,
          raven.situationalAwareness.situationalAware,
          getSituationalAwarenessStartTime(raven.situationalAwareness),
          getSituationalAwarenessPageDuration(raven.situationalAwareness),
          true,
        ),
      ).pipe(
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - graphAgainEvent$: ', e);
          return [
            new toastActions.ShowToast(
              'warning',
              'Failed To Graph Again Source',
              '',
            ),
            new sourceExplorerActions.UpdateSourceExplorer({
              fetchPending: false,
            }),
          ];
        }),
      ),
    ),
  );

  /**
   * Effect for ImportFile.
   */
  @Effect()
  importFile$: Observable<Action> = this.actions$.pipe(
    ofType<ImportFile>(SourceExplorerActionTypes.ImportFile),
    concatMap(action => this.withLoadingBar(this.importFile(action))),
  );

  /**
   * Effect for LoadErrorsDisplay.
   */
  @Effect()
  loadErrorsDisplay$: Observable<Action> = this.actions$.pipe(
    ofType<LoadErrorsDisplay>(SourceExplorerActionTypes.LoadErrorsDisplay),
    withLatestFrom(this.store$),
    map(([, state]) => state.raven.sourceExplorer.loadErrors),
    switchMap(loadErrors => {
      if (loadErrors.length) {
        const errors = loadErrors.map(error => `${error}\n\n`);

        return [
          new dialogActions.OpenConfirmDialog(
            'OK',
            `Data sets empty or do not exist.\nTimeline will not be drawn for:\n\n ${errors}`,
            '350px',
          ),
          new sourceExplorerActions.UpdateSourceExplorer({ loadErrors: [] }),
        ];
      }
      return [];
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
    mergeMap(({ state: { config, raven }, action }) =>
      concat(
        this.open(
          null,
          raven.sourceExplorer.filtersByTarget,
          raven.sourceExplorer.treeBySourceId,
          action.sourceId,
          raven.timeline.bands,
          raven.timeline.selectedBandId,
          raven.timeline.selectedSubBandId,
          config.raven.defaultBandSettings,
          raven.sourceExplorer.pins,
          raven.situationalAwareness.situationalAware,
          getSituationalAwarenessStartTime(raven.situationalAwareness),
          getSituationalAwarenessPageDuration(raven.situationalAwareness),
          false,
        ),
        of(
          new sourceExplorerActions.UpdateSourceExplorer({
            fetchPending: false,
          }),
        ),
        of(
          new sourceExplorerActions.UpdateTreeSource(action.sourceId, {
            opened: true,
          }),
        ),
      ).pipe(
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - openEvent$: ', e);
          return [
            new toastActions.ShowToast('warning', 'Failed To Open Source', ''),
            new sourceExplorerActions.UpdateSourceExplorer({
              fetchPending: false,
            }),
            new sourceExplorerActions.UpdateTreeSource(action.sourceId, {
              opened: false,
            }),
          ];
        }),
      ),
    ),
  );

  /**
   * Effect for RemoveGraphableFilter.
   */
  @Effect()
  removeGraphableFilter$: Observable<Action> = this.actions$.pipe(
    ofType<sourceExplorerActions.RemoveGraphableFilter>(
      SourceExplorerActionTypes.RemoveGraphableFilter,
    ),
    concatMap(action =>
      concat(
        of(
          new sourceExplorerActions.RemoveFilter(
            action.source as RavenFilterSource,
          ),
        ),
        of(
          new sourceExplorerActions.SubBandIdRemove(
            [action.source.id],
            action.source.subBandIds[0],
          ),
        ),
        of(
          new sourceExplorerActions.UpdateGraphAfterFilterRemove(
            action.source.id,
          ),
        ),
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
        this.mpsServerService
          .removeSource(action.source.url, action.source.id)
          .pipe(
            map(() => new sourceExplorerActions.RemoveSource(action.source.id)),
          ),
        of(
          new sourceExplorerActions.UpdateSourceExplorer({
            fetchPending: false,
          }),
        ),
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
      this.mpsServerService
        .saveState(action.source.url, action.name, getState(action.name, state))
        .pipe(
          map(
            () =>
              new sourceExplorerActions.UpdateSourceExplorer({
                currentState: getRavenState(action.name, state),
                currentStateId: `${action.source.id}/${action.name}`,
              }),
            new sourceExplorerActions.UpdateSourceExplorer({
              fetchPending: false,
            }),
          ),
        ),
    ),
  );

  /**
   * Effect for SaveState.
   */
  @Effect()
  updateGraphAfterFilterAdd$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateGraphAfterFilterAdd>(
      SourceExplorerActionTypes.UpdateGraphAfterFilterAdd,
    ),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(
      ({
        state: {
          config,
          raven: { sourceExplorer, situationalAwareness },
        },
        action,
      }) =>
        concat(
          this.fetchGraphableFilters(
            sourceExplorer.treeBySourceId,
            action.sourceId,
            config.raven.defaultBandSettings,
            null,
            sourceExplorer.filtersByTarget,
            situationalAwareness.situationalAware,
            getSituationalAwarenessStartTime(situationalAwareness),
            getSituationalAwarenessPageDuration(situationalAwareness),
          ).pipe(
            withLatestFrom(this.store$),
            map(([newSubBands, state]) => ({ newSubBands, state })),
            concatMap(({ newSubBands, state: { raven: { timeline } } }) => {
              const actions: Action[] = [];
              const filterTarget = (sourceExplorer.treeBySourceId[
                action.sourceId
              ] as RavenFilterSource).filterTarget;

              newSubBands.forEach((subBand: RavenSubBand) => {
                const activityBand = hasActivityBandForFilterTarget(
                  timeline.bands,
                  filterTarget,
                );
                if (activityBand) {
                  actions.push(
                    // Add filterSource id to existing band.
                    new timelineActions.SourceIdAdd(
                      action.sourceId,
                      activityBand.subBandId,
                    ),
                    // Replace points in band.
                    new timelineActions.SetPointsForSubBand(
                      activityBand.bandId,
                      activityBand.subBandId,
                      subBand.points,
                    ),
                  );
                } else {
                  actions.push(
                    new sourceExplorerActions.SubBandIdAdd(
                      action.sourceId,
                      subBand.id,
                    ),
                    new timelineActions.AddBand(
                      action.sourceId,
                      toCompositeBand(subBand),
                      {
                        additionalSubBandProps: {
                          filterTarget: (sourceExplorer.treeBySourceId[
                            action.sourceId
                          ] as RavenFilterSource).filterTarget,
                        },
                      },
                    ),
                  );
                }
              });

              return actions;
            }),
          ),
          of(
            new sourceExplorerActions.UpdateSourceExplorer({
              fetchPending: false,
            }),
          ),
        ),
    ),
  );

  /**
   * Effect for UpdateGraphAfterFilterRemove.
   */
  @Effect()
  updateGraphAfterFilterRemove$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateGraphAfterFilterRemove>(
      SourceExplorerActionTypes.UpdateGraphAfterFilterRemove,
    ),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(
      ({
        state: {
          config,
          raven: {
            sourceExplorer,
            timeline: { bands },
            situationalAwareness,
          },
        },
        action,
      }) =>
        concat(
          this.fetchGraphableFilters(
            sourceExplorer.treeBySourceId,
            action.sourceId,
            config.raven.defaultBandSettings,
            null,
            sourceExplorer.filtersByTarget,
            situationalAwareness.situationalAware,
            getSituationalAwarenessStartTime(situationalAwareness),
            getSituationalAwarenessPageDuration(situationalAwareness),
          ).pipe(
            concatMap((newSubBands: RavenSubBand[]) => {
              const actions: Action[] = [];
              const target = (sourceExplorer.treeBySourceId[
                action.sourceId
              ] as RavenFilterSource).filterTarget;

              if (newSubBands.length) {
                newSubBands.forEach((subBand: RavenSubBand) => {
                  const activityBand = hasActivityBandForFilterTarget(
                    bands,
                    target,
                  );

                  if (activityBand) {
                    actions.push(
                      // Remove sourceId from band.
                      new timelineActions.RemoveSourceIdFromSubBands(
                        action.sourceId,
                      ),
                      // Replace band points.
                      new timelineActions.SetPointsForSubBand(
                        activityBand.bandId,
                        activityBand.subBandId,
                        subBand.points,
                      ),
                    );
                  }
                });
              } else {
                // When the last graphable filter source is unselected.
                const activityBand = hasActivityBandForFilterTarget(
                  bands,
                  target,
                );

                if (activityBand) {
                  actions.push(
                    // Remove sourceId from band.
                    new timelineActions.RemoveSourceIdFromSubBands(
                      action.sourceId,
                    ),
                    // Set empty data points.
                    new timelineActions.SetPointsForSubBand(
                      activityBand.bandId,
                      activityBand.subBandId,
                      [],
                    ),
                  );
                }
              }

              return actions;
            }),
          ),
          of(
            new sourceExplorerActions.UpdateSourceExplorer({
              fetchPending: false,
            }),
          ),
        ),
    ),
  );

  @Effect()
  updateCurrentState$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateCurrentState>(SourceExplorerActionTypes.UpdateCurrentState),
    withLatestFrom(this.store$),
    map(([, state]) => ({ state })),
    concatMap(({ state }) =>
      this.mpsServerService
        .updateState(
          `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}${
            state.raven.sourceExplorer.currentStateId
          }`,
          getState(
            getSourceNameFromId(state.raven.sourceExplorer.currentStateId),
            state,
          ),
        )
        .pipe(
          map(
            () =>
              new sourceExplorerActions.UpdateSourceExplorer({
                currentState: getRavenState(
                  getSourceNameFromId(
                    state.raven.sourceExplorer.currentStateId,
                  ),
                  state,
                ),
              }),
            new sourceExplorerActions.UpdateSourceExplorer({
              fetchPending: false,
            }),
          ),
        ),
    ),
  );

  /**
   * Helper. api call to mps server to create the folder
   */
  createFolder(action: sourceExplorerActions.FolderAdd, state: RavenAppState) {
    const headers = new HttpHeaders().set('Content-Type', `application/json`);
    const responseType = 'text';
    const url = `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}${
      action.folder.url
    }/${action.folder.name}`;

    return this.http.put(url, '', { headers, responseType }).pipe(
      concatMap(() => {
        return of(new sourceExplorerActions.FolderAddSuccess());
      }),
      catchError((e: Error) => {
        console.error('SourceExplorerEffects - createFolder$: ', e);
        return [
          new toastActions.ShowToast('warning', 'Failed To Create Folder', ''),
          new sourceExplorerActions.FolderAddFailure(),
        ];
      }),
    );
  }

  /**
   * Helper. Returns a stream of actions that need to occur when expanding a source explorer source.
   */
  expand(source: RavenSource): Observable<Action>[] {
    const actions: Observable<Action>[] = [];

    if (source) {
      if (!source.childIds.length) {
        if (source.content) {
          actions.push(
            of(
              new sourceExplorerActions.NewSources(
                source.id,
                toRavenSources(source.id, false, source.content),
              ),
            ),
          );
        } else {
          actions.push(
            this.mpsServerService
              .fetchNewSources(source.url, source.id, false)
              .pipe(
                concatMap((sources: RavenSource[]) => [
                  new sourceExplorerActions.NewSources(source.id, sources), // Add new sources to the source-explorer.
                ]),
              ),
          );
        }
      }
    } else {
      console.warn(
        'source-explorer.effect: expand: source is not defined: ',
        source,
      );
    }

    return actions;
  }

  /**
   * Fetch helper. Fetches graphable filter data from MPS Server and maps it to Raven sub-band data.
   */
  fetchGraphableFilters(
    treeBySourceId: StringTMap<RavenSource>,
    sourceId: string,
    defaultBandSettings: RavenDefaultBandSettings,
    customFilter: RavenCustomFilter | null,
    filtersByTarget: StringTMap<StringTMap<string[]>>,
    situAware: boolean,
    startTime: string,
    pageDuration: string,
  ) {
    const source = treeBySourceId[sourceId];
    return this.http
      .post<MpsServerGraphData>(
        getFormattedSourceUrl(
          treeBySourceId,
          source,
          customFilter,
          situAware,
          startTime,
          pageDuration,
        ),
        getTargetFilters(
          treeBySourceId,
          filtersByTarget,
          (source as RavenGraphableFilterSource).filterTarget,
        ),
        { responseType: 'json' },
      )
      .pipe(
        map((graphData: MpsServerGraphData) =>
          toRavenBandData(
            sourceId,
            source.name,
            graphData,
            defaultBandSettings,
            customFilter,
            treeBySourceId,
          ),
        ),
      );
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
    situAware: boolean,
    startTime: string,
    pageDuration: string,
  ) {
    const source = treeBySourceId[sourceId];
    return this.http
      .get<MpsServerGraphData>(
        getFormattedSourceUrl(
          treeBySourceId,
          source,
          customFilter,
          situAware,
          startTime,
          pageDuration,
        ),
      )
      .pipe(
        map((graphData: MpsServerGraphData) =>
          toRavenBandData(
            sourceId,
            source.name,
            graphData,
            defaultBandSettings,
            customFilter,
            treeBySourceId,
          ),
        ),
      );
  }

  /**
   * Helper. Returns a stream of actions that need to occur when importing a file.
   */
  importFile(action: sourceExplorerActions.ImportFile) {
    const headers = new HttpHeaders().set(
      'Content-Type',
      `${action.file.type === 'pef' ? 'application/json' : 'text/csv'}`,
    );
    const url = `${action.source.url}/${action.file.name}?timeline_type=${
      action.file.type
    }&time_format=${action.file.timeFormat}`;

    return this.http
      .put(url, action.file.data, { headers: headers, responseType: 'text' })
      .pipe(
        concatMap(() => {
          if (action.file.mapping) {
            return this.mpsServerService
              .importMappingFile(
                action.source.url,
                action.file.name,
                action.file.mapping,
              )
              .pipe(map(() => new sourceExplorerActions.ImportFileSuccess()));
          } else {
            return of(new sourceExplorerActions.ImportFileSuccess());
          }
        }),
        catchError((e: Error) => {
          console.error('SourceExplorerEffects - importFile$: ', e);
          return [
            new toastActions.ShowToast('warning', 'Failed To Import File', ''),
            new sourceExplorerActions.ImportFileFailure(),
          ];
        }),
      );
  }

  /**
   * Helper. Returns a stream of actions that need to occur when loading a layout.
   */
  loadLayout(
    state: RavenAppState,
    bands: RavenCompositeBand[],
    initialSources: RavenSource[],
    savedState: RavenState,
    sourceId: string,
    pins: RavenPin[],
  ) {
    return [
      of(
        new sourceExplorerActions.UpdateSourceExplorer({
          ...fromSourceExplorer.initialState,
          currentState: savedState,
          currentStateId: sourceId,
          fetchPending: true,
        }),
      ),
      of(
        new timelineActions.UpdateTimeline({
          ...fromTimeline.initialState,
          bands: bands.map(band => ({
            ...band,
            subBands: band.subBands.map((subBand: RavenSubBand) => ({
              ...subBand,
              sourceIds: [],
            })),
          })),
          expansionByActivityId: savedState.expansionByActivityId,
        }),
      ),
      of(
        new configActions.UpdateDefaultBandSettings({
          ...savedState.defaultBandSettings,
        }),
      ),
      ...this.load(bands, initialSources, pins),
      of(new timelineActions.RemoveBandsWithNoPoints()),
      ...pins.map(pin => of(new sourceExplorerActions.PinAdd(pin))),
      ...pins.map(pin => of(new timelineActions.PinAdd(pin))),
      of(new timelineActions.ResetViewTimeRange()),
      of(new sourceExplorerActions.ApplyStateOrLayoutSuccess()),
      of(
        new sourceExplorerActions.UpdateSourceExplorer({ fetchPending: false }),
      ),
    ];
  }

  /**
   * Helper. Returns a stream of actions that need to occur when loading a state.
   */
  loadState(
    state: RavenAppState,
    initialSources: RavenSource[],
    savedState: RavenState | null,
    sourceId: string,
  ): Observable<Action>[] {
    if (savedState) {
      return [
        of(
          new sourceExplorerActions.UpdateSourceExplorer({
            ...fromSourceExplorer.initialState,
            currentState: savedState,
            currentStateId: sourceId,
            fetchPending: true,
          }),
        ),
        of(
          new timelineActions.UpdateTimeline({
            ...fromTimeline.initialState,
            bands: savedState.bands.map(band => ({
              ...band,
              subBands: band.subBands.map((subBand: RavenSubBand) => ({
                ...subBand,
                sourceIds: [],
              })),
            })),
            expansionByActivityId: savedState.expansionByActivityId,
            guides: savedState.guides ? savedState.guides : [],
            maxTimeRange: savedState.maxTimeRange,
            viewTimeRange: savedState.ignoreShareableLinkTimes
              ? { end: 0, start: 0 }
              : {
                  end: utc(savedState.viewTimeRange.end),
                  start: utc(savedState.viewTimeRange.start),
                },
          }),
        ),
        of(
          new configActions.UpdateDefaultBandSettings({
            ...savedState.defaultBandSettings,
          }),
        ),
        ...this.load(savedState.bands, initialSources, savedState.pins),
        ...savedState.pins.map(pin =>
          of(new sourceExplorerActions.PinAdd(pin)),
        ),
        ...savedState.pins.map(pin => of(new timelineActions.PinAdd(pin))),
        of(new sourceExplorerActions.ApplyStateOrLayoutSuccess()),
        of(
          new sourceExplorerActions.UpdateSourceExplorer({
            fetchPending: false,
          }),
        ),
      ];
    } else {
      return [];
    }
  }

  /**
   * Helper. Returns a stream of actions that need to occur when loading a state or layout.
   * This first expands all parent source ids in the source explorer. Then it opens the actual leaf sources for data.
   */
  load(
    bands: RavenCompositeBand[],
    initialSources: RavenSource[],
    pins: RavenPin[],
  ): Observable<Action>[] {
    const { parentSourceIds, sourceIds } = getSourceIds(bands);

    return [
      of(new sourceExplorerActions.NewSources('/', initialSources)),
      ...parentSourceIds.map((sourceId: string) =>
        combineLatest(this.store$).pipe(
          take(1),
          map(
            (state: RavenAppState[]) =>
              state[0].raven.sourceExplorer.treeBySourceId[sourceId],
          ),
          concatMap(source => {
            if (source) {
              return concat(
                ...this.expand(source),
                of(
                  new sourceExplorerActions.UpdateTreeSource(source.id, {
                    expanded: true,
                  }),
                ),
              );
            } else {
              console.log(
                'source-explorer.effect: load: source does not exist: ',
                sourceId,
              );
              return of(new sourceExplorerActions.LoadErrorsAdd([sourceId]));
            }
          }),
        ),
      ),
      combineLatest(this.store$).pipe(
        take(1),
        map((state: RavenAppState[]) => state[0]),
        concatMap((state: RavenAppState) =>
          // Restore filters after the explorer tree has been restored since we need to know the source type.
          concat(
            of(
              new sourceExplorerActions.UpdateSourceExplorer({
                customFiltersBySourceId: getCustomFiltersBySourceId(
                  bands,
                  state.raven.sourceExplorer.treeBySourceId,
                ),
              }),
            ),
            this.restoreFilters(
              bands,
              state.raven.sourceExplorer.treeBySourceId,
            ),
          ),
        ),
      ),
      ...sourceIds.map((sourceId: string) =>
        combineLatest(this.store$).pipe(
          take(1),
          map((state: RavenAppState[]) => state[0]),
          concatMap((state: RavenAppState) =>
            concat(
              ...this.openAllInstancesForSource(
                state.raven.sourceExplorer.customFiltersBySourceId[sourceId],
                state.raven.sourceExplorer.filtersByTarget,
                state.raven.sourceExplorer.treeBySourceId,
                sourceId,
                bands.map(band => ({
                  ...band,
                  subBands: band.subBands.map((subBand: RavenSubBand) => ({
                    ...subBand,
                    // cleanup source id by removing args
                    sourceIds: subBand.sourceIds.map(
                      srcId =>
                        srcId.indexOf('?') > -1
                          ? srcId.substring(0, srcId.indexOf('?'))
                          : srcId,
                    ),
                  })),
                })),
                state.config.raven.defaultBandSettings,
                pins,
                state.raven.situationalAwareness.situationalAware,
                getSituationalAwarenessStartTime(
                  state.raven.situationalAwareness,
                ),
                getSituationalAwarenessPageDuration(
                  state.raven.situationalAwareness,
                ),
              ),
              of(
                new sourceExplorerActions.UpdateTreeSource(sourceId, {
                  opened: true,
                }),
              ),
            ),
          ),
        ),
      ),
      of(new layoutActions.Resize()),
      of(new sourceExplorerActions.LoadErrorsDisplay()),
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
    pins: RavenPin[],
    situAware: boolean,
    startTime: string,
    pageDuration: string,
    graphAgain: boolean,
  ) {
    return this.fetchSubBands(
      treeBySourceId,
      sourceId,
      defaultBandSettings,
      customFilter,
      filtersByTarget,
      situAware,
      startTime,
      pageDuration,
    ).pipe(
      concatMap((newSubBands: RavenSubBand[]) => {
        const actions: Action[] = [];

        if (treeBySourceId[sourceId].type === 'graphableFilter') {
          // Clear existing points regardless if fetch returns any data.
          actions.push(
            new timelineActions.RemoveAllPointsInSubBandWithParentSource(
              treeBySourceId[sourceId].parentId,
            ),
          );
        }

        if (newSubBands.length > 0) {
          newSubBands.forEach((subBand: RavenSubBand) => {
            const activityBands =
              treeBySourceId[sourceId].type === 'customGraphable'
                ? []
                : activityBandsWithLegend(
                    currentBands,
                    subBand,
                    getPinLabel(treeBySourceId[sourceId].id, pins),
                  );
            const existingBands = graphAgain
              ? []
              : getBandsWithSourceId(currentBands, sourceId);
            if (!graphAgain && activityBands.length > 0) {
              activityBands.forEach(activityBand =>
                actions.push(
                  new sourceExplorerActions.SubBandIdAdd(
                    sourceId,
                    activityBand.subBandId,
                  ),
                  new timelineActions.AddPointsToSubBand(
                    sourceId,
                    activityBand.bandId,
                    activityBand.subBandId,
                    subBand.points,
                  ),
                ),
              );
            } else if (existingBands.length > 0) {
              existingBands.forEach(existingBand => {
                if (subBand.type === 'state') {
                  // Use the newly create state band with possibly updated possibleStates.
                  actions.push(
                    new sourceExplorerActions.SubBandIdAdd(
                      sourceId,
                      subBand.id,
                    ),
                    new timelineActions.AddSubBand(
                      sourceId,
                      existingBand.bandId,
                      subBand,
                    ),
                    // Romove the old state band.
                    new timelineActions.RemoveSubBand(existingBand.subBandId),
                  );
                } else {
                  actions.push(
                    new sourceExplorerActions.SubBandIdAdd(
                      sourceId,
                      existingBand.subBandId,
                    ),
                    new timelineActions.AddPointsToSubBand(
                      sourceId,
                      existingBand.bandId,
                      existingBand.subBandId,
                      subBand.points,
                    ),
                  );
                }
              });
            } else if (
              bandId &&
              subBandId &&
              isAddTo(currentBands, bandId, subBandId, subBand.type)
            ) {
              actions.push(
                new sourceExplorerActions.SubBandIdAdd(sourceId, subBandId),
                new timelineActions.AddPointsToSubBand(
                  sourceId,
                  bandId,
                  subBandId,
                  subBand.points,
                ),
              );
            } else if (bandId && isOverlay(currentBands, bandId)) {
              actions.push(
                new sourceExplorerActions.SubBandIdAdd(sourceId, subBand.id),
                new timelineActions.AddSubBand(sourceId, bandId, subBand),
                new timelineActions.SetCompositeYLabelDefault(bandId),
              );
            } else {
              actions.push(
                new sourceExplorerActions.SubBandIdAdd(sourceId, subBand.id),
                new timelineActions.AddBand(sourceId, toCompositeBand(subBand)),
              );
            }

            // Make sure that if we are dealing with a custom graphable source that we update the custom filters with the new sub-band id.
            if (treeBySourceId[sourceId].type === 'customGraphable') {
              actions.push(
                new sourceExplorerActions.SetCustomFilterSubBandId(
                  sourceId,
                  customFilter ? customFilter.label : '',
                  subBand.id,
                ),
              );
            }
          });

          // Resize bands when we `open` to make sure they are all resized properly.
          actions.push(new layoutActions.Resize());
        } else {
          // Notify user no bands will be drawn.
          actions.push(new sourceExplorerActions.LoadErrorsAdd([sourceId]));

          // Remove custom filter if custom source
          if (
            treeBySourceId[sourceId].type === 'customGraphable' &&
            customFilter
          ) {
            actions.push(
              new sourceExplorerActions.RemoveCustomFilter(
                sourceId,
                customFilter.label,
              ),
            );
          }
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
    pins: RavenPin[],
    situAware: boolean,
    startTime: string,
    pageDuration: string,
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
          pins,
          situAware,
          startTime,
          pageDuration,
          false,
        ),
      );
    }

    if (treeBySourceId[sourceId]) {
      if (
        treeBySourceId[sourceId].type === 'customFilter' ||
        treeBySourceId[sourceId].type === 'filter'
      ) {
        // No drawing for customFilters or filters. TODO: Why?
        return [];
      }

      if (treeBySourceId[sourceId].type === 'graphableFilter') {
        return [
          of(
            new sourceExplorerActions.AddGraphableFilter(treeBySourceId[
              sourceId
            ] as RavenGraphableFilterSource),
          ),
        ];
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
            pins,
            situAware,
            startTime,
            pageDuration,
            false,
          ),
        ];
      }
    } else {
      // Error case. No sources to open.
      // TODO: Catch and handle errors here.
      return [];
    }
  }

  /**
   * Helper. Makes requests for a list of source ids and returns the type of each source id.
   * This is used when we need to know the source type when we apply a layout.
   */
  fetchSourcesByType(state: RavenAppState, parentSourceIds: string[]) {
    // Collapse parentSourceIds into a map of sourceIds so we only fetch sources once.
    const sourceIds = parentSourceIds.reduce((ids, id) => {
      ids[id] = true;
      return ids;
    }, {});

    return forkJoin(
      Object.keys(sourceIds).map(sourceId =>
        this.http
          .get(
            `${state.config.app.baseUrl}/${
              state.config.mpsServer.apiUrl
            }${sourceId}`,
          )
          .pipe(
            timeout(3000), // Timeout long requests since MPS Server returns type information quickly, and long requests probably are not what we are looking for.
            map((mpsServerSources: MpsServerSource[]) =>
              toRavenSources('', false, mpsServerSources),
            ),
            map((sources: RavenSource[]) =>
              sources.map(source => ({ name: source.name, type: source.type })),
            ),
            catchError(() => of([])),
          ),
      ),
    ).pipe(
      map((res: any[][]) => flatten(res)), // forkJoin returns an array with each response in a sub-array. So flatten here to get a single array of sources.
      map(sources =>
        // Build and return a map of source names to their type.
        sources.reduce((sourceTypes, source) => {
          sourceTypes[source.name] = source.type;
          return sourceTypes;
        }, {}),
      ),
    );
  }

  /**
   * Helper. Returns a stream of fetch children or descendants actions to restore expansion.
   */
  restoreExpansion(
    bands: RavenCompositeBand[],
    expansionByActivityId: StringTMap<string>,
  ) {
    const actions: Action[] = [];
    Object.keys(expansionByActivityId).forEach(activityId => {
      const activityPointInBand = getActivityPointInBand(bands, activityId);
      if (activityPointInBand) {
        actions.push(
          new timelineActions.FetchChildrenOrDescendants(
            activityPointInBand.bandId,
            activityPointInBand.subBandId,
            activityPointInBand.activityPoint,
            expansionByActivityId[activityId],
          ),
        );
      }
    });
    return actions;
  }

  /**
   * Helper. Returns a stream of Add/Set filter actions to restore filters and custom filters.
   */
  restoreFilters(
    bands: RavenCompositeBand[],
    treeBySourceId: StringTMap<RavenSource>,
  ) {
    const actions: Action[] = [];

    bands.forEach((band: RavenCompositeBand) => {
      band.subBands.forEach((subBand: RavenSubBand) => {
        subBand.sourceIds.forEach(sourceId => {
          if (
            treeBySourceId[sourceId] &&
            treeBySourceId[sourceId].type === 'filter'
          ) {
            actions.push(
              new sourceExplorerActions.AddFilter(treeBySourceId[
                sourceId
              ] as RavenFilterSource),
            );
          } else {
            const hasQueryString = sourceId.match(new RegExp('(.*)\\?(.*)'));

            if (hasQueryString) {
              const [, id, args] = hasQueryString;
              const source = treeBySourceId[id] as RavenCustomFilterSource;

              if (source && source.type === 'customFilter') {
                const hasQueryStringArgs = args.match(new RegExp('(.*)=(.*)'));

                if (hasQueryStringArgs) {
                  actions.push(
                    new sourceExplorerActions.SetCustomFilter(
                      source,
                      hasQueryStringArgs[2],
                    ),
                  );
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
   * Helper. wrap action with loadingBar fetchPending
   */
  withLoadingBar(actions$: Observable<Action>): Observable<Action> {
    return concat(
      of(
        new sourceExplorerActions.UpdateSourceExplorer({
          fetchPending: true,
        }),
      ),
      actions$,
      of(
        new sourceExplorerActions.UpdateSourceExplorer({
          fetchPending: false,
        }),
      ),
    );
  }
}
