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
  CloseDataPointDrawer,
  OpenDataPointDrawer,
  ToggleDataPointDrawer,
} from './../actions/layout';

import * as layoutActions from './../actions/layout';

import { LayoutActionTypes } from './../actions/layout';

@Injectable()
export class LayoutEffects {
  @Effect()
  closeDataPointDrawer$: Observable<Action> = this.actions$.pipe(
    ofType<CloseDataPointDrawer>(LayoutActionTypes.CloseDataPointDrawer),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    map(({ action }) => new layoutActions.SetTimelinePanelSize(75),
    ),
  );

  @Effect()
  openDataPointDrawer$: Observable<Action> = this.actions$.pipe(
    ofType<OpenDataPointDrawer>(LayoutActionTypes.OpenDataPointDrawer),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    map(({ action }) => new layoutActions.SetTimelinePanelSize(60),
    ),
  );

  @Effect()
  toggleDataPointDrawer$: Observable<Action> = this.actions$.pipe(
    ofType<ToggleDataPointDrawer>(LayoutActionTypes.ToggleDataPointDrawer),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    map(({ action, state }) => state.layout.showDataPointDrawer ? new layoutActions.CloseDataPointDrawer() : new layoutActions.OpenDataPointDrawer(),
    ),
  );

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
  ) { }
}
