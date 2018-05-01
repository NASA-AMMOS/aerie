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

import { Observable } from 'rxjs/Observable';
import { concat } from 'rxjs/observable/concat';
import { of } from 'rxjs/observable/of';

import {
  concatMap,
  map,
  switchMap,
  withLatestFrom,
} from 'rxjs/operators';

import { AppState } from './../../app/store';

import * as layoutActions from './../actions/layout';
import { TimelineActionTypes } from './../actions/timeline';

import {
  SelectPoint,
  UpdateViewTimeRange,
} from './../actions/timeline';

import * as timelineActions from './../actions/timeline';

import {
  MpsServerGraphData,
  MpsServerResourcePoint,
  RavenCompositeBand,
  RavenResourceBand,
  RavenSource,
  RavenTimeRange,
} from './../shared/models';

import {
  getResourcePoints,
  timestamp,
} from './../shared/util';

@Injectable()
export class TimelineEffects {
  /**
   * Effect for SelectPoint.
   */
  @Effect()
  selectPoint$: Observable<Action> = this.actions$.pipe(
    ofType<SelectPoint>(TimelineActionTypes.SelectPoint),
    withLatestFrom(this.store$),
    map(([action, state]) => state),
    concatMap(state => {
      const actions: Action[] = [];

      if (state.timeline.selectedPoint) {
        actions.push(new layoutActions.UpdateLayout({
          rightDrawerSelectedTabIndex: 2,
        }));

        if (!state.layout.showRightDrawer) {
          actions.push(new layoutActions.ToggleRightDrawer());
        }
      }

      return actions;
    }),
  );

  /**
   * Effect for UpdateViewTimeRange.
   * This effect returns an ordered sequence of actions (via `concat`).
   * The actions are built like so:
   *
   * 1. Loop through all sub-bands currently in the store:
   * 2.     If a sub-band is a resource, and has decimate set to true, then add actions:
   * 3.         fetchPending true action.
   * 4.         fetch new resource points for each source in the sub-band and add those new points to the sub-band via UpdateSubBand action.
   * 5.         fetchPending false action.
   */
  @Effect()
  updateViewTimeRange$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateViewTimeRange>(TimelineActionTypes.UpdateViewTimeRange),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state: { sourceExplorer, timeline } }) => {
      const actions: Observable<Action>[] = [];

      timeline.bands.forEach((band: RavenCompositeBand) => {
        band.subBands.forEach((subBand: RavenResourceBand) => {
          // Only resources with the `decimate` flag should re-query for data.
          if (subBand.type === 'resource' && subBand.decimate) {
            subBand.sourceIds.forEach(sourceId => {
              actions.push(
                of(new timelineActions.UpdateTimeline({ fetchPending: true })),
                this.fetchNewResourcePoints(sourceExplorer.treeBySourceId[sourceId], action.viewTimeRange).pipe(
                  switchMap(({ points }) => [
                    new timelineActions.UpdateSubBand(band.id, subBand.id, {
                      points: subBand.points.filter(point => point.sourceId !== sourceId).concat(points),
                    }),
                  ]),
                ),
                of(new timelineActions.UpdateTimeline({ fetchPending: false })),
              );
            });
          }
        });
      });

      return concat(
        ...actions,
      );
    }),
  );

  /**
   * Helper. Fetches new resource points for a given time range.
   * Good for use on decimated data.
   */
  fetchNewResourcePoints(source: RavenSource, viewTimeRange: RavenTimeRange) {
    const { end, start } = viewTimeRange;
    const url = `${source.url}&start=${timestamp(start)}&end=${timestamp(end)}`;

    return this.http.get(url).pipe(
      map((graphData: MpsServerGraphData) => graphData['Timeline Data']),
      map((timelineData: MpsServerResourcePoint[]) => getResourcePoints(source.id, timelineData)),
    );
  }

  constructor(
    private actions$: Actions,
    private http: HttpClient,
    private store$: Store<AppState>,
  ) {}
}
