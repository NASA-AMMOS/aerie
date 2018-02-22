/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';

import { Actions, Effect } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { RouterNavigationAction, } from '@ngrx/router-store';
import { Observable } from 'rxjs/Observable';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/withLatestFrom';

import { AppState } from './../../app/store';

import * as layoutActions from './../actions/layout';

@Injectable()
export class RouterEffects {
  @Effect()
  routerNavigation$: Observable<Action> = this.actions$
    .ofType<RouterNavigationAction>('ROUTER_NAVIGATION')
    .withLatestFrom(this.store$)
    .map(([action, state]) => ({ action, state }))
    .map(({ state, action }) => {
      const { layout } = (action.payload.routerState as any).queryParams; // TODO: See if we can remove 'any' here.

      if (layout === 'minimal') {
        return new layoutActions.SetMode('minimal', false, false, false);
      } else if (layout === 'default') {
        return new layoutActions.SetMode('default', true, true, true);
      } else {
        return new layoutActions.SetMode('custom', state.layout.showDetailsDrawer, state.layout.showLeftDrawer, state.layout.showSouthBandsDrawer);
      }
    });

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>
  ) {}
}
