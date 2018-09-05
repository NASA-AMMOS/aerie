/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { RavenAppState } from '../raven-store';

import {
  ActivatedRouteSnapshot,
  CanActivate,
  RouterStateSnapshot,
} from '@angular/router';

import { Observable, of, Subject } from 'rxjs';

import {
  catchError,
  filter,
  switchMap,
  take,
  takeUntil,
  tap,
} from 'rxjs/operators';

import * as fromConfig from '../../shared/reducers/config.reducer';
import * as epochsActions from '../actions/epochs.actions';
import * as sourceExplorerActions from '../actions/source-explorer.actions';
import * as fromSourceExplorer from '../reducers/source-explorer.reducer';

@Injectable()
export class RavenGuard implements CanActivate {
  // Config state.
  baseUrl: string;
  epochsUrl: string;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(private store$: Store<RavenAppState>) {
    // Config state.
    this.store$
      .select(fromConfig.getUrls)
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(({ baseUrl, epochsUrl }) => {
        this.epochsUrl = epochsUrl;
        this.baseUrl = baseUrl;
      });
  }

  /**
   * Returns a true boolean Observable if we can activate the route.
   */
  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    return this.initialSourcesLoaded().pipe(
      switchMap(() => of(true)),
      catchError(() => of(false))
    );
  }

  /**
   * Returns a true boolean Observable only if initialSourcesLoaded is true.
   * If initialSourcesLoaded is false then the stream is filtered.
   * Note that whenever the `fromSourceExplorer.getInitialSourcesLoaded` selector changes this stream is re-triggered.
   */
  initialSourcesLoaded(): Observable<boolean> {
    return this.store$.pipe(
      select(fromSourceExplorer.getInitialSourcesLoaded),
      tap((initialSourcesLoaded: boolean) => {
        if (!initialSourcesLoaded) {
          this.store$.dispatch(new sourceExplorerActions.FetchInitialSources());
          this.store$.dispatch(
            new epochsActions.FetchEpochs(`${this.baseUrl}/${this.epochsUrl}`)
          );
        }
      }),
      filter((initialSourcesLoaded: boolean) => initialSourcesLoaded),
      take(1)
    );
  }
}
