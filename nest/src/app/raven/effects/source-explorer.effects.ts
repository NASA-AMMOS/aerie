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
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import flatten from 'lodash-es/flatten';
import uniqueId from 'lodash-es/uniqueId';
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
import { ConfigActions, ToastActions } from '../../shared/actions';
import { StringTMap } from '../../shared/models';
import {
  getSituationalAwarenessPageDuration,
  getSituationalAwarenessStartTime,
  utc,
} from '../../shared/util';
import {
  DialogActions,
  EpochsActions,
  LayoutActions,
  SourceExplorerActions,
  TimelineActions,
} from '../actions';
import {
  FilterState,
  MpsServerGraphData,
  MpsServerSource,
  RavenActivityBand,
  RavenCompositeBand,
  RavenCustomFilter,
  RavenCustomFilterSource,
  RavenDefaultBandSettings,
  RavenEpoch,
  RavenFilterSource,
  RavenGraphableFilterSource,
  RavenOpenArgs,
  RavenPin,
  RavenSource,
  RavenState,
  RavenStateBand,
  RavenSubBand,
  SourceFilter,
} from '../models';
import { RavenAppState } from '../raven-store';
import { LayoutState } from '../reducers/layout.reducer';
import * as fromTimeline from '../reducers/timeline.reducer';
import { MpsServerService } from '../services/mps-server.service';
import {
  activityBandsWithLegendAndSourceId,
  getActivityPointInBand,
  getAddToSubBandId,
  getBandsWithSourceId,
  getCustomFilterForLabel,
  getCustomFiltersBySourceId,
  getFormattedSourceUrl,
  getMpsPathForSource,
  getParentSourceIds,
  getPinLabel,
  getRavenState,
  getSourceIds,
  getSourceNameFromId,
  getState,
  getTargetFilters,
  hasActivityBandForFilterTarget,
  isAddTo,
  isOverlay,
  subBandById,
  toCompositeBand,
  toRavenBandData,
  toRavenSources,
  updateSourceId,
} from '../util';
import { withLoadingBar } from './utils';

@Injectable()
export class SourceExplorerEffects {
  constructor(
    private actions: Actions,
    private http: HttpClient,
    private mpsServerService: MpsServerService,
    private store: Store<RavenAppState>,
  ) {}

