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
      const actions: Action[] = [];
      const layout = action.payload.routerState.queryParams.layout;
      const statePath = action.payload.routerState.queryParams.state;

      if (layout === 'minimal') {
        actions.push(new layoutActions.SetMode('minimal', true, false, false, true));
      } else if (layout === 'default') {
        actions.push(new layoutActions.SetMode('default', true, true, false, true));
      } else {
        actions.push(
          new layoutActions.SetMode(
            'custom',
            state.layout.showDetailsPanel,
            state.layout.showLeftPanel,
            state.layout.showRightPanel,
            state.layout.showSouthBandsPanel,
          ),
        );
      }

      // Load a state if a state path exists (note the state path is just a source id, we call it state in the URL since it's more clear to the user what it is).
      if (statePath) {
        actions.push(new sourceExplorerActions.ApplyState(`${state.config.baseUrl}/${state.config.baseSourcesUrl}${statePath}`, statePath));
      }

      return actions;
    }),
  );

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
  ) {}
}
