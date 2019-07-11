/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClient, HttpHeaders } from '@angular/common/http';
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
import * as toastActions from '../../shared/actions/toast.actions';
import { StringTMap, TimeRange } from '../../shared/models';
import { timestamp } from '../../shared/util';
import * as sourceExplorerActions from '../actions/source-explorer.actions';
import { TimelineActionTypes } from '../actions/timeline.actions';
import * as timelineActions from '../actions/timeline.actions';
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
  UpdateCsvFile,
  UpdateViewTimeRange,
  ZoomInViewTimeRange,
  ZoomOutViewTimeRange,
} from '../actions/timeline.actions';
import {
  MpsServerDocumentId,
  MpsServerGraphData,
  MpsServerResourceMetadata,
  MpsServerResourcePoint,
  RavenActivityPoint,
  RavenCompositeBand,
  RavenDefaultBandSettings,
  RavenPin,
  RavenPoint,
  RavenResourceBand,
  RavenResourcePoint,
  RavenSource,
  RavenSubBand,
} from '../models';
import { RavenAppState } from '../raven-store';
import {
  activityBandsWithLegendAndSourceId,
  getPinLabel,
  getResourcePoints,
  subBandById,
  toCompositeBand,
  toRavenDescendantsData,
} from '../util';

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

  @Effect()
  updateCsvFile$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateCsvFile>(TimelineActionTypes.UpdateCsvFile),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      concat(
        ...this.updateCsvFile(
          state,
          action.selectedBandId,
          action.selectedSubBandId,
          action.sourceId,
          action.points,
          action.csvHeaderMap,
        ),
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
  fetchNewResourcePoints(source: RavenSource, viewTimeRange: TimeRange) {
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

  updateCsvFile(
    state: RavenAppState,
    selectedBandId: string,
    selectedSubBandId: string,
    dataSourceUrl: string,
    points: RavenPoint[],
    csvHeaderMap: StringTMap<string>,
  ) {
    const headers = new HttpHeaders().set('Content-Type', `application/json`);
    const responseType = 'text';

    const actions: Observable<Action>[] = [];

    points.forEach(point => {
      const fileUrl = dataSourceUrl.substring(
        0,
        dataSourceUrl.lastIndexOf('/'),
      );
      let url = `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}${fileUrl}?__document_id=${point.id}`;
      if (point.pointStatus === 'deleted') {
        actions.push(
          this.http.delete(url, { responseType: 'text' }).pipe(
            concatMap(() => {
              return of(
                new timelineActions.UpdateCsvFileSuccess(),
                new timelineActions.RemovePointsInSubBand(
                  selectedBandId,
                  selectedSubBandId,
                  [point],
                ),
              );
            }),
            catchError((e: Error) => {
              return [
                new toastActions.ShowToast(
                  'warning',
                  'Failed To Update CSV File',
                  '',
                ),
              ];
            }),
          ),
        );
      } else if (point.pointStatus !== 'unchanged') {
        // Map point data back to what the server sent to Raven.
        const serverData =
          point.type === 'activity'
            ? {
                'Activity Name': (point as RavenActivityPoint).activityName,
                'Activity Type': (point as RavenActivityPoint).activityType,
                'Tend Assigned': timestamp((point as RavenActivityPoint).end),
                'Tstart Assigned': timestamp(
                  (point as RavenActivityPoint).start,
                ),
              }
            : {
                'Data Timestamp': timestamp(
                  (point as RavenResourcePoint).start,
                ),
                'Data Value': (point as RavenResourcePoint).value,
              };

        // Map to original CSV data.
        let data: string;
        if (Object.keys(csvHeaderMap).length > 0) {
          const mappedData = {};
          Object.keys(csvHeaderMap).forEach(
            key => (mappedData[csvHeaderMap[key]] = serverData[key]),
          );
          data = JSON.stringify(mappedData);
        } else {
          data = JSON.stringify(serverData);
        }
        if (point.pointStatus === 'added') {
          url = url.substring(0, url.indexOf('?'));
          actions.push(
            this.http
              .post(url, data, {
                headers,
                responseType,
              })
              .pipe(
                map((idstr: any) => JSON.parse(idstr)),
                concatMap((ids: MpsServerDocumentId[]) => {
                  return of(
                    new timelineActions.UpdatePointInSubBand(
                      selectedBandId,
                      selectedSubBandId,
                      point.id,
                      { id: ids[0]['_id']['$oid'], pointStatus: 'unchanged' },
                    ),
                    new timelineActions.UpdateCsvFileSuccess(),
                  );
                }),
                catchError((e: Error) => {
                  return [
                    new toastActions.ShowToast(
                      'warning',
                      'Failed To Update CSV File',
                      '',
                    ),
                  ];
                }),
              ),
          );
        } else {
          actions.push(
            this.http
              .put(url, data, {
                headers,
                responseType,
              })
              .pipe(
                concatMap(() => {
                  return of(
                    new timelineActions.UpdateCsvFileSuccess(),
                    new timelineActions.UpdatePointInSubBand(
                      selectedBandId,
                      selectedSubBandId,
                      point.id,
                      { pointStatus: 'unchanged' },
                    ),
                  );
                }),
                catchError((e: Error) => {
                  return [
                    new toastActions.ShowToast(
                      'warning',
                      'Failed To Update CSV File',
                      '',
                    ),
                  ];
                }),
              ),
          );
        }
      }
    });
    actions.push(of(new timelineActions.UpdateSubBand(
      selectedBandId,
      selectedSubBandId,
      { pointsChanged: false },
    )));
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
