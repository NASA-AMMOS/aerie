/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { keyBy } from 'lodash';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';

import { Observable } from 'rxjs/Observable';
import { forkJoin } from 'rxjs/observable/forkJoin';
import { of } from 'rxjs/observable/of';

import {
  catchError,
  map,
  mergeMap,
  withLatestFrom,
} from 'rxjs/operators';

import { AppState } from './../../app/store';

import { DisplayActionTypes } from './../actions/display';

import * as displayActions from './../actions/display';
import * as sourceExplorerActions from './../actions/source-explorer';
import * as timelineActions from './../actions/timeline';

import {
  MpsServerGraphData,
  RavenCompositeBand,
  RavenSubBand,
  RavenSubBandPointData,
  StringTMap,
} from './../shared/models';

import {
  getPointsBySubBandType,
  updateTimeRanges,
} from './../shared/util';

@Injectable()
export class DisplayEffects {
  @Effect()
  stateDelete$: Observable<Action> = this.actions$.pipe(
    ofType<displayActions.StateDelete>(DisplayActionTypes.StateDelete),
    withLatestFrom(this.store$),
    map(([action, state]) => action),
    map(action => action.source.url),
    mergeMap(url =>
      // TODO: Make this better.
      this.http.delete(url.substring(0, url.indexOf('?')).replace(/generic-mongodb/i, 'fs-mongodb')).pipe(
        map(() => new displayActions.StateDeleteSuccess()),
        catchError(() => of(new displayActions.StateDeleteFailure())),
      ),
    ),
  );

  @Effect()
  stateLoad$: Observable<Action> = this.actions$.pipe(
    ofType<displayActions.StateLoad>(DisplayActionTypes.StateLoad),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    mergeMap(({ state, action }) =>
      this.http.get(action.source.url).pipe(
        map(res => res[0]),
        map(data => data.state),
      ),
    ),
    mergeMap((state: AppState) =>
      forkJoin([
        of(state),
        ...this.getPointDataForSubBands(state.timeline.bands),
      ]),
    ),
    map(([state, ...pointData]) => ({ state, pointData })),
    mergeMap(({ state, pointData }) => {
      const bands = this.updateSubBandsWithPointData(state.timeline.bands, pointData);

      return [
        new timelineActions.UpdateTimeline({
          bands,
          labelWidth: state.timeline.labelWidth,
          ...updateTimeRanges({ end: 0, start: 0 }, bands),
        }),
        new sourceExplorerActions.UpdateSourceExplorer({ ...state.sourceExplorer }),
        new displayActions.StateLoadSuccess(),
      ];
    }),
  );

  @Effect()
  stateSave$: Observable<Action> = this.actions$.pipe(
    ofType<displayActions.StateSave>(DisplayActionTypes.StateSave),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    mergeMap(({ state, action }) => {
      const stateToSave = {
        name: `raven2-state-${action.name}`,
        state: {
          ...state,
          timeline: {
            ...state.timeline,
            bands: state.timeline.bands.map(band => ({
              ...band,
              subBands: band.subBands.map(subBand => ({
                ...subBand,
                points: [],
              })),
            })),
          },
        },
      };

      return this.http.put(`${action.source.url}/${action.name}`, stateToSave).pipe(
        map(() => new displayActions.StateSaveSuccess()),
        catchError(() => of(new displayActions.StateLoadFailure())),
      );
    }),
  );

  constructor(
    private http: HttpClient,
    private actions$: Actions,
    private store$: Store<AppState>,
  ) {}

  /**
   * Helper. Returns an Observable that resolves to all sub-band points for the given list of bands.
   */
  getPointDataForSubBands(bands: RavenCompositeBand[]): Observable<RavenSubBandPointData | null>[] {
    const pointData: Observable<RavenSubBandPointData | null>[] = [];

    bands.forEach((band: RavenCompositeBand) => {
      band.subBands.forEach((subBand: RavenSubBand) => {
        pointData.push(
          this.http.get<MpsServerGraphData>(subBand.sourceUrl).pipe(
            map((graphData: MpsServerGraphData) => ({
              ...getPointsBySubBandType(subBand, graphData['Timeline Data']),
              subBandId: subBand.id,
            })),
            catchError((e) => {
              console.error(`DisplayEffects - getPointDataForSubBands - failure to GET ${subBand.sourceUrl}: `, e);
              return of(null);
            }),
          ),
        );
      });
    });

    return pointData;
  }

  /**
   * Helper. Returns a new list of bands with sub-bands updated by the given point data.
   */
  updateSubBandsWithPointData(bands: RavenCompositeBand[], pointData: RavenSubBandPointData[]): RavenCompositeBand[] {
    const pointDataBySubBandId: StringTMap<RavenSubBandPointData> = keyBy(pointData, 'subBandId');

    return bands.map((band: RavenCompositeBand) => ({
      ...band,
      subBands: band.subBands.map((subBand: RavenSubBand) => {
        if (pointDataBySubBandId[subBand.id]) {
          const { maxTimeRange, points } = pointDataBySubBandId[subBand.id];

          return {
            ...subBand,
            maxTimeRange,
            points,
          };
        }

        return subBand;
      }),
    }));
  }
}
