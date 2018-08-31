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
import { Observable, of } from 'rxjs';
import { interval } from 'rxjs/observable/interval';
import { RavenTimeRange } from '../../shared/models';
import { RavenAppState } from '../raven-store';

import {
  concatMap,
  map,
  switchMap,
  takeUntil,
  withLatestFrom,
} from 'rxjs/operators';

import {
  HideTimeCursor,
  ShowTimeCursor,
  TimeCursorActionTypes,
} from '../actions/time-cursor';

import * as timeCursorActions from '../actions/time-cursor';
import * as timelineActions from '../actions/timeline';

@Injectable()
export class TimeCursorEffects {
  /**
   * Effect for HideTimeCursor.
   * Note how we return an empty action here {}. This is so `cursorInterval$` `takeUntil` fires.
   * If we return an empty observable here (e.g. [] or of()), `takeUntil` will never fire.
   */
  @Effect({ dispatch: false })
  hideTimeCursor$: Observable<Action> = this.actions$.pipe(
    ofType<HideTimeCursor>(TimeCursorActionTypes.HideTimeCursor),
    concatMap(() => of({} as Action))
  );

  /**
   * Effect for ShowTimeCursor.
   */
  @Effect()
  showTimeCursor$: Observable<Action> = this.actions$.pipe(
    ofType<ShowTimeCursor>(TimeCursorActionTypes.ShowTimeCursor),
    withLatestFrom(this.store$),
    map(([, state]) => state.raven),
    concatMap(({ timeCursor: { clockUpdateIntervalInSecs } }) =>
      this.cursorInterval$(clockUpdateIntervalInSecs)
    )
  );

  constructor(
    private actions$: Actions,
    private store$: Store<RavenAppState>
  ) {}

  /**
   * Helper. Returns a cursor time interval Observable that fires every `clockUpdateIntervalInSecs` seconds.
   * Update the time cursor at each tick of the interval until the `hideTimeCursor$` Observable fires.
   */
  cursorInterval$(clockUpdateIntervalInSecs: number): Observable<Action> {
    return interval(clockUpdateIntervalInSecs * 1000).pipe(
      withLatestFrom(this.store$),
      map(([, state]) => state.raven),
      switchMap(raven =>
        this.updateCursor(
          raven.timeCursor.cursorTime,
          raven.timeCursor.clockRate,
          raven.timeCursor.clockUpdateIntervalInSecs,
          raven.timeCursor.autoPage,
          raven.timeline.viewTimeRange
        )
      ),
      takeUntil(this.hideTimeCursor$)
    );
  }

  /**
   * Helper. Dispatches actions that updates the time cursor time.
   * Pans the timeline right if needed.
   */
  updateCursor(
    cursorTime: number | null,
    clockRate: number,
    clockUpdateIntervalInSecs: number,
    autoPage: boolean,
    viewTimeRange: RavenTimeRange
  ) {
    const actions = [];

    const newCursorTime = cursorTime
      ? cursorTime + clockRate * clockUpdateIntervalInSecs
      : null;
    actions.push(
      new timeCursorActions.UpdateTimeCursorSettings({
        cursorTime: newCursorTime,
      })
    );

    // If we are auto-paging and our time cursor goes outside the view window, then pan the view window right.
    if (
      autoPage &&
      cursorTime &&
      cursorTime > viewTimeRange.start &&
      cursorTime < viewTimeRange.end &&
      newCursorTime &&
      newCursorTime > viewTimeRange.end
    ) {
      actions.push(new timelineActions.PanRightViewTimeRange());
    }

    return actions;
  }
}
