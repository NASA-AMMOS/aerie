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

import { Actions, Effect } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { forkJoin } from 'rxjs/observable/forkJoin';
import { of } from 'rxjs/observable/of';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/withLatestFrom';

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
  getPointsByType,
  updateTimeRanges,
} from './../shared/util';

@Injectable()
export class DisplayEffects {
  @Effect()
  stateLoad$: Observable<Action> = this.actions$
    .ofType<displayActions.StateLoad>(DisplayActionTypes.StateLoad)
    .mergeMap(() => {
      // TODO: HTTP call to get saved state.
      const serializedState = localStorage.getItem('state');

      if (serializedState) {
        return of(JSON.parse(serializedState));
      } else {
        return of({});
      }
    })
    .mergeMap((state: AppState) =>
      forkJoin([
        of(state),
        ...this.getPointDataForSubBands(state.timeline.bands),
      ]),
    )
    .map(([state, ...pointData]) => ({ state, pointData }))
    .mergeMap(({ state, pointData }) => {
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
    });

  @Effect({ dispatch: false })
  stateSave$: Observable<Action> = this.actions$
    .ofType<displayActions.StateSave>(DisplayActionTypes.StateSave)
    .withLatestFrom(this.store$)
    .map(([action, state]) => state)
    .mergeMap((state: AppState) => {
      const stateToSave: AppState = {
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
      };

      // TODO: HTTP call to save.
      localStorage.setItem('state', JSON.stringify(stateToSave));

      return [];
    });

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
          this.http.get<MpsServerGraphData>(subBand.sourceUrl)
            .map((graphData: MpsServerGraphData) => ({
              ...getPointsByType(subBand, graphData['Timeline Data']),
              subBandId: subBand.id,
            }))
            .catch((e) => {
              console.error(`DisplayEffects - getPointDataForSubBands - failure to GET ${subBand.sourceUrl}: `, e);
              return of(null);
            }),
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
