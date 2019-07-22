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
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { of } from 'rxjs';
import { concatMap, map, switchMap, withLatestFrom } from 'rxjs/operators';
import {
  LayoutActions,
  SituationalAwarenessActions,
  TimelineActions,
} from '../actions';
import { RavenAppState } from '../raven-store';
import { importState } from '../util';

@Injectable()
export class LayoutEffects {
  constructor(
    private actions: Actions,
    private http: HttpClient,
    private store: Store<RavenAppState>,
  ) {}

  panelsResized = createEffect(() =>
    this.actions.pipe(
      ofType(LayoutActions.toggleLeftPanel, LayoutActions.toggleRightPanel),
      map(() => LayoutActions.resize()),
    ),
  );

  resize = createEffect(
    () =>
      this.actions.pipe(
        ofType(LayoutActions.resize),
        switchMap(() => {
          setTimeout(() => dispatchEvent(new Event('resize')));
          return [];
        }),
      ),
    { dispatch: false },
  );

  /**
   * Effect for ToggleApplyLayoutDrawerEvent.
   * Fetches the state associated with the current state id so it's available in the
   * apply layout drawer when it opens.
   * If there is no current state id it should set the current state to null.
   */
  toggleApplyLayoutDrawerEvent = createEffect(() =>
    this.actions.pipe(
      ofType(LayoutActions.toggleApplyLayoutDrawerEvent),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      switchMap(({ action, state }) => {
        if (action.opened && state.raven.timeline.currentStateId !== '') {
          return this.http
            .get(
              `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}${state.raven.timeline.currentStateId}`,
            )
            .pipe(
              map(res => importState(res[0])),
              switchMap(currentState => [
                TimelineActions.updateTimeline({
                  update: {
                    currentState,
                  },
                }),
                LayoutActions.toggleApplyLayoutDrawer({
                  opened: action.opened,
                }),
                LayoutActions.updateLayout({ update: { fetchPending: false } }),
              ]),
            );
        }

        return [
          TimelineActions.updateTimeline({
            update: {
              currentState: null,
              fetchPending: false,
            },
          }),
        ];
      }),
    ),
  );

  toggleRightPanel = createEffect(() =>
    this.actions.pipe(
      ofType(LayoutActions.toggleRightPanel),
      withLatestFrom(this.store),
      map(([, state]) => state.raven),
      map(raven => {
        if (raven.layout.showRightPanel && raven.layout.showLeftPanel) {
          return LayoutActions.updateLayout({
            update: {
              timelinePanelSize: raven.layout.showLeftPanel ? 60 : 75,
            },
          });
        } else {
          return LayoutActions.updateLayout({
            update: {
              timelinePanelSize: raven.layout.showLeftPanel ? 75 : 100,
            },
          });
        }
      }),
    ),
  );

  toggleSituationalAwarenessDrawer = createEffect(() =>
    this.actions.pipe(
      ofType(LayoutActions.toggleSituationalAwarenessDrawer),
      withLatestFrom(this.store),
      map(([, state]) => state),
      concatMap(state => {
        if (state.raven.layout.showSituationalAwarenessDrawer) {
          return of(
            SituationalAwarenessActions.fetchPefEntries({
              url: `${state.config.app.baseUrl}/mpsserver/api/v2/situational_awareness?`,
            }),
          );
        } else {
          return [];
        }
      }),
    ),
  );
}