  addCustomGraph = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.addCustomGraph),
      withLatestFrom(this.store),
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
            this.open({
              bandId: timeline.selectedBandId,
              currentBands: timeline.bands,
              customFilter: getCustomFilterForLabel(
                label,
                sourceExplorer.customFiltersBySourceId[sourceId],
              ),
              defaultBandSettings: config.raven.defaultBandSettings,
              filtersByTarget: sourceExplorer.filtersByTarget,
              graphAgain: true,
              pageDuration: getSituationalAwarenessPageDuration(
                situationalAwareness,
              ),
              pins: sourceExplorer.pins,
              restoringLayout: false,
              situAware: situationalAwareness.situationalAware,
              sourceId,
              startTime: getSituationalAwarenessStartTime(situationalAwareness),
              subBandId: timeline.selectedSubBandId,
              treeBySourceId: sourceExplorer.treeBySourceId,
            }),
            of(SourceExplorerActions.loadErrorsDisplay()),
            of(
              SourceExplorerActions.updateSourceExplorer({
                update: { fetchPending: false },
              }),
            ),
          ),
      ),
    ),
  );

  addGraphableFilter = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.addGraphableFilter),
      concatMap(({ source }) =>
        concat(
          of(SourceExplorerActions.addFilter({ source })),
          of(
            SourceExplorerActions.updateGraphAfterFilterAdd({
              sourceId: source.id,
            }),
          ),
        ),
      ),
    ),
  );

  applyCurrentState = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.applyCurrentState),
      withLatestFrom(this.store),
      map(([, state]) => ({ state })),
      concatMap(({ state }) =>
        forkJoin([
          of(state),
          this.mpsServerService.fetchNewSources(
            `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}`,
            '/',
            true,
            null,
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
            state.raven.timeline.currentState,
            state.raven.timeline.currentStateId,
          ),
        ),
      ),
    ),
  );

  /**
   * @note Right now we are only applying layouts to the `fs_file` type.
   */
  applyLayout = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.applyLayout),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      concatMap(({ action, state }) =>
        forkJoin([
          of(action),
          of(state),
          this.mpsServerService.fetchNewSources(
            `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}`,
            '/',
            true,
            null,
          ),
        ]),
      ),
      map(([action, state, initialSources]) => ({
        action,
        initialSources,
        state,
      })),
      concatMap(({ action, state, initialSources }) => {
        const savedState = state.raven.timeline.currentState as RavenState;

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
      concatMap(
        ({ action, state, savedState, initialSources, sourceTypes }) => {
          let updatedBands: RavenCompositeBand[] = [];
          if (Object.keys(action.update.pins).length > 0) {
            // Apply layout with pins.
            updatedBands = savedState.bands.map((band: RavenCompositeBand) => {
              const parentId = uniqueId();

              return {
                ...band,
                id: parentId,
                subBands: band.subBands.map((subBand: RavenSubBand) => {
                  if (subBand.type === 'divider') {
                    return {
                      ...subBand,
                    };
                  } else {
                    const labelPin = subBand.labelPin;
                    const pin = action.update.pins[labelPin];
                    const targetSource =
                      state.raven.sourceExplorer.treeBySourceId[pin.sourceId];

                    return {
                      ...subBand,
                      id: uniqueId(),
                      parentUniqueId: parentId,
                      sourceIds: subBand.sourceIds.map((sourceId: string) =>
                        updateSourceId(
                          sourceId,
                          targetSource.id,
                          sourceTypes,
                          targetSource.type,
                        ),
                      ),
                    };
                  }
                }),
              };
            });
          } else {
            action.update.targetSourceIds.forEach((targetSourceId: string) => {
              targetSourceId = targetSourceId.trim();
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
                    sourceIds: subBand.sourceIds.map((sourceId: string) =>
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
          }
          return concat(
            ...this.loadLayout(
              updatedBands,
              initialSources,
              savedState,
              state.raven.timeline.currentStateId,
              Object.values(action.update.pins),
            ),
          );
        },
      ),
    ),
  );

  applyLayoutToSources = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.applyLayoutToSources),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      concatMap(({ action, state }) =>
        forkJoin([
          of(action),
          this.mpsServerService.fetchState(action.layoutSourceUrl),
          this.mpsServerService.fetchNewSources(
            `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}`,
            '/',
            true,
            null,
          ),
        ]),
      ),
      map(([action, savedState, initialSources]) => ({
        action,
        initialSources,
        savedState,
      })),
      concatMap(({ action, initialSources, savedState }) =>
        concat(
          of(
            SourceExplorerActions.newSources({
              sourceId: '/',
              sources: initialSources,
            }),
          ),
          of(
            TimelineActions.updateTimeline({
              update: { currentState: savedState },
            }),
          ),
          ...this.expandSourcePaths(action.sourcePaths),
          of(
            SourceExplorerActions.applyLayout({
              update: {
                pins: savedState.pins.reduce((allPins, pin) => {
                  allPins[pin.name] = pin.sourceId;
                  return allPins;
                }, {}),
                targetSourceIds: action.sourcePaths,
              },
            }),
          ),
        ),
      ),
    ),
  );

  applyState = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.applyState),
      withLatestFrom(this.store),
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
            null,
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
    ),
  );

  applyStateOrLayoutSuccess = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.applyStateOrLayoutSuccess),
      withLatestFrom(this.store),
      map(([, state]) => state),
      concatMap(state =>
        concat(
          of(
            TimelineActions.updateTimeline({
              update: { currentStateChanged: false },
            }),
          ),
          this.restoreExpansion(
            state.raven.timeline.bands,
            state.raven.timeline.expansionByActivityId,
          ),
        ),
      ),
    ),
  );

  closeEvent = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.closeEvent),
      switchMap(({ sourceId }) => [
        TimelineActions.removeBandsOrPointsForSource({ sourceId }),
        SourceExplorerActions.updateTreeSource({
          sourceId,
          update: { opened: false },
        }),
        LayoutActions.resize(), // Resize bands when we `close` to make sure they are all resized properly.
      ]),
    ),
  );

  createFolder = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.folderAdd),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      concatMap(({ action, state }) => {
        const headers = new HttpHeaders().set(
          'Content-Type',
          `application/json`,
        );
        const responseType = 'text';
        const url = `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}${action.folder.url}/${action.folder.name}`;

        const request = this.http.put(url, '', { headers, responseType }).pipe(
          concatMap(() => {
            return of(SourceExplorerActions.folderAddSuccess());
          }),
          catchError((e: Error) => {
            console.error('SourceExplorerEffects - createFolder: ', e);
            return [
              ToastActions.showToast({
                message: 'Failed To Create Folder',
                title: '',
                toastType: 'warning',
              }),
              SourceExplorerActions.folderAddFailure(),
            ];
          }),
        );

        return concat(
          withLoadingBar([request]),
          of(
            SourceExplorerActions.expandEvent({ sourceId: action.folder.url }),
          ),
        );
      }),
    ),
  );

  expandEvent = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.expandEvent),
      withLatestFrom(this.store),
      map(
        ([action, state]) =>
          state.raven.sourceExplorer.treeBySourceId[action.sourceId],
      ),
      switchMap(source =>
        concat(
          ...this.expand(source),
          of(
            SourceExplorerActions.updateSourceExplorer({
              update: { fetchPending: false },
            }),
          ),
          of(
            SourceExplorerActions.updateTreeSource({
              sourceId: source.id,
              update: { expanded: true },
            }),
          ),
        ).pipe(
          catchError((e: Error) => {
            console.error('SourceExplorerEffects - expandEvent: ', e.message);
            return [
              ToastActions.showToast({
                message: `Failed To Expand Source "${source.name}"`,
                title: '',
                toastType: 'warning',
              }),

              SourceExplorerActions.updateSourceExplorer({
                update: { fetchPending: false },
              }),
              SourceExplorerActions.updateTreeSource({
                sourceId: source.id,
                update: { expanded: false },
              }),
            ];
          }),
        ),
      ),
    ),
  );

  fetchInitialSources = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.fetchInitialSources),
      withLatestFrom(this.store),
      map(([, state]) => state),
      concatMap((state: RavenAppState) =>
        concat(
          this.mpsServerService
            .fetchNewSources(
              `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}`,
              '/',
              true,
              null,
            )
            .pipe(
              map((sources: RavenSource[]) =>
                SourceExplorerActions.newSources({ sourceId: '/', sources }),
              ),
            ),
          of(
            SourceExplorerActions.updateSourceExplorer({
              update: {
                fetchPending: false,
                initialSourcesLoaded: true,
              },
            }),
          ),
        ).pipe(
          catchError((e: Error) => {
            console.error(
              'SourceExplorerEffects - fetchInitialSources: ',
              e.message,
            );
            return [
              ToastActions.showToast({
                message: 'Failed To Fetch Initial Sources',
                title: '',
                toastType: 'warning',
              }),
              SourceExplorerActions.updateSourceExplorer({
                update: {
                  fetchPending: false,
                  initialSourcesLoaded: false,
                },
              }),
            ];
          }),
        ),
      ),
    ),
  );

  fetchNewSources = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.fetchNewSources),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      concatMap(({ action, state }) =>
        concat(
          this.mpsServerService
            .fetchNewSources(
              action.sourceUrl,
              action.sourceId,
              false,
              state.raven.sourceExplorer.treeBySourceId,
            )
            .pipe(
              concatMap((sources: RavenSource[]) => [
                SourceExplorerActions.newSources({
                  sourceId: action.sourceId,
                  sources,
                }),
              ]),
            ),
          of(
            SourceExplorerActions.updateSourceExplorer({
              update: { fetchPending: false },
            }),
          ),
        ).pipe(
          catchError((e: Error) => {
            console.error(
              'SourceExplorerEffects - fetchNewSources: ',
              e.message,
            );
            return [
              ToastActions.showToast({
                message: 'Failed To Fetch Sources',
                title: '',
                toastType: 'warning',
              }),
              SourceExplorerActions.updateSourceExplorer({
                update: { fetchPending: false },
              }),
            ];
          }),
        ),
      ),
    ),
  );

  graphCustomSource = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.graphCustomSource),
      concatMap(action =>
        concat(
          of(
            SourceExplorerActions.addCustomFilter({
              customFilter: action.filter,
              label: action.label,
              sourceId: action.sourceId,
            }),
          ),
          of(
            SourceExplorerActions.addCustomGraph({
              customFilter: action.filter,
              label: action.label,
              sourceId: action.sourceId,
            }),
          ),
        ),
      ),
    ),
  );

  graphAgainEvent = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.graphAgainEvent),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      mergeMap(({ state: { config, raven }, action }) =>
        withLoadingBar([
          this.open({
            bandId: raven.timeline.selectedBandId,
            currentBands: raven.timeline.bands,
            customFilter: null,
            defaultBandSettings: config.raven.defaultBandSettings,
            filtersByTarget: raven.sourceExplorer.filtersByTarget,
            graphAgain: true,
            pageDuration: getSituationalAwarenessPageDuration(
              raven.situationalAwareness,
            ),
            pins: raven.sourceExplorer.pins,
            restoringLayout: false,
            situAware: raven.situationalAwareness.situationalAware,
            sourceId: action.sourceId,
            startTime: getSituationalAwarenessStartTime(
              raven.situationalAwareness,
            ),
            subBandId: raven.timeline.selectedSubBandId,
            treeBySourceId: raven.sourceExplorer.treeBySourceId,
          }),
        ]).pipe(
          catchError((e: Error) => {
            console.error(
              'SourceExplorerEffects - graphAgainEvent: ',
              e.message,
            );
            return [
              ToastActions.showToast({
                message: 'Failed To Graph Again Source',
                title: '',
                toastType: 'warning',
              }),
              SourceExplorerActions.updateSourceExplorer({
                update: { fetchPending: false },
              }),
            ];
          }),
        ),
      ),
    ),
  );

  importFile = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.importFile),
      concatMap(action => {
        const headers = new HttpHeaders().set(
          'Content-Type',
          `${action.file.type === 'pef' ? 'application/json' : 'text/csv'}`,
        );
        const url = `${action.source.url}/${action.file.name}?timeline_type=${action.file.type}&time_format=${action.file.timeFormat}`;

        const importFileRequest = this.http
          .put(url, action.file.data, {
            headers: headers,
            responseType: 'text',
          })
          .pipe(
            concatMap(() => {
              if (action.file.mapping) {
                return this.mpsServerService
                  .importMappingFile(
                    action.source.url,
                    action.file.name,
                    action.file.mapping,
                  )
                  .pipe(map(() => SourceExplorerActions.importFileSuccess()));
              } else {
                return of(SourceExplorerActions.importFileSuccess());
              }
            }),
            catchError((e: Error) => {
              console.error('SourceExplorerEffects - importFile: ', e.message);
              return [
                ToastActions.showToast({
                  message: 'Failed To Import File',
                  title: '',
                  toastType: 'warning',
                }),
                SourceExplorerActions.importFileFailure(),
              ];
            }),
          );

        return withLoadingBar([importFileRequest]);
      }),
    ),
  );

  loadErrorsDisplay = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.loadErrorsDisplay),
      withLatestFrom(this.store),
      map(([, state]) => state.raven.sourceExplorer.loadErrors),
      switchMap(loadErrors => {
        if (loadErrors.length) {
          const errors = loadErrors.map(error => `${error}\n\n`);

          return [
            DialogActions.openConfirmDialog({
              cancelText: 'OK',
              message: `Data sets empty or do not exist.\nTimeline will not be drawn for:\n\n ${errors}`,
              width: '350px ',
            }),
            SourceExplorerActions.updateSourceExplorer({
              update: { loadErrors: [] },
            }),
          ];
        }
        return [];
      }),
    ),
  );

  openEvent = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.openEvent),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      mergeMap(({ state: { config, raven }, action }) =>
        concat(
          this.open({
            bandId: raven.timeline.selectedBandId,
            currentBands: raven.timeline.bands,
            customFilter: null,
            defaultBandSettings: config.raven.defaultBandSettings,
            filtersByTarget: raven.sourceExplorer.filtersByTarget,
            graphAgain: false,
            pageDuration: getSituationalAwarenessPageDuration(
              raven.situationalAwareness,
            ),
            pins: raven.sourceExplorer.pins,
            restoringLayout: false,
            situAware: raven.situationalAwareness.situationalAware,
            sourceId: action.sourceId,
            startTime: getSituationalAwarenessStartTime(
              raven.situationalAwareness,
            ),
            subBandId: raven.timeline.selectedSubBandId,
            treeBySourceId: raven.sourceExplorer.treeBySourceId,
          }),
          of(
            SourceExplorerActions.updateSourceExplorer({
              update: { fetchPending: false },
            }),
          ),
          of(
            SourceExplorerActions.updateTreeSource({
              sourceId: action.sourceId,
              update: { opened: true },
            }),
          ),
        ).pipe(
          catchError((e: Error) => {
            console.error('SourceExplorerEffects - openEvent: ', e.message);
            return [
              ToastActions.showToast({
                message: 'Failed To Open Source',
                title: '',
                toastType: 'warning',
              }),
              SourceExplorerActions.updateSourceExplorer({
                update: { fetchPending: false },
              }),
              SourceExplorerActions.updateTreeSource({
                sourceId: action.sourceId,
                update: { opened: false },
              }),
            ];
          }),
        ),
      ),
    ),
  );

  removeGraphableFilter = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.removeGraphableFilter),
      concatMap(action =>
        concat(
          of(
            SourceExplorerActions.removeFilter({
              source: action.source as RavenFilterSource,
            }),
          ),
          of(
            SourceExplorerActions.subBandIdRemove({
              sourceIds: [action.source.id],
              subBandId: action.source.subBandIds[0],
            }),
          ),
          of(
            SourceExplorerActions.updateGraphAfterFilterRemove({
              sourceId: action.source.id,
            }),
          ),
        ),
      ),
    ),
  );

  removeSourceEvent = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.removeSourceEvent),
      concatMap(action =>
        concat(
          this.mpsServerService
            .removeSource(action.source.url, action.source.id)
            .pipe(
              map(() =>
                SourceExplorerActions.removeSource({
                  sourceId: action.source.id,
                }),
              ),
            ),
          of(
            SourceExplorerActions.updateSourceExplorer({
              update: { fetchPending: false },
            }),
          ),
        ),
      ),
    ),
  );

  saveState = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.saveState),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      concatMap(({ state, action }) =>
        this.mpsServerService
          .saveState(
            action.source.url,
            action.name,
            getState(action.name, state),
          )
          .pipe(
            concatMap(() =>
              of(
                TimelineActions.updateTimeline({
                  update: {
                    currentState: getRavenState(action.name, state),
                    currentStateChanged: false,
                    currentStateId: `${action.source.id}/${action.name}`,
                  },
                }),
                SourceExplorerActions.updateSourceExplorer({
                  update: { fetchPending: false },
                }),
              ),
            ),
          ),
      ),
    ),
  );

  updateGraphAfterFilterAdd = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.updateGraphAfterFilterAdd),
      withLatestFrom(this.store),
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
              withLatestFrom(this.store),
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
                      TimelineActions.sourceIdAdd({
                        sourceId: action.sourceId,
                        subBandId: activityBand.subBandId,
                      }),
                      // Replace points in band.
                      TimelineActions.setPointsForSubBand({
                        bandId: activityBand.bandId,
                        points: subBand.points,
                        subBandId: activityBand.subBandId,
                      }),
                    );
                  } else {
                    actions.push(
                      SourceExplorerActions.subBandIdAdd({
                        sourceId: action.sourceId,
                        subBandId: subBand.id,
                      }),
                      TimelineActions.addBand({
                        band: toCompositeBand(subBand),
                        modifiers: {
                          additionalSubBandProps: {
                            filterTarget: (sourceExplorer.treeBySourceId[
                              action.sourceId
                            ] as RavenFilterSource).filterTarget,
                          },
                        },
                        sourceId: action.sourceId,
                      }),
                    );
                  }
                });

                return actions;
              }),
            ),
            of(
              SourceExplorerActions.updateSourceExplorer({
                update: { fetchPending: false },
              }),
            ),
          ),
      ),
    ),
  );

  updateGraphAfterFilterRemove = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.updateGraphAfterFilterRemove),
      withLatestFrom(this.store),
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
                        TimelineActions.removeSourceIdFromSubBands({
                          sourceId: action.sourceId,
                        }),
                        // Replace band points.
                        TimelineActions.setPointsForSubBand({
                          bandId: activityBand.bandId,
                          points: subBand.points,
                          subBandId: activityBand.subBandId,
                        }),
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
                      TimelineActions.removeSourceIdFromSubBands({
                        sourceId: action.sourceId,
                      }),
                      // Set empty data points.
                      TimelineActions.setPointsForSubBand({
                        bandId: activityBand.bandId,
                        points: [],
                        subBandId: activityBand.subBandId,
                      }),
                    );
                  }
                }

                return actions;
              }),
            ),
            of(
              SourceExplorerActions.updateSourceExplorer({
                update: { fetchPending: false },
              }),
            ),
          ),
      ),
    ),
  );

  updateCurrentState = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.updateCurrentState),
      withLatestFrom(this.store),
      map(([, state]) => ({ state })),
      concatMap(({ state }) =>
        this.mpsServerService
          .updateState(
            `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}${state.raven.timeline.currentStateId}`,
            getState(
              getSourceNameFromId(state.raven.timeline.currentStateId),
              state,
            ),
          )
          .pipe(
            concatMap(() =>
              of(
                TimelineActions.updateTimeline({
                  update: {
                    currentState: getRavenState(
                      getSourceNameFromId(state.raven.timeline.currentStateId),
                      state,
                    ),
                  },
                }),
                SourceExplorerActions.updateSourceExplorer({
                  update: { fetchPending: false },
                }),
              ),
            ),
          ),
      ),
    ),
  );

  updateSourceFilter = createEffect(() =>
    this.actions.pipe(
      ofType(SourceExplorerActions.updateSourceFilter),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      map(({ action, state }) => {
        if (SourceFilter.isEmpty(action.sourceFilter)) {
          // Remove all filtering.
          return of(FilterState.empty());
        } else {
          // Apply a new filter.
          const url = `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}`;

          return this.mpsServerService
            .getSourcesMatchingFilter(url, action.sourceFilter)
            .pipe(
              map(mpsServerSources =>
                mpsServerSources.map(getMpsPathForSource),
              ),
              map(sourceIds =>
                FilterState.fromMatches(action.sourceFilter, sourceIds),
              ),
            );
        }
      }),
      mergeMap((filterState$: Observable<FilterState>) =>
        concat(
          of(
            SourceExplorerActions.updateSourceExplorer({
              update: {
                fetchPending: true,
              },
            }),
          ),
          filterState$.pipe(
            concatMap((filterState: FilterState) =>
              of(
                SourceExplorerActions.updateSourceExplorer({
                  update: {
                    fetchPending: false,
                    filterState,
                  },
                }),
              ),
            ),
          ),
        ),
      ),
    ),
  );

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
              SourceExplorerActions.newSources({
                sourceId: source.id,
                sources: toRavenSources(source.id, false, source.content, null),
              }),
            ),
          );
        } else {
          actions.push(
            this.mpsServerService
              .fetchNewSources(source.url, source.id, false, null)
              .pipe(
                concatMap((sources: RavenSource[]) => [
                  SourceExplorerActions.newSources({
                    sourceId: source.id,
                    sources,
                  }),
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

  expandSourcePaths(sourcePaths: string[]): Observable<Action>[] {
    return sourcePaths.reduce((actions: Observable<Action>[], sourcePath) => {
      actions.push(
        ...getParentSourceIds(sourcePath).map((sourceId: string) =>
          combineLatest([this.store]).pipe(
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
                    SourceExplorerActions.updateTreeSource({
                      sourceId: source.id,
                      update: { expanded: true },
                    }),
                  ),
                );
              } else {
                console.log(
                  'source-explorer.effect: load: source does not exist: ',
                  sourceId,
                );
                return of(
                  SourceExplorerActions.loadErrorsAdd({
                    sourceIds: [sourceId],
                  }),
                );
              }
            }),
          ),
        ),
      );
      return actions;
    }, []);
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
    situAware: boolean,
    startTime: string,
    pageDuration: string,
  ) {
    const source = treeBySourceId[sourceId];
    const graphables = ['graphableFilter', 'customFilter', 'customGraphable'];
    if (source.openable || graphables.includes(source.type)) {
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
    } else {
      return of([]);
    }
  }

  /**
   * Helper. Returns a stream of actions that need to occur when loading a layout.
   */
  loadLayout(
    bands: RavenCompositeBand[],
    initialSources: RavenSource[],
    savedState: RavenState,
    sourceId: string,
    pins: RavenPin[],
  ) {
    return [
      of(
        SourceExplorerActions.updateSourceExplorer({
          update: { fetchPending: true },
        }),
      ),
      of(
        TimelineActions.updateTimeline({
          update: {
            ...fromTimeline.initialState,
            bands: bands.map(band => ({
              ...band,
              subBands: band.subBands.map((subBand: RavenSubBand) => ({
                ...subBand,
                sourceIds: [],
              })),
            })),
            currentState: savedState,
            currentStateId: sourceId,
            expansionByActivityId: savedState.expansionByActivityId,
          },
        }),
      ),
      of(
        ConfigActions.updateDefaultBandSettings({
          update: {
            ...savedState.defaultBandSettings,
          },
        }),
      ),
      ...this.load(bands, initialSources, pins, true),
      of(TimelineActions.removeBandsWithNoPoints()),
      ...pins.map(pin => of(SourceExplorerActions.pinAdd({ pin }))),
      ...pins.map(pin => of(TimelineActions.pinAdd({ pin }))),
      of(TimelineActions.resetViewTimeRange()),
      of(SourceExplorerActions.applyStateOrLayoutSuccess()),
      of(
        SourceExplorerActions.updateSourceExplorer({
          update: { fetchPending: false },
        }),
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
          SourceExplorerActions.updateSourceExplorer({
            update: { fetchPending: true },
          }),
        ),
        ...this.updateInUseEpoch(savedState.inUseEpoch),
        ...this.updatePanels(savedState, state.raven.layout),
        of(
          TimelineActions.updateTimeline({
            update: {
              ...fromTimeline.initialState,
              bands: savedState.bands.map(band => ({
                ...band,
                subBands: band.subBands.map((subBand: RavenSubBand) => ({
                  ...subBand,
                  sourceIds: [],
                })),
              })),
              currentState: savedState,
              currentStateId: sourceId,
              expansionByActivityId: savedState.expansionByActivityId,
              guides: savedState.guides ? savedState.guides : [],
              maxTimeRange: savedState.maxTimeRange,
              viewTimeRange: savedState.ignoreShareableLinkTimes
                ? { end: 0, start: 0 }
                : {
                    end: utc(savedState.viewTimeRange.end),
                    start: utc(savedState.viewTimeRange.start),
                  },
            },
          }),
        ),
        of(
          ConfigActions.updateDefaultBandSettings({
            update: {
              ...savedState.defaultBandSettings,
            },
          }),
        ),
        ...this.load(savedState.bands, initialSources, savedState.pins, false),
        ...savedState.pins.map(pin =>
          of(SourceExplorerActions.pinAdd({ pin })),
        ),
        ...savedState.pins.map(pin => of(TimelineActions.pinAdd({ pin }))),
        of(SourceExplorerActions.applyStateOrLayoutSuccess()),
        of(
          SourceExplorerActions.updateSourceExplorer({
            update: { fetchPending: false },
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
    restoringLayout: boolean,
  ): Observable<Action>[] {
    const { parentSourceIds, sourceIds } = getSourceIds(bands);

    return [
      of(
        SourceExplorerActions.newSources({
          sourceId: '/',
          sources: initialSources,
        }),
      ),
      ...parentSourceIds.map((sourceId: string) =>
        combineLatest([this.store]).pipe(
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
                  SourceExplorerActions.updateTreeSource({
                    sourceId: source.id,
                    update: { expanded: true },
                  }),
                ),
              );
            } else {
              console.log(
                'source-explorer.effect: load: source does not exist: ',
                sourceId,
              );
              return of(
                SourceExplorerActions.loadErrorsAdd({ sourceIds: [sourceId] }),
              );
            }
          }),
        ),
      ),
      combineLatest([this.store]).pipe(
        take(1),
        map((state: RavenAppState[]) => state[0]),
        concatMap((state: RavenAppState) =>
          // Restore filters after the explorer tree has been restored since we need to know the source type.
          concat(
            of(
              SourceExplorerActions.updateSourceExplorer({
                update: {
                  customFiltersBySourceId: getCustomFiltersBySourceId(
                    bands,
                    state.raven.sourceExplorer.treeBySourceId,
                  ),
                },
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
        combineLatest([this.store]).pipe(
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
                    sourceIds: subBand.sourceIds.map((srcId: string) =>
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
                restoringLayout,
              ),
              of(
                SourceExplorerActions.updateTreeSource({
                  sourceId,
                  update: { opened: true },
                }),
              ),
            ),
          ),
        ),
      ),
      of(LayoutActions.resize()),
      of(SourceExplorerActions.loadErrorsDisplay()),
    ];
  }

  /**
   * Helper. Returns a stream of actions that need to occur when opening a source explorer source.
   * The order of the cases in this function are very important. Do not change the order.
   */
  open({
    bandId,
    currentBands,
    customFilter,
    defaultBandSettings,
    filtersByTarget,
    graphAgain,
    pageDuration,
    pins,
    restoringLayout,
    situAware,
    sourceId,
    startTime,
    subBandId,
    treeBySourceId,
  }: RavenOpenArgs) {
    return this.fetchSubBands(
      treeBySourceId,
      sourceId,
      defaultBandSettings,
      customFilter,
      situAware,
      startTime,
      pageDuration,
    ).pipe(
      concatMap((newSubBands: RavenSubBand[]) => {
        const actions: Action[] = [];
        if (treeBySourceId[sourceId].type === 'graphableFilter') {
          // Clear existing points regardless if fetch returns any data.
          actions.push(
            TimelineActions.removeAllPointsInSubBandWithParentSource({
              parentSourceId: treeBySourceId[sourceId].parentId,
            }),
          );
        }
        if (newSubBands.length > 0) {
          newSubBands.forEach((subBand: RavenSubBand) => {
            const activityBands = activityBandsWithLegendAndSourceId(
              currentBands,
              subBand,
              getPinLabel(treeBySourceId[sourceId].id, pins),
              restoringLayout ? sourceId : '',
            );
            const existingBands = graphAgain
              ? []
              : getBandsWithSourceId(currentBands, sourceId);
            if (!graphAgain && activityBands.length > 0) {
              activityBands.forEach(activityBand => {
                actions.push(
                  SourceExplorerActions.subBandIdAdd({
                    sourceId,
                    subBandId: activityBand.subBandId,
                  }),
                  TimelineActions.addPointsToSubBand({
                    bandId: activityBand.bandId,
                    points: subBand.points,
                    sourceId,
                    subBandId: activityBand.subBandId,
                  }),
                );
                const band = subBandById(
                  currentBands,
                  activityBand.bandId,
                  activityBand.subBandId,
                ) as RavenActivityBand;
                if (band && band.activityFilter !== '') {
                  actions.push(
                    TimelineActions.filterActivityInSubBand({
                      activityInitiallyHidden:
                        defaultBandSettings.activityInitiallyHidden,
                      bandId: activityBand.bandId,
                      filter: band.activityFilter,
                      subBandId: activityBand.subBandId,
                    }),
                  );
                }
              });
            } else if (existingBands.length > 0) {
              existingBands.forEach(existingBand => {
                if (subBand.type === 'state') {
                  actions.push(
                    SourceExplorerActions.subBandIdAdd({
                      sourceId,
                      subBandId: existingBand.subBandId,
                    }),
                    TimelineActions.setPointsForSubBand({
                      bandId: existingBand.bandId,
                      points: subBand.points,
                      subBandId: existingBand.subBandId,
                    }),
                    // Add source id to existing band.
                    TimelineActions.sourceIdAdd({
                      sourceId,
                      subBandId: existingBand.subBandId,
                    }),

                    // Use new possibleStates since that can change from one adaptation to another.
                    TimelineActions.updateSubBand({
                      bandId: existingBand.bandId,
                      subBandId: existingBand.subBandId,
                      update: {
                        possibleStates: (subBand as RavenStateBand)
                          .possibleStates,
                      },
                    }),
                  );
                } else if (
                  subBand.type !== 'activity' ||
                  isAddTo(
                    currentBands,
                    existingBand.bandId,
                    existingBand.subBandId,
                    'activity',
                  )
                ) {
                  actions.push(
                    SourceExplorerActions.subBandIdAdd({
                      sourceId,
                      subBandId: existingBand.subBandId,
                    }),
                    TimelineActions.addPointsToSubBand({
                      bandId: existingBand.bandId,
                      points: subBand.points,
                      sourceId,
                      subBandId: existingBand.subBandId,
                    }),
                  );
                }
              });
            } else if (
              subBand.type === 'activity' &&
              bandId &&
              getAddToSubBandId(currentBands, bandId)
            ) {
              const addToSubBandId = getAddToSubBandId(currentBands, bandId);
              if (addToSubBandId) {
                actions.push(
                  SourceExplorerActions.subBandIdAdd({
                    sourceId,
                    subBandId: addToSubBandId,
                  }),
                  TimelineActions.addPointsToSubBand({
                    bandId,
                    points: subBand.points,
                    sourceId,
                    subBandId: addToSubBandId,
                  }),
                );
              }
            } else if (bandId && isOverlay(currentBands, bandId)) {
              actions.push(
                SourceExplorerActions.subBandIdAdd({
                  sourceId,
                  subBandId: subBand.id,
                }),
                TimelineActions.addSubBand({ sourceId, bandId, subBand }),
                TimelineActions.setCompositeYLabelDefault({ bandId }),
              );
            } else {
              actions.push(
                SourceExplorerActions.subBandIdAdd({
                  sourceId,
                  subBandId: subBand.id,
                }),
                TimelineActions.addBand({
                  band: toCompositeBand(subBand),
                  sourceId,
                }),
              );
            }

            // Make sure that if we are dealing with a custom graphable source that we update the custom filters with the new sub-band id.
            if (treeBySourceId[sourceId].type === 'customGraphable') {
              actions.push(
                SourceExplorerActions.setCustomFilterSubBandId({
                  label: customFilter ? customFilter.label : '',
                  sourceId,
                  subBandId: subBand.id,
                }),
              );
            }
          });
        } else {
          // Notify user no bands will be drawn.
          actions.push(
            SourceExplorerActions.loadErrorsAdd({ sourceIds: [sourceId] }),
          );

          // Remove custom filter if custom source
          if (
            treeBySourceId[sourceId].type === 'customGraphable' &&
            customFilter
          ) {
            actions.push(
              SourceExplorerActions.removeCustomFilter({
                label: customFilter.label,
                sourceId,
              }),
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
    restoringLayout: boolean,
  ): Observable<Action>[] {
    if (customFilters) {
      return customFilters.map(customFilter =>
        this.open({
          bandId: null,
          currentBands,
          customFilter,
          defaultBandSettings,
          filtersByTarget,
          graphAgain: false,
          pageDuration,
          pins,
          restoringLayout,
          situAware,
          sourceId,
          startTime,
          subBandId: null,
          treeBySourceId,
        }),
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
            SourceExplorerActions.addGraphableFilter({
              source: treeBySourceId[sourceId] as RavenGraphableFilterSource,
            }),
          ),
        ];
      } else {
        return [
          this.open({
            bandId: null,
            currentBands,
            customFilter: null,
            defaultBandSettings,
            filtersByTarget,
            graphAgain: false,
            pageDuration,
            pins,
            restoringLayout,
            situAware,
            sourceId,
            startTime,
            subBandId: null,
            treeBySourceId,
          }),
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
            `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}${sourceId}`,
          )
          .pipe(
            timeout(3000), // Timeout long requests since MPS Server returns type information quickly, and long requests probably are not what we are looking for.
            map((mpsServerSources: MpsServerSource[]) =>
              toRavenSources('', false, mpsServerSources, null),
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
          TimelineActions.expandChildrenOrDescendants({
            activityPoint: activityPointInBand.activityPoint,
            bandId: activityPointInBand.bandId,
            expandType: expansionByActivityId[activityId],
            subBandId: activityPointInBand.subBandId,
          }),
          TimelineActions.fetchChildrenOrDescendants({
            activityPoint: activityPointInBand.activityPoint,
            bandId: activityPointInBand.bandId,
            expandType: expansionByActivityId[activityId],
            subBandId: activityPointInBand.subBandId,
          }),
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
        subBand.sourceIds.forEach((sourceId: string) => {
          if (
            treeBySourceId[sourceId] &&
            treeBySourceId[sourceId].type === 'filter'
          ) {
            actions.push(
              SourceExplorerActions.addFilter({
                source: treeBySourceId[sourceId] as RavenFilterSource,
              }),
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
                    SourceExplorerActions.setCustomFilter({
                      filter: hasQueryStringArgs[2],
                      source,
                    }),
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
   * Helper. Returns an action if inUseEpoch is not null.
   */
  updateInUseEpoch(inUseEpoch: RavenEpoch | null) {
    const actions: Observable<Action>[] = [];
    if (inUseEpoch) {
      actions.push(
        of(EpochsActions.setInUseEpochByName({ epochName: inUseEpoch.name })),
      );
    }
    return actions;
  }

  /**
   * Helper. Returns a stream of actions that need to occur when restoring the layout of the panels.
   */
  updatePanels(
    savedState: RavenState,
    layout: LayoutState,
  ): Observable<Action>[] {
    const actions: Observable<Action>[] = [];
    if (savedState.showDetailsPanel !== layout.showDetailsPanel) {
      actions.push(of(LayoutActions.toggleDetailsPanel()));
    }
    if (savedState.showRightPanel !== layout.showRightPanel) {
      actions.push(of(LayoutActions.toggleRightPanel()));
    }
    if (savedState.showLeftPanel !== layout.showLeftPanel) {
      actions.push(of(LayoutActions.toggleLeftPanel()));
    }
    if (savedState.showSouthBandsPanel !== layout.showSouthBandsPanel) {
      actions.push(of(LayoutActions.toggleSouthBandsPanel()));
    }
    return actions;
  }
}
