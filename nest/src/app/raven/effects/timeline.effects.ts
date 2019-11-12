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
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { concat, Observable, of } from 'rxjs';
import {
  catchError,
  concatMap,
  map,
  switchMap,
  withLatestFrom,
} from 'rxjs/operators';
import {
  SourceExplorerActions,
  TimelineActions,
  ToastActions,
} from '../actions';
import {
  MpsServerDocumentId,
  MpsServerGraphData,
  MpsServerResourceMetadata,
  MpsServerResourcePoint,
  RavenActivityPoint,
  RavenCompositeBand,
  RavenPoint,
  RavenResourceBand,
  RavenResourcePoint,
  RavenSource,
  RavenSubBand,
  StringTMap,
  TimeRange,
} from '../models';
import { RavenAppState } from '../raven-store';
import {
  activityBandsWithLegendAndSourceId,
  getPinLabel,
  getResourcePoints,
  subBandById,
  timestamp,
  toCompositeBand,
  toRavenDescendantsData,
} from '../util';

@Injectable()
export class TimelineEffects {
  constructor(
    private actions: Actions,
    private http: HttpClient,
    private store: Store<RavenAppState>,
  ) {}

  fetchChildrenOrDescendants = createEffect(() =>
    this.actions.pipe(
      ofType(TimelineActions.fetchChildrenOrDescendants),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      concatMap(({ action, state }) => {
        let sourceId = '';
        let pinLabel = '';

        const activityPointId = action.activityPoint.uniqueId;
        const parentSubBand = state.raven.timeline.bands
          ? subBandById(
              state.raven.timeline.bands,
              action.bandId,
              action.subBandId,
            )
          : null;
        const url =
          action.expandType === 'expandChildren'
            ? action.activityPoint.childrenUrl
            : action.activityPoint.descendantsUrl;
        const defaultBandSettings = state.config.raven.defaultBandSettings;
        const treeBySourceId = state.raven.sourceExplorer.treeBySourceId;
        const currentBands = state.raven.timeline.bands;

        if (parentSubBand) {
          sourceId = parentSubBand.sourceIds[0];
          pinLabel = parentSubBand.labelPin;
        }

        return concat(
          of(
            TimelineActions.updateTimeline({ update: { fetchPending: true } }),
          ),
          this.http.get(url).pipe(
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
                        TimelineActions.addPointsToSubBand({
                          bandId: activityBand.bandId,
                          points: subBand.points,
                          sourceId,
                          subBandId: activityBand.subBandId,
                        }),
                      );
                    });
                  } else {
                    const ravenCompositeBand = toCompositeBand(subBand);
                    actions.push(
                      TimelineActions.addBand({
                        band: ravenCompositeBand,
                        sourceId: '__childrenOrDescendants',
                      }),
                    );
                  }
                });
                actions.push(
                  TimelineActions.fetchChildrenOrDescendantsSuccess(),
                );
              }
              return actions;
            }),
          ),
          of(
            TimelineActions.updateTimeline({ update: { fetchPending: false } }),
          ),
        );
      }),
    ),
  );

  fetchChildrenOrDescendantsSuccess = createEffect(() =>
    this.actions.pipe(
      ofType(TimelineActions.fetchChildrenOrDescendantsSuccess),
      withLatestFrom(this.store),
      map(([, state]) => state.raven.timeline),
      concatMap(({ bands, expansionByActivityId }) => {
        const actions: Action[] = [];

        for (let i = 0, l = bands.length; i < l; i++) {
          for (let j = 0, ll = bands[i].subBands.length; j < ll; j++) {
            const subBand = bands[i].subBands[j];
            for (let k = 0, lll = subBand.points.length; k < lll; k++) {
              const activityPoint = subBand.points[k] as RavenActivityPoint;
              if (
                activityPoint.expansion === 'noExpansion' &&
                expansionByActivityId[activityPoint.activityId]
              ) {
                actions.push(
                  TimelineActions.expandChildrenOrDescendants({
                    activityPoint,
                    bandId: bands[i].id,
                    expandType: expansionByActivityId[activityPoint.activityId],
                    subBandId: subBand.id,
                  }),
                  TimelineActions.fetchChildrenOrDescendants({
                    activityPoint,
                    bandId: bands[i].id,
                    expandType: expansionByActivityId[activityPoint.activityId],
                    subBandId: subBand.id,
                  }),
                );
                return actions;
              }
            }
          }
        }

        return concat(actions);
      }),
    ),
  );

  removeAllBands = createEffect(() =>
    this.actions.pipe(
      ofType(TimelineActions.removeAllBands),
      withLatestFrom(this.store),
      map(([, state]) => state.raven),
      concatMap(raven => {
        const actions: Action[] = [];

        raven.timeline.bands.forEach((band: RavenCompositeBand) => {
          band.subBands.forEach((subBand: RavenSubBand) => {
            actions.push(
              TimelineActions.removeSubBand({ subBandId: subBand.id }),
              SourceExplorerActions.subBandIdRemove({
                sourceIds: subBand.sourceIds,
                subBandId: subBand.id,
              }),
            );
          });
        });

        return actions;
      }),
    ),
  );

  updateCsvFile = createEffect(() =>
    this.actions.pipe(
      ofType(TimelineActions.updateCsvFile),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      concatMap(({ action, state }) =>
        concat(
          ...this.doUpdateCsvFile(
            state,
            action.bandId,
            action.subBandId,
            action.sourceId,
            action.points,
            action.csvHeaderMap,
          ),
        ),
      ),
    ),
  );

  updatePinLabels = createEffect(() =>
    this.actions.pipe(
      ofType(
        TimelineActions.addBand,
        TimelineActions.addSubBand,
        TimelineActions.pinAdd,
        TimelineActions.pinRemove,
        TimelineActions.pinRename,
      ),
      withLatestFrom(this.store),
      map(([, state]) => state.raven),
      concatMap(({ timeline, sourceExplorer }) => {
        const actions: Action[] = [];

        timeline.bands.forEach(band => {
          band.subBands.forEach(subBand => {
            if (subBand.type !== 'divider') {
              actions.push(
                TimelineActions.updateSubBand({
                  bandId: band.id,
                  subBandId: subBand.id,
                  update: {
                    labelPin: getPinLabel(
                      subBand.sourceIds[0],
                      sourceExplorer.pins,
                    ),
                  },
                }),
              );
            }
          });
        });

        return actions;
      }),
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
  updateViewTimeRange = createEffect(() =>
    this.actions.pipe(
      ofType(
        TimelineActions.resetViewTimeRange,
        TimelineActions.panLeftViewTimeRange,
        TimelineActions.panRightViewTimeRange,
        TimelineActions.updateViewTimeRange,
        TimelineActions.zoomInViewTimeRange,
        TimelineActions.zoomOutViewTimeRange,
      ),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      switchMap(
        ({
          action,
          state: {
            raven: { sourceExplorer, timeline },
          },
        }) => {
          const actions: Observable<Action>[] = [];
          const viewTimeRange =
            (action as any).viewTimeRange || timeline.viewTimeRange;

          timeline.bands.forEach((band: RavenCompositeBand) => {
            band.subBands.forEach((subBand: RavenResourceBand) => {
              // Only resources with the `decimate` flag should re-query for data.
              if (subBand.type === 'resource' && subBand.decimate) {
                subBand.sourceIds.forEach(sourceId => {
                  actions.push(
                    of(
                      TimelineActions.updateTimeline({
                        update: { fetchPending: true },
                      }),
                    ),
                    this.fetchNewResourcePoints(
                      sourceExplorer.treeBySourceId[sourceId],
                      viewTimeRange,
                    ).pipe(
                      switchMap(({ points }) => [
                        TimelineActions.updateSubBand({
                          bandId: band.id,
                          subBandId: subBand.id,
                          update: {
                            points: subBand.points
                              .filter(point => point.sourceId !== sourceId)
                              .concat(points),
                          },
                        }),
                      ]),
                    ),
                    of(
                      TimelineActions.updateTimeline({
                        update: { fetchPending: false },
                      }),
                    ),
                  );
                });
              }
            });
          });

          return concat(...actions);
        },
      ),
    ),
  );

  /**
   * Fetches new resource points for a given time range.
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

  doUpdateCsvFile(
    state: RavenAppState,
    selectedBandId: string,
    selectedSubBandId: string,
    dataSourceUrl: string,
    points: RavenPoint[],
    csvHeaderMap: StringTMap<string>,
  ): Observable<Action>[] {
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
                TimelineActions.updateCsvFileSuccess(),
                TimelineActions.removePointsInSubBand({
                  bandId: selectedBandId,
                  points: [point],
                  subBandId: selectedSubBandId,
                }),
              );
            }),
            catchError((e: Error) => {
              return [
                ToastActions.showToast({
                  message: 'Failed To Update CSV File',
                  title: '',
                  toastType: 'warning',
                }),
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
                    TimelineActions.updatePointInSubBand({
                      bandId: selectedBandId,
                      pointId: point.id,
                      subBandId: selectedSubBandId,
                      update: {
                        id: ids[0]['_id']['$oid'],
                        pointStatus: 'unchanged',
                      },
                    }),
                    TimelineActions.updateCsvFileSuccess(),
                  );
                }),
                catchError((e: Error) => {
                  return [
                    ToastActions.showToast({
                      message: 'Failed To Update CSV File',
                      title: '',
                      toastType: 'warning',
                    }),
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
                    TimelineActions.updateCsvFileSuccess(),
                    TimelineActions.updatePointInSubBand({
                      bandId: selectedBandId,
                      pointId: point.id,
                      subBandId: selectedSubBandId,
                      update: { pointStatus: 'unchanged' },
                    }),
                  );
                }),
                catchError((e: Error) => {
                  return [
                    ToastActions.showToast({
                      message: 'Failed To Update CSV File',
                      title: '',
                      toastType: 'warning',
                    }),
                  ];
                }),
              ),
          );
        }
      }
    });
    actions.push(
      of(
        TimelineActions.updateSubBand({
          bandId: selectedBandId,
          subBandId: selectedSubBandId,
          update: {
            pointsChanged: false,
          },
        }),
      ),
    );
    return actions;
  }
}
