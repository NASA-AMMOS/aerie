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
import { Observable } from 'rxjs';
import { map, mergeMap, withLatestFrom } from 'rxjs/operators';
import { RavenAppState } from '../raven-store';

import * as configActions from '../../shared/actions/config.actions';
import * as sourceExplorerActions from '../actions/source-explorer.actions';

@Injectable()
export class RouterEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<RavenAppState>,
  ) {}

  @Effect()
  routerNavigation$: Observable<Action> = this.actions$.pipe(
    ofType<RouterNavigationAction<ActivatedRouteSnapshot>>('ROUTER_NAVIGATION'),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    mergeMap(({ state, action }) => {
      const { queryParams } = action.payload.routerState.root;

      const layout = queryParams.layout;
      const shareableName = queryParams.s;
      const statePath = queryParams.state;

      const actions: Action[] = [];
      // Get project config.
      actions.push(
        new configActions.FetchProjectConfig(
          `${state.config.app.baseUrl}/${
            state.config.mpsServer.ravenConfigUrl
          }`,
        ),
      );
      if (shareableName) {
        // If there is an `s` query parameter then use it to load a shareable link.
        actions.push(
          new sourceExplorerActions.UpdateSourceExplorer({ shareableName }),
        );
      } else {
        // Otherwise use other query parameters to load an app layout and/or state.
        actions.push(
          new sourceExplorerActions.UpdateSourceExplorer({ layout }),
        );
        actions.push(
          new sourceExplorerActions.UpdateSourceExplorer({ statePath }),
        );
      }
      return actions;
    }),
  );
}
