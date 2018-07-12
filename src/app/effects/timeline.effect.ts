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

import {
  concat,
  Observable,
  of,
} from 'rxjs';

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
  AddBand,
  PanLeftViewTimeRange,
  PanRightViewTimeRange,
  PinAdd,
  PinRemove,
  PinRename,
  ResetViewTimeRange,
  SelectPoint,
  UpdateViewTimeRange,
  ZoomInViewTimeRange,
  ZoomOutViewTimeRange,
} from './../actions/timeline';

import * as timelineActions from './../actions/timeline';

import {
  MpsServerGraphData,
  MpsServerResourceMetadata,
  MpsServerResourcePoint,
  RavenCompositeBand,
  RavenPin,
  RavenResourceBand,
  RavenSource,
  RavenTimeRange,
} from './../shared/models';

import {
  getPinLabel,
  getResourcePoints,
  timestamp,
} from './../shared/util';

@Injectable()
export class TimelineEffects {
  /**
   * Effect for AddBand | PinAdd | PinRemove | PinRename.
   */
  @Effect()
  updatePinLabels$: Observable<Action> = this.actions$.pipe(
    ofType<AddBand | PinAdd | PinRemove | PinRename>(
      TimelineActionTypes.AddBand,
      TimelineActionTypes.PinAdd,
      TimelineActionTypes.PinRemove,
      TimelineActionTypes.PinRename,
    ),
    withLatestFrom(this.store$),
    map(([action, state]) => state),
    concatMap(({ timeline, sourceExplorer }) => this.updatePinLabels(timeline.bands, sourceExplorer.pins)),
  );

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
          rightPanelSelectedTabIndex: 1,
        }));

        if (!state.layout.showRightPanel) {
          actions.push(new layoutActions.ToggleRightPanel());
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
    ofType<ResetViewTimeRange | PanLeftViewTimeRange | PanRightViewTimeRange | UpdateViewTimeRange | ZoomInViewTimeRange | ZoomOutViewTimeRange>(
      TimelineActionTypes.ResetViewTimeRange,
      TimelineActionTypes.PanLeftViewTimeRange,
      TimelineActionTypes.PanRightViewTimeRange,
      TimelineActionTypes.UpdateViewTimeRange,
      TimelineActionTypes.ZoomInViewTimeRange,
      TimelineActionTypes.ZoomOutViewTimeRange,
    ),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state: { sourceExplorer, timeline } }) => {
      const actions: Observable<Action>[] = [];
      const viewTimeRange = (action as UpdateViewTimeRange).viewTimeRange || timeline.viewTimeRange;

      timeline.bands.forEach((band: RavenCompositeBand) => {
        band.subBands.forEach((subBand: RavenResourceBand) => {
          // Only resources with the `decimate` flag should re-query for data.
          if (subBand.type === 'resource' && subBand.decimate) {
            subBand.sourceIds.forEach(sourceId => {
              actions.push(
                of(new timelineActions.UpdateTimeline({ fetchPending: true })),
                this.fetchNewResourcePoints(sourceExplorer.treeBySourceId[sourceId], viewTimeRange).pipe(
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
      map((graphData: MpsServerGraphData) =>
        getResourcePoints(source.id, graphData['Timeline Metadata'] as MpsServerResourceMetadata, graphData['Timeline Data'] as MpsServerResourcePoint[]),
      ),
    );
  }

  /**
   * Helper. Returns a list of actions that update the pin labels for all bands and their sub-bands.
   */
  updatePinLabels(bands: RavenCompositeBand[], pins: RavenPin[]): Action[] {
    const actions: Action[] = [];

    bands.forEach(band => {
      band.subBands.forEach(subBand => {
        actions.push(
          new timelineActions.UpdateSubBand(
            band.id,
            subBand.id,
            { labelPin: getPinLabel(subBand.sourceIds[0], pins) },
          ),
        );
      });
    });

    return actions;
  }

  constructor(
    private actions$: Actions,
    private http: HttpClient,
    private store$: Store<AppState>,
  ) {}
}
