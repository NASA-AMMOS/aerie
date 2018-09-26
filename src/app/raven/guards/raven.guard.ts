/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable, of } from 'rxjs';
import { map, withLatestFrom } from 'rxjs/operators';
import { RavenAppState } from '../raven-store';

import {
  ActivatedRouteSnapshot,
  CanActivate,
  RouterStateSnapshot,
} from '@angular/router';

import { catchError, filter, switchMap, take, tap } from 'rxjs/operators';

import * as configActions from '../../shared/actions/config.actions';
import * as epochsActions from '../actions/epochs.actions';
import * as sourceExplorerActions from '../actions/source-explorer.actions';

@Injectable()
export class RavenGuard implements CanActivate {
  constructor(private store$: Store<RavenAppState>) {}

  /**
   * Returns a true boolean Observable if we can activate the route.
   */
  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
  ): Observable<boolean> | Promise<boolean> | boolean {
    return this.initialSourcesLoaded().pipe(
      switchMap(() => of(true)),
      catchError(() => of(false)),
    );
  }

  /**
   * Returns a true boolean Observable only if initialSourcesLoaded is true.
   * If initialSourcesLoaded is false then the stream is filtered.
   * Note that whenever the `fromSourceExplorer.getInitialSourcesLoaded` selector changes this stream is re-triggered.
   */
  initialSourcesLoaded(): Observable<boolean> {
    return this.store$.pipe(
      withLatestFrom(this.store$),
      map(([, state]) => state),
      tap(state => {
        if (!state.raven.sourceExplorer.initialSourcesLoaded) {
          this.store$.dispatch(
            new configActions.FetchProjectConfig(
              `${state.config.app.baseUrl}/${
                state.config.mpsServer.ravenConfigUrl
              }`,
            ),
          );
          this.store$.dispatch(new sourceExplorerActions.FetchInitialSources());
          this.store$.dispatch(
            new epochsActions.FetchEpochs(
              `${state.config.app.baseUrl}/${state.config.mpsServer.epochsUrl}`,
            ),
          );
        }
      }),
      filter((initialSourcesLoaded: boolean) => initialSourcesLoaded),
      take(1),
    );
  }
}
