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
import { Observable } from 'rxjs';
import {
  catchError,
  concatMap,
  map,
  mergeMap,
  withLatestFrom,
} from 'rxjs/operators';
import * as stripJsonComments from 'strip-json-comments';
import { ConfigState } from '../../../config';
import * as epochsActions from '../../raven/actions/epochs.actions';
import * as layoutActions from '../../raven/actions/layout.actions';
import * as sourceExplorerActions from '../../raven/actions/source-explorer.actions';
import * as timeCursorActions from '../../raven/actions/time-cursor.actions';
import { RavenAppState } from '../../raven/raven-store';
import { LayoutState } from '../../raven/reducers/layout.reducer';
import * as configActions from '../actions/config.actions';

import {
  ConfigActionTypes,
  FetchProjectConfig,
} from '../actions/config.actions';

@Injectable()
export class ConfigEffects {
  constructor(
    private http: HttpClient,
    private actions$: Actions,
    private store$: Store<RavenAppState>,
  ) {}

  @Effect()
  fetchProjectConfig$: Observable<Action> = this.actions$.pipe(
    ofType<FetchProjectConfig>(ConfigActionTypes.FetchProjectConfig),
    withLatestFrom(this.store$),
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
                new configActions.UpdateMpsServerSettings({
                  [key]: projectConfig[key],
                }),
              );
            } else if (
              key === 'excludeActivityTypes' ||
              key === 'ignoreShareableLinkTimes' ||
              key === 'itarMessage' ||
              key === 'shareableLinkStatesUrl'
            ) {
              actions.push(
                new configActions.UpdateRavenSettings({
                  [key]: projectConfig[key],
                }),
              );
            } else if (
              key === 'showTimeCursor' &&
              projectConfig['showTimeCursor']
            ) {
              actions.push(new timeCursorActions.ShowTimeCursor());
            } else {
              actions.push(
                new configActions.UpdateDefaultBandSettings({
                  [key]: projectConfig[key],
                }),
              );
            }
          });
          actions.push(new configActions.FetchProjectConfigSuccess());
          if (epochUrl) {
            actions.push(
              new epochsActions.FetchEpochs(
                `${state.config.app.baseUrl}/${epochUrl}`,
              ),
            );
          }
          return actions;
        }),
      ),
    ),
    catchError((e: Error) => {
      return [new configActions.FetchProjectConfigSuccess()];
    }),
  );

  @Effect()
  fetchProjectConfigSuccess$: Observable<Action> = this.actions$.pipe(
    ofType<FetchProjectConfig>(ConfigActionTypes.FetchProjectConfigSuccess),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ state })),
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
  );

  /**
   * Load an app layout mode which shows/hides panels in the main Raven2 UI.
   */
  loadLayout(layoutState: LayoutState, layout: string): Action[] {
    const actions: Action[] = [];

    if (layout === 'minimal') {
      actions.push(
        new layoutActions.SetMode('minimal', true, false, false, true),
      );
    } else if (layout === 'default') {
      actions.push(
        new layoutActions.SetMode('default', true, true, false, true),
      );
    } else {
      actions.push(
        new layoutActions.SetMode(
          'custom',
          layoutState.showDetailsPanel,
          layoutState.showLeftPanel,
          layoutState.showRightPanel,
          layoutState.showSouthBandsPanel,
        ),
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
    const statePath = `/${
      configState.raven.shareableLinkStatesUrl
    }/${shareableName}`;

    return [
      ...this.loadLayout(layoutState, 'minimal'),
      ...this.loadState(configState, statePath, '', ''),
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
        new sourceExplorerActions.ApplyState(
          `${configState.app.baseUrl}/${
            configState.mpsServer.apiUrl
          }${statePath}`,
          statePath,
        ),
      ];
    } else if (layoutPath && sourcePath) {
      // apply layout to sourcePath
      return [
        new sourceExplorerActions.UpdateSourceExplorer({
          currentStateId: layoutPath,
        }),
        new sourceExplorerActions.UpdateSourceExplorer({
          fetchPending: true,
        }),
        new sourceExplorerActions.ApplyLayoutToSources(
          `${configState.app.baseUrl}/${
            configState.mpsServer.apiUrl
          }${layoutPath}`,
          layoutPath,
          sourcePath.split(','),
        ),
      ];
    } else if (layoutPath) {
      return [
        new sourceExplorerActions.UpdateSourceExplorer({
          currentStateId: layoutPath,
        }),
      ];
    }
    return [];
  }
}
