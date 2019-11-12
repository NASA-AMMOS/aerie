/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import {
  RouterNavigationAction,
  SerializedRouterStateSnapshot,
} from '@ngrx/router-store';
import { Action, Store } from '@ngrx/store';
import { map, mergeMap, withLatestFrom } from 'rxjs/operators';
import { ConfigActions, SourceExplorerActions } from '../actions';
import { RavenAppState } from '../raven-store';

@Injectable()
export class RouterEffects {
  constructor(private actions: Actions, private store: Store<RavenAppState>) {}

  routerNavigation = createEffect(() =>
    this.actions.pipe(
      ofType<RouterNavigationAction<SerializedRouterStateSnapshot>>(
        '@ngrx/router-store/navigation',
      ),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      mergeMap(({ state, action }) => {
        const { queryParams } = action.payload.routerState.root;

        const layoutPath = queryParams.layout;
        const shareableName = queryParams.s;
        const statePath = queryParams.state;
        const sourcePath = queryParams.source;

        const actions: Action[] = [];

        // Get project config.
        actions.push(
          ConfigActions.fetchProjectConfig({
            url: `${state.config.app.baseUrl}/${state.config.mpsServer.ravenConfigUrl}`,
          }),
        );

        if (shareableName) {
          // If there is an `s` query parameter then use it to load a shareable link.
          actions.push(
            SourceExplorerActions.updateSourceExplorer({
              update: { shareableName },
            }),
          );
        } else {
          // Otherwise use other query parameters to load an app layout and/or state.
          actions.push(
            SourceExplorerActions.updateSourceExplorer({
              update: {
                layoutPath,
                sourcePath,
                statePath,
              },
            }),
          );
        }

        return actions;
      }),
    ),
  );
}
