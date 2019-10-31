/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { interval, Observable, of } from 'rxjs';
import {
  concatMap,
  exhaustMap,
  map,
  switchMap,
  takeUntil,
  withLatestFrom,
} from 'rxjs/operators';
import { TimeCursorActions, TimelineActions } from '../actions';
import { RavenAppState } from '../raven-store';

@Injectable()
export class TimeCursorEffects {
  constructor(private actions: Actions, private store: Store<RavenAppState>) {}

  /**
   * @note We return an empty action here {}. This is so `cursorInterval$` `takeUntil` fires.
   * If we return an empty observable here (e.g. [] or of()), `takeUntil` will never fire.
   */
  hideTimeCursor = createEffect(
    () =>
      this.actions.pipe(
        ofType(TimeCursorActions.hideTimeCursor),
        concatMap(() => of({} as Action)),
      ),
    { dispatch: false },
  );

  showTimeCursor = createEffect(() =>
    this.actions.pipe(
      ofType(TimeCursorActions.showTimeCursor),
      withLatestFrom(this.store),
      map(([, state]) => state.raven),
      exhaustMap(({ timeCursor: { clockUpdateIntervalInSecs } }) =>
        this.cursorInterval$(clockUpdateIntervalInSecs),
      ),
    ),
  );

  /**
   * Helper. Returns a cursor time interval Observable that fires every `clockUpdateIntervalInSecs` seconds.
   * Update the time cursor at each tick of the interval until the `hideTimeCursor` Observable fires.
   */
  cursorInterval$(clockUpdateIntervalInSecs: number): Observable<Action> {
    return interval(clockUpdateIntervalInSecs * 1000).pipe(
      withLatestFrom(this.store),
      map(([, state]) => state.raven),
      switchMap(({ timeCursor, timeline: { viewTimeRange } }) => {
        const actions = [];

        const { autoPage, clockRate, cursorTime, followTimeCursor } = timeCursor;
        const delta = clockRate * clockUpdateIntervalInSecs;
        const newCursorTime = cursorTime
          ? cursorTime + delta
          : null;

        actions.push(
          TimeCursorActions.updateTimeCursorSettings({
            update: { cursorTime: newCursorTime },
          }),
        );

        if (followTimeCursor) {
          console.log('adjust viewTimeRange');
          actions.push(TimelineActions.updateViewTimeRange({ viewTimeRange: {start: viewTimeRange.start + delta, end: viewTimeRange.end + delta }}));
        }
        // If we are auto-paging and our time cursor goes outside the view window, then pan the view window right.
        else if (autoPage && newCursorTime && newCursorTime > viewTimeRange.end) {
          actions.push(TimelineActions.panRightViewTimeRange());
        }

        return actions;
      }),
      takeUntil(this.hideTimeCursor),
    );
  }
}
