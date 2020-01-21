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
import { concat, of } from 'rxjs';
import {
  catchError,
  concatMap,
  map,
  switchMap,
  withLatestFrom,
} from 'rxjs/operators';
import { SituationalAwarenessActions, TimelineActions } from '../actions';
import {
  MpsServerGraphData,
  MpsServerSituationalAwarenessPefEntry,
  RavenDefaultBandSettings,
} from '../models';
import { RavenAppState } from '../raven-store';
import {
  getInitialPageStartEndTime,
  toCompositeBand,
  toRavenBandData,
  toRavenPefEntries,
} from '../util';

@Injectable()
export class SituationalAwarenessEffects {
  constructor(
    private actions: Actions,
    private http: HttpClient,
    private store: Store<RavenAppState>,
  ) {}

  changeSituationalAwareness = createEffect(() =>
    this.actions.pipe(
      ofType(SituationalAwarenessActions.changeSituationalAwareness),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      concatMap(({ state, action }) =>
        concat(
          of(
            SituationalAwarenessActions.updateSituationalAwarenessSettings({
              update: {
                situationalAware: action.situAware,
              },
            }),
          ),
          action.situAware
            ? this.getPefEntriesAsState(
                action.url,
                state.config.raven.defaultBandSettings,
              )
            : of(
                TimelineActions.removeBandsOrPointsForSource({
                  sourceId: 'situAwarePef',
                }),
              ),
          action.situAware
            ? of(
                TimelineActions.updateViewTimeRange({
                  viewTimeRange: getInitialPageStartEndTime(
                    state.raven.situationalAwareness,
                  ),
                }),
              )
            : [],
          of(
            SituationalAwarenessActions.updateSituationalAwarenessSettings({
              update: {
                fetchPending: false,
              },
            }),
          ),
        ),
      ),
      catchError(() => {
        return of(
          SituationalAwarenessActions.updateSituationalAwarenessSettings({
            update: {
              fetchPending: false,
              pefEntries: [],
            },
          }),
        );
      }),
    ),
  );

  fetchPefEntries = createEffect(() =>
    this.actions.pipe(
      ofType(SituationalAwarenessActions.fetchPefEntries),
      concatMap(action => this.getPefEntries(action.url)),
      catchError((e: Error) => {
        console.error(
          'SituationalAwarenessEffects - fetchSituationalAwareness: ',
          e.message,
        );
        return of(
          SituationalAwarenessActions.updateSituationalAwarenessSettings({
            update: {
              pefEntries: [],
            },
          }),
        );
      }),
    ),
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
          SituationalAwarenessActions.updateSituationalAwarenessSettings({
            update: {
              pefEntries,
            },
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
          '',
        ),
      ),
      switchMap(subBands =>
        of(
          TimelineActions.addBand({
            band: toCompositeBand(subBands[0]),
            sourceId: 'situAwarePef',
          }),
        ),
      ),
    );
  }
}
