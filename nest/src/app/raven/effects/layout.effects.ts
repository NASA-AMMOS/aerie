/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { Observable, of } from 'rxjs';
import { concatMap, map, switchMap, withLatestFrom } from 'rxjs/operators';
import {
  LayoutActionTypes,
  Resize,
  ToggleApplyLayoutDrawerEvent,
  ToggleLeftPanel,
  ToggleRightPanel,
  ToggleSituationalAwarenessDrawer,
} from '../actions/layout.actions';
import * as layoutActions from '../actions/layout.actions';
import * as situationalAwarenessActions from '../actions/situational-awareness.actions';
import * as timelineActions from '../actions/timeline.actions';
import { RavenAppState } from '../raven-store';
import { importState } from '../util';

@Injectable()
export class LayoutEffects {
  constructor(
    private actions$: Actions,
    private http: HttpClient,
    private store$: Store<RavenAppState>,
  ) {}

  /**
   * Effect for triggering a band resize after any panels are resized.
   */
  @Effect()
  panelsResized$: Observable<Action> = this.actions$.pipe(
    ofType<ToggleLeftPanel | ToggleRightPanel>(
      LayoutActionTypes.ToggleLeftPanel,
      LayoutActionTypes.ToggleRightPanel,
    ),
    map(() => new Resize()),
  );

  /**
   * Effect for Resize.
   */
  @Effect({ dispatch: false })
  resize$: Observable<Action> = this.actions$.pipe(
    ofType<Resize>(LayoutActionTypes.Resize),
    switchMap(() => {
      setTimeout(() => dispatchEvent(new Event('resize')));
      return [];
    }),
  );

  /**
   * Effect for ToggleApplyLayoutDrawerEvent.
   * Fetches the state associated with the current state id so it's available in the
   * apply layout drawer when it opens.
   * If there is no current state id it should set the current state to null.
   */
  @Effect()
  toggleApplyLayoutDrawerEvent$: Observable<Action> = this.actions$.pipe(
    ofType<ToggleApplyLayoutDrawerEvent>(
      LayoutActionTypes.ToggleApplyLayoutDrawerEvent,
    ),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state }) => {
      if (action.opened && state.raven.timeline.currentStateId !== '') {
        return this.http
          .get(
            `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}${
              state.raven.timeline.currentStateId
            }`,
          )
          .pipe(
            map(res => importState(res[0])),
            switchMap(currentState => [
              new timelineActions.UpdateTimeline({
                currentState,
              }),
              new layoutActions.ToggleApplyLayoutDrawer(action.opened),
              new layoutActions.UpdateLayout({ fetchPending: false }),
            ]),
          );
      }

      return [
        new timelineActions.UpdateTimeline({
          currentState: null,
          fetchPending: false,
        }),
      ];
    }),
  );

  /**
   * Effect for ToggleRightPanel.
   */
  @Effect()
  toggleRightPanel$: Observable<Action> = this.actions$.pipe(
    ofType<ToggleRightPanel>(LayoutActionTypes.ToggleRightPanel),
    withLatestFrom(this.store$),
    map(([, state]) => state.raven),
    map(raven => {
      if (raven.layout.showRightPanel && raven.layout.showLeftPanel) {
        return new layoutActions.UpdateLayout({
          timelinePanelSize: raven.layout.showLeftPanel ? 60 : 75,
        });
      } else {
        return new layoutActions.UpdateLayout({
          timelinePanelSize: raven.layout.showLeftPanel ? 75 : 100,
        });
      }
    }),
  );

  /**
   * Effect for ToggleSituationalAwarenessDrawer.
   */
  @Effect()
  toggleSituationalAwarenessDrawer$: Observable<Action> = this.actions$.pipe(
    ofType<ToggleSituationalAwarenessDrawer>(
      LayoutActionTypes.ToggleSituationalAwarenessDrawer,
    ),
    withLatestFrom(this.store$),
    map(([, state]) => state),
    concatMap(state => {
      if (state.raven.layout.showSituationalAwarenessDrawer) {
        return of(
          new situationalAwarenessActions.FetchPefEntries(
            `${
              state.config.app.baseUrl
            }/mpsserver/api/v2/situational_awareness?`,
          ),
        );
      } else {
        return [];
      }
    }),
  );
}
