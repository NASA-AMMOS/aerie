/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';

import { Actions, Effect, ofType } from '@ngrx/effects';
import { RouterNavigationAction } from '@ngrx/router-store';
import { Action, Store } from '@ngrx/store';

import {
  Observable,
} from 'rxjs';

import {
  map,
  mergeMap,
  withLatestFrom,
} from 'rxjs/operators';

import { AppState } from './../store';

import { ConfigState } from './../reducers/config';
import { LayoutState } from './../reducers/layout';

import * as layoutActions from './../actions/layout';
import * as sourceExplorerActions from './../actions/source-explorer';

@Injectable()
export class RouterEffects {
  @Effect()
  routerNavigation$: Observable<Action> = this.actions$.pipe(
    ofType<RouterNavigationAction<ActivatedRouteSnapshot>>('ROUTER_NAVIGATION'),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    mergeMap(({ state, action }) => {
      const { queryParams } = action.payload.routerState;

      const layout = queryParams.layout;
      const shareableName = queryParams.s;
      const statePath = queryParams.state;

      if (shareableName) {
        // If there is an `s` query parameter then use it to load a shareable link.
        return [
          ...this.loadShareableLink(state.config, state.layout, shareableName),
        ];
      } else {
        // Otherwise use other query parameters to load an app layout and/or state.
        return [
          ...this.loadLayout(state.layout, layout),
          ...this.loadState(state.config, statePath),
        ];
      }
    }),
  );

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
  ) {}

  /**
   * Load an app layout mode which shows/hides panels in the main Raven2 UI.
   */
  loadLayout(layoutState: LayoutState, layout: string): Action[] {
    const actions: Action[] = [];

    if (layout === 'minimal') {
      actions.push(new layoutActions.SetMode('minimal', true, false, false, true));
    } else if (layout === 'default') {
      actions.push(new layoutActions.SetMode('default', true, true, false, true));
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
  loadShareableLink(configState: ConfigState, layoutState: LayoutState, shareableName: string): Action[] {
    const statePath = `/${configState.shareableLinkStatesUrl}/${shareableName}`;

    return [
      ...this.loadLayout(layoutState, 'minimal'),
      ...this.loadState(configState, statePath),
    ];
  }

  /**
   * Load a state (via ApplyState) if a state path exists.
   * Note the state path is just a source id, we call it state in the URL since it's more clear to the user what it is.
   */
  loadState(configState: ConfigState, statePath: string): Action[] {
    if (statePath) {
      return [
        new sourceExplorerActions.ApplyState(`${configState.baseUrl}/${configState.baseSourcesUrl}${statePath}`, statePath),
      ];
    }
    return [];
  }
}
