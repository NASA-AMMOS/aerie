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

import { Observable } from 'rxjs/Observable';

import {
  map,
  mergeMap,
  withLatestFrom,
} from 'rxjs/operators';

import { AppState } from './../../app/store';

import * as layoutActions from './../actions/layout';

@Injectable()
export class RouterEffects {
  @Effect()
  routerNavigation$: Observable<Action> = this.actions$.pipe(
    ofType<RouterNavigationAction<ActivatedRouteSnapshot>>('ROUTER_NAVIGATION'),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    mergeMap(({ state, action }) => {
      const actions: Action[] = [];
      const { layout } = action.payload.routerState.queryParams;

      if (layout === 'minimal') {
        actions.push(new layoutActions.SetMode('minimal', false, false, true));
      } else if (layout === 'default') {
        actions.push(new layoutActions.SetMode('default', true, true, true));
      } else {
        actions.push(new layoutActions.SetMode('custom', state.layout.showDetailsDrawer, state.layout.showLeftDrawer, state.layout.showSouthBandsDrawer));
      }

      return actions;
    }),
  );

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
  ) {}
}
