/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';

import {
  ActivatedRouteSnapshot,
  CanActivate,
  RouterStateSnapshot,
} from '@angular/router';

import { Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { of } from 'rxjs/observable/of';

import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/take';

import { AppState } from './../../app/store';

import * as sourceExplorerActions from './../actions/source-explorer';
import * as fromSourceExplorer from './../reducers/source-explorer';

@Injectable()
export class TimelineGuard implements CanActivate {
  constructor(private store$: Store<AppState>) {}

  /**
   * Returns a true boolean Observable if we can activate the route.
   */
  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    return this.initialSourcesLoaded()
      .switchMap(() => of(true))
      .catch(() => of(false));
  }

  /**
   * Returns a true boolean Observable only if initialSourcesLoaded is true.
   * If initialSourcesLoaded is false then the stream is filtered.
   * Note that whenever the `fromSourceExplorer.getInitialSourcesLoaded` selector changes this stream is re-triggered.
   */
  initialSourcesLoaded(): Observable<boolean> {
    return this.store$
      .select(fromSourceExplorer.getInitialSourcesLoaded)
      .do((initialSourcesLoaded: boolean) => {
        if (!initialSourcesLoaded) { this.store$.dispatch(new sourceExplorerActions.FetchInitialSources()); }
      })
      .filter((initialSourcesLoaded: boolean) => initialSourcesLoaded)
      .take(1);
  }
}
