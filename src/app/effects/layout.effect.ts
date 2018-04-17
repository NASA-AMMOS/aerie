/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';

import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';

import { Observable } from 'rxjs/Observable';

import {
  map,
  withLatestFrom,
} from 'rxjs/operators';

import { AppState } from './../../app/store';

import {
  LayoutActionTypes,
  ToggleRightDrawer,
} from './../actions/layout';

import * as layoutActions from './../actions/layout';

@Injectable()
export class LayoutEffects {
  @Effect()
  toggleRightDrawer$: Observable<Action> = this.actions$.pipe(
    ofType<ToggleRightDrawer>(LayoutActionTypes.ToggleRightDrawer),
    withLatestFrom(this.store$),
    map(([action, state]) => state.layout.showRightDrawer),
    map((showRightDrawer: boolean) => {
      if (showRightDrawer) {
        return new layoutActions.UpdateLayout({ timelinePanelSize: 50 });
      } else {
        return new layoutActions.UpdateLayout({ timelinePanelSize: 75 });
      }
    }),
  );

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
  ) {}
}
