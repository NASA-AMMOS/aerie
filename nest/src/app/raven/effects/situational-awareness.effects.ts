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
import { concat, Observable, of } from 'rxjs';
import {
  catchError,
  concatMap,
  map,
  switchMap,
  withLatestFrom,
} from 'rxjs/operators';
import { getInitialPageStartEndTime } from '../../shared/util';
import {
  FetchPefEntries,
  SituationalAwarenessActionTypes,
} from '../actions/situational-awareness.actions';
import * as situationalAwarenessActions from '../actions/situational-awareness.actions';
import * as timelineActions from '../actions/timeline.actions';
import {
  MpsServerGraphData,
  MpsServerSituationalAwarenessPefEntry,
  RavenDefaultBandSettings,
} from '../models';
import { RavenAppState } from '../raven-store';
import { toCompositeBand, toRavenBandData, toRavenPefEntries } from '../util';

@Injectable()
export class SituationalAwarenessEffects {
  constructor(
    private http: HttpClient,
    private actions$: Actions,
    private store$: Store<RavenAppState>,
  ) {}

  @Effect()
  changeSituationalAwareness$: Observable<Action> = this.actions$.pipe(
    ofType<situationalAwarenessActions.ChangeSituationalAwareness>(
      SituationalAwarenessActionTypes.ChangeSituationalAwareness,
    ),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ state, action }) =>
      concat(
        of(
          new situationalAwarenessActions.UpdateSituationalAwarenessSettings({
            situationalAware: action.situAware,
          }),
        ),
        action.situAware
          ? this.getPefEntriesAsState(
              action.url,
              state.config.raven.defaultBandSettings,
            )
          : of(
              new timelineActions.RemoveBandsOrPointsForSource('situAwarePef'),
            ),
        action.situAware
          ? of(
              new timelineActions.UpdateViewTimeRange(
                getInitialPageStartEndTime(state.raven.situationalAwareness),
              ),
            )
          : [],
        of(
          new situationalAwarenessActions.UpdateSituationalAwarenessSettings({
            fetchPending: false,
          }),
        ),
      ),
    ),
    catchError((e: Error) => {
      return of(
        new situationalAwarenessActions.UpdateSituationalAwarenessSettings({
          fetchPending: false,
          pefEntries: [],
        }),
      );
    }),
  );

  @Effect()
  fetchPefEntries$: Observable<Action> = this.actions$.pipe(
    ofType<FetchPefEntries>(SituationalAwarenessActionTypes.FetchPefEntries),
    concatMap(action => this.getPefEntries(action.url)),
    catchError((e: Error) => {
      console.error(
        'SituationalAwarenessEffects - fetchSituationalAwareness$: ',
        e,
      );
      return of(
        new situationalAwarenessActions.UpdateSituationalAwarenessSettings({
          pefEntries: [],
        }),
      );
    }),
  );

  /**
   * Helper. Fetches Pef entries for situational awareness.
   */
  getPefEntries(url: string) {
    return this.http.get(url).pipe(
      map((mpsServerPefEntries: MpsServerSituationalAwarenessPefEntry[]) =>
        toRavenPefEntries(mpsServerPefEntries),
      ),
      switchMap(pefEntries =>
        of(
          new situationalAwarenessActions.UpdateSituationalAwarenessSettings({
            pefEntries,
          }),
        ),
      ),
    );
  }

  /**
   * Helper. Fetches Pef entries for situational awareness as a state timeline.
   */
  getPefEntriesAsState(
    url: string,
    defaultBandSettings: RavenDefaultBandSettings,
  ) {
    return this.http.get(url + 'asState=true&format=TMS').pipe(
      map((graphData: MpsServerGraphData) =>
        toRavenBandData(
          'situAwarePef',
          'situAwarePef',
          graphData,
          defaultBandSettings,
          null,
          {},
        ),
      ),
      switchMap(subBands =>
        of(
          new timelineActions.AddBand(
            'situAwarePef',
            toCompositeBand(subBands[0]),
          ),
        ),
      ),
    );
  }
}
