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
import { TimelineActionTypes } from '../actions/timeline.actions';
import { RavenAppState } from '../raven-store';

import { concat, Observable, of } from 'rxjs';

import { concatMap, map, switchMap, withLatestFrom } from 'rxjs/operators';

import {
  AddBand,
  AddSubBand,
  FetchChildrenOrDescendants,
  FetchChildrenOrDescendantsSuccess,
  PanLeftViewTimeRange,
  PanRightViewTimeRange,
  PinAdd,
  PinRemove,
  PinRename,
  RemoveAllBands,
  ResetViewTimeRange,
  UpdateViewTimeRange,
  ZoomInViewTimeRange,
  ZoomOutViewTimeRange,
} from '../actions/timeline.actions';

import {
  MpsServerGraphData,
  MpsServerResourceMetadata,
  MpsServerResourcePoint,
  RavenActivityPoint,
  RavenCompositeBand,
  RavenDefaultBandSettings,
  RavenPin,
  RavenResourceBand,
  RavenSource,
  RavenSubBand,
  RavenTimeRange,
  StringTMap,
} from '../../shared/models';

import {
  activityBandsWithLegendAndSourceId,
  getPinLabel,
  getResourcePoints,
  subBandById,
  timestamp,
  toCompositeBand,
  toRavenDescendantsData,
} from '../../shared/util';

import * as sourceExplorerActions from '../actions/source-explorer.actions';
import * as timelineActions from '../actions/timeline.actions';

@Injectable()
export class TimelineEffects {
  constructor(
    private actions$: Actions,
    private http: HttpClient,
    private store$: Store<RavenAppState>,
  ) {}

