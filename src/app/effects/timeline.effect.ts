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
  concatMap,
  map,
  withLatestFrom,
} from 'rxjs/operators';

import { AppState } from './../../app/store';

import * as layoutActions from './../actions/layout';
import { TimelineActionTypes } from './../actions/timeline';

import { SelectPoint } from '../actions/timeline';

@Injectable()
export class TimelineEffects {
  @Effect()
  selectPoint$: Observable<Action> = this.actions$.pipe(
    ofType<SelectPoint>(TimelineActionTypes.SelectPoint),
    withLatestFrom(this.store$),
    map(([action, state]) => state),
    concatMap(state => {
      const actions: Action[] = [];

      if (state.timeline.selectedPoint) {
        actions.push(new layoutActions.UpdateLayout({
          rightDrawerSelectedTabIndex: 2,
        }));

        if (!state.layout.showRightDrawer) {
          actions.push(new layoutActions.ToggleRightDrawer());
        }
      }

      return actions;
    }),
  );

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
  ) {}
}
