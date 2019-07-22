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
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { of } from 'rxjs';
import {
  catchError,
  concatMap,
  map,
  mergeMap,
  withLatestFrom,
} from 'rxjs/operators';
import * as stripJsonComments from 'strip-json-comments';
import { ConfigState } from '../../../config';
import {
  EpochsActions,
  LayoutActions,
  SourceExplorerActions,
  TimeCursorActions,
  TimelineActions,
} from '../../raven/actions';
import { RavenAppState } from '../../raven/raven-store';
import { LayoutState } from '../../raven/reducers/layout.reducer';
import { ConfigActions } from '../actions';

@Injectable()
export class ConfigEffects {
  constructor(
    private actions: Actions,
    private http: HttpClient,
    private store: Store<RavenAppState>,
  ) {}

  fetchProjectConfig = createEffect(() =>
    this.actions.pipe(
      ofType(ConfigActions.fetchProjectConfig),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      concatMap(({ action, state }) =>
        this.http.get(action.url, { responseType: 'text' }).pipe(
          map((mpsServerConfig: any) =>
            JSON.parse(stripJsonComments(mpsServerConfig)),
          ),
          concatMap(projectConfig => {
            let epochUrl = state.config.mpsServer.epochsUrl;
            const actions: Action[] = [];
            Object.keys(projectConfig).forEach(key => {
              if (key === 'epochsUrl') {
                epochUrl = projectConfig[key];
                actions.push(
                  ConfigActions.updateMpsServerSettings({
                    update: {
                      [key]: projectConfig[key],
                    },
                  }),
                );
              } else if (
                key === 'excludeActivityTypes' ||
                key === 'ignoreShareableLinkTimes' ||
                key === 'itarMessage' ||
                key === 'shareableLinkStatesUrl'
              ) {
                actions.push(
                  ConfigActions.updateRavenSettings({
                    update: {
                      [key]: projectConfig[key],
                    },
                  }),
                );
              } else if (
                key === 'showTimeCursor' &&
                projectConfig['showTimeCursor']
              ) {
                actions.push(TimeCursorActions.showTimeCursor());
              } else {
                actions.push(
                  ConfigActions.updateDefaultBandSettings({
                    update: {
                      [key]: projectConfig[key],
                    },
                  }),
                );
              }
            });
            actions.push(ConfigActions.fetchProjectConfigSuccess());
            if (epochUrl) {
              actions.push(
                EpochsActions.fetchEpochs({
                  replaceAction: 'AppendAndReplace',
                  url: `${state.config.app.baseUrl}/${epochUrl}`,
                }),
              );
            }
            return actions;
          }),
        ),
      ),
      catchError(() => {
        return of(ConfigActions.fetchProjectConfigSuccess());
      }),
    ),
  );

  fetchProjectConfigSuccess = createEffect(() =>
    this.actions.pipe(
      ofType(ConfigActions.fetchProjectConfigSuccess),
      withLatestFrom(this.store),
      map(([, state]) => ({ state })),
      mergeMap(({ state }) => {
        const layout = state.raven.sourceExplorer.layout;
        const shareableName = state.raven.sourceExplorer.shareableName;
        const statePath = state.raven.sourceExplorer.statePath;
        const layoutPath = state.raven.sourceExplorer.layoutPath;
        const sourcePath = state.raven.sourceExplorer.sourcePath;
        if (shareableName) {
          return [
            ...this.loadShareableLink(
              state.config,
              state.raven.layout,
              shareableName,
            ),
          ];
        } else {
          // Otherwise use other query parameters to load an app layout and/or state.
          return [
            ...this.loadLayout(state.raven.layout, layout),
            ...this.loadState(state.config, statePath, layoutPath, sourcePath),
          ];
        }
      }),
    ),
  );

  /**
   * Load an app layout mode which shows/hides panels in the main Raven2 UI.
   */
  loadLayout(layoutState: LayoutState, layout: string): Action[] {
    const actions: Action[] = [];

    if (layout === 'minimal') {
      actions.push(
        LayoutActions.setMode({
          mode: 'minimal',
          showDetailsPanel: true,
          showLeftPanel: false,
          showRightPanel: false,
          showSouthBandsPanel: true,
        }),
      );
    } else if (layout === 'default') {
      actions.push(
        LayoutActions.setMode({
          mode: 'default',
          showDetailsPanel: true,
          showLeftPanel: true,
          showRightPanel: false,
          showSouthBandsPanel: true,
        }),
      );
    } else {
      actions.push(
        LayoutActions.setMode({
          mode: 'custom',
          showDetailsPanel: layoutState.showDetailsPanel,
          showLeftPanel: layoutState.showLeftPanel,
          showRightPanel: layoutState.showRightPanel,
          showSouthBandsPanel: layoutState.showSouthBandsPanel,
        }),
      );
    }

    return actions;
  }

  /**
   * Returns a stream of actions that loads a sharable link.
   * To load a shareable link we set the layout to `minimal` mode (i.e. no source-explorer),
   * and apply the state using the `statePath`, which is composed of the `shareableLinkStatesUrl` from ravenConfig and
   * the user given `shareableName`.
   */
  loadShareableLink(
    configState: ConfigState,
    layoutState: LayoutState,
    shareableName: string,
  ): Action[] {
    const statePath = `/${configState.raven.shareableLinkStatesUrl}/${shareableName}`;

    return [
      ...this.loadState(configState, statePath, '', ''),
      ...this.loadLayout(layoutState, 'minimal'),
    ];
  }

  /**
   * Load a state (via ApplyState) if a state path exists.
   * Note the state path is just a source id, we call it state in the URL since it's more clear to the user what it is.
   */
  loadState(
    configState: ConfigState,
    statePath: string,
    layoutPath: string,
    sourcePath: string,
  ): Action[] {
    if (statePath) {
      return [
        SourceExplorerActions.applyState({
          sourceId: statePath,
          sourceUrl: `${configState.app.baseUrl}/${configState.mpsServer.apiUrl}${statePath}`,
        }),
      ];
    } else if (layoutPath && sourcePath) {
      return [
        SourceExplorerActions.updateSourceExplorer({
          update: {
            currentStateId: layoutPath,
            fetchPending: true,
          },
        }),
        SourceExplorerActions.applyLayoutToSources({
          layoutSourceId: layoutPath,
          layoutSourceUrl: `${configState.app.baseUrl}/${configState.mpsServer.apiUrl}${layoutPath}`,
          sourcePaths: sourcePath.split(','),
        }),
      ];
    } else if (layoutPath) {
      return [
        TimelineActions.updateTimeline({
          update: {
            currentStateId: layoutPath,
          },
        }),
      ];
    }
    return [];
  }
}