  @Effect()
  fetchChildrenOrDescendants$: Observable<Action> = this.actions$.pipe(
    ofType<FetchChildrenOrDescendants>(
      TimelineActionTypes.FetchChildrenOrDescendants,
    ),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      concat(
        of(new timelineActions.UpdateTimeline({ fetchPending: true })),
        this.fetchChildrenOrDescendants(
          state.raven.timeline.bands
            ? subBandById(
                state.raven.timeline.bands,
                action.bandId,
                action.subBandId,
              )
            : null,
          action.activityPoint.uniqueId,
          action.expandType === 'expandChildren'
            ? action.activityPoint.childrenUrl
            : action.activityPoint.descendantsUrl,
          state.config.raven.defaultBandSettings,
          state.raven.sourceExplorer.treeBySourceId,
          state.raven.timeline.bands,
          state.raven.timeline.expansionByActivityId,
        ),
        of(new timelineActions.UpdateTimeline({ fetchPending: false })),
      ),
    ),
  );

  @Effect()
  fetchChildrenOrDescendantsSuccess$: Observable<Action> = this.actions$.pipe(
    ofType<FetchChildrenOrDescendantsSuccess>(
      TimelineActionTypes.FetchChildrenOrDescendantsSuccess,
    ),
    withLatestFrom(this.store$),
    map(([, state]) => state.raven.timeline),
    concatMap(timeline =>
      concat(
        this.getChildrenOfExpandedPoints(
          timeline.bands,
          timeline.expansionByActivityId,
        ),
      ),
    ),
  );

  /**
   * Effect for RemoveAllBands.
   */
  @Effect()
  removeAllBands$: Observable<Action> = this.actions$.pipe(
    ofType<RemoveAllBands>(TimelineActionTypes.RemoveAllBands),
    withLatestFrom(this.store$),
    map(([, state]) => state.raven),
    concatMap(raven =>
      this.removeAllBands(
        raven.timeline.bands,
        raven.sourceExplorer.treeBySourceId,
      ),
    ),
  );

  /**
   * Effect for AddBand | PinAdd | PinRemove | PinRename.
   */
  @Effect()
  updatePinLabels$: Observable<Action> = this.actions$.pipe(
    ofType<AddBand | AddSubBand | PinAdd | PinRemove | PinRename>(
      TimelineActionTypes.AddBand,
      TimelineActionTypes.AddSubBand,
      TimelineActionTypes.PinAdd,
      TimelineActionTypes.PinRemove,
      TimelineActionTypes.PinRename,
    ),
    withLatestFrom(this.store$),
    map(([, state]) => state.raven),
    concatMap(({ timeline, sourceExplorer }) =>
      this.updatePinLabels(timeline.bands, sourceExplorer.pins),
    ),
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
    ofType<
      | ResetViewTimeRange
      | PanLeftViewTimeRange
      | PanRightViewTimeRange
      | UpdateViewTimeRange
      | ZoomInViewTimeRange
      | ZoomOutViewTimeRange
    >(
      TimelineActionTypes.ResetViewTimeRange,
      TimelineActionTypes.PanLeftViewTimeRange,
      TimelineActionTypes.PanRightViewTimeRange,
      TimelineActionTypes.UpdateViewTimeRange,
      TimelineActionTypes.ZoomInViewTimeRange,
      TimelineActionTypes.ZoomOutViewTimeRange,
    ),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state: { raven: { sourceExplorer, timeline } } }) => {
      const actions: Observable<Action>[] = [];
      const viewTimeRange =
        (action as UpdateViewTimeRange).viewTimeRange || timeline.viewTimeRange;

      timeline.bands.forEach((band: RavenCompositeBand) => {
        band.subBands.forEach((subBand: RavenResourceBand) => {
          // Only resources with the `decimate` flag should re-query for data.
          if (subBand.type === 'resource' && subBand.decimate) {
            subBand.sourceIds.forEach(sourceId => {
              actions.push(
                of(new timelineActions.UpdateTimeline({ fetchPending: true })),
                this.fetchNewResourcePoints(
                  sourceExplorer.treeBySourceId[sourceId],
                  viewTimeRange,
                ).pipe(
                  switchMap(({ points }) => [
                    new timelineActions.UpdateSubBand(band.id, subBand.id, {
                      points: subBand.points
                        .filter(point => point.sourceId !== sourceId)
                        .concat(points),
                    }),
                  ]),
                ),
                of(new timelineActions.UpdateTimeline({ fetchPending: false })),
              );
            });
          }
        });
      });

      return concat(...actions);
    }),
  );

  /**
   * Helper. Fetches children or descendants of an activity point
   */
  fetchChildrenOrDescendants(
    parentSubBand: RavenSubBand | null,
    activityPointId: string,
    url: string,
    defaultBandSettings: RavenDefaultBandSettings,
    treeBySourceId: StringTMap<RavenSource>,
    currentBands: RavenCompositeBand[],
    expansionByActivityId: StringTMap<string>,
  ) {
    let sourceId = '';
    let pinLabel = '';
    if (parentSubBand) {
      sourceId = parentSubBand.sourceIds[0];
      pinLabel = parentSubBand.labelPin;
    }
    return this.http.get(url).pipe(
      map((graphData: MpsServerGraphData) =>
        toRavenDescendantsData(
          '__childrenOrDescendants',
          activityPointId,
          graphData,
          defaultBandSettings,
          null,
          treeBySourceId,
        ),
      ),
      concatMap((newSubBands: RavenSubBand[]) => {
        const actions: Action[] = [];

        if (newSubBands.length > 0) {
          newSubBands.forEach((subBand: RavenSubBand) => {
            const activityBands = activityBandsWithLegendAndSourceId(
              currentBands,
              subBand,
              pinLabel,
              '',
            );
            if (activityBands.length > 0) {
              activityBands.forEach(activityBand => {
                actions.push(
                  new timelineActions.AddPointsToSubBand(
                    sourceId,
                    activityBand.bandId,
                    activityBand.subBandId,
                    subBand.points,
                  ),
                );
              });
            } else {
              const ravenCompositeBand = toCompositeBand(subBand);
              actions.push(
                new timelineActions.AddBand(
                  '__childrenOrDescendants',
                  ravenCompositeBand,
                ),
              );
            }
          });
          actions.push(new timelineActions.FetchChildrenOrDescendantsSuccess());
        }
        return actions;
      }),
    );
  }

  /**
   * Helper. Fetches new resource points for a given time range.
   * Good for use on decimated data.
   */
  fetchNewResourcePoints(source: RavenSource, viewTimeRange: RavenTimeRange) {
    const { end, start } = viewTimeRange;
    const url = `${source.url}&start=${timestamp(start)}&end=${timestamp(end)}`;

    return this.http
      .get(url)
      .pipe(
        map((graphData: MpsServerGraphData) =>
          getResourcePoints(
            source.id,
            graphData['Timeline Metadata'] as MpsServerResourceMetadata,
            graphData['Timeline Data'] as MpsServerResourcePoint[],
          ),
        ),
      );
  }

  /**
   * Helper. Returns list of Actions to fetch children of expanded activity points.
   */
  getChildrenOfExpandedPoints(
    allCompositeBands: RavenCompositeBand[],
    expansionByActivityId: StringTMap<string>,
  ) {
    const actions: Action[] = [];
    for (let i = 0, l = allCompositeBands.length; i < l; i++) {
      for (let j = 0, ll = allCompositeBands[i].subBands.length; j < ll; j++) {
        const subBand = allCompositeBands[i].subBands[j];
        for (let k = 0, lll = subBand.points.length; k < lll; k++) {
          const point = subBand.points[k];
          if (
            (point as RavenActivityPoint).expansion === 'noExpansion' &&
            expansionByActivityId[point.activityId]
          ) {
            actions.push(
              new timelineActions.ExpandChildrenOrDescendants(
                allCompositeBands[i].id,
                subBand.id,
                point,
                expansionByActivityId[(point as RavenActivityPoint).activityId],
              ),
            ),
              actions.push(
                new timelineActions.FetchChildrenOrDescendants(
                  allCompositeBands[i].id,
                  subBand.id,
                  point as RavenActivityPoint,
                  expansionByActivityId[
                    (point as RavenActivityPoint).activityId
                  ],
                ),
              );
            return actions;
          }
        }
      }
    }
    return actions;
  }

  /**
   *
   * Helper. Returns a list of action to remove bands from timeline and update source explorer.
   */
  removeAllBands(
    bands: RavenCompositeBand[],
    treeBySourceId: StringTMap<RavenSource>,
  ) {
    const actions: Action[] = [];

    bands.forEach((band: RavenCompositeBand) => {
      band.subBands.forEach((subBand: RavenSubBand) => {
        actions.push(new timelineActions.RemoveSubBand(subBand.id)),
          actions.push(
            new sourceExplorerActions.SubBandIdRemove(
              subBand.sourceIds,
              subBand.id,
            ),
          );
      });
    });

    return actions;
  }

  /**
   * Helper. Returns a list of actions that update the pin labels for all bands and their sub-bands.
   */
  updatePinLabels(bands: RavenCompositeBand[], pins: RavenPin[]): Action[] {
    const actions: Action[] = [];

    bands.forEach(band => {
      band.subBands.forEach(subBand => {
        actions.push(
          new timelineActions.UpdateSubBand(band.id, subBand.id, {
            labelPin: getPinLabel(subBand.sourceIds[0], pins),
          }),
        );
      });
    });

    return actions;
  }
}
