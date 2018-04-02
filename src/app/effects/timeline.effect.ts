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
import * as timelineActions from './../actions/timeline';
import { TimelineActionTypes } from './../actions/timeline';

import { SelectDataPointEvent } from '../actions/timeline';

import { getRavenPoint } from './../shared/util/points';
@Injectable()
export class TimelineEffects {
  @Effect()
  selectDataPointEvent$: Observable<Action> = this.actions$.pipe(
    ofType<SelectDataPointEvent>(TimelineActionTypes.SelectDataPointEvent),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) => {
      const actions: Action[] = [];
      const ravenPoint = getRavenPoint(state.timeline.bands, action.ctlData);
      if (ravenPoint !== null) {
        actions.push(new timelineActions.SelectDataPoint(ravenPoint));
        actions.push(new layoutActions.OpenDataPointDrawer());
      }

      return actions;
    }),
  );

  constructor(
    private actions$: Actions,
    private store$: Store<AppState>,
  ) { }
}
