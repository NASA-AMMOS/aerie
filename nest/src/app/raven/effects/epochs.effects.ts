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
import { catchError, concatMap, map, withLatestFrom } from 'rxjs/operators';
import * as toastActions from '../../shared/actions/toast.actions';
import {
  EpochsActionTypes,
  FetchEpochs,
  SaveNewEpochFile,
  UpdateProjectEpochs,
} from '../actions/epochs.actions';
import * as epochsActions from '../actions/epochs.actions';
import * as sourceExplorerActions from '../actions/source-explorer.actions';
import { MpsServerEpoch, RavenEpoch } from '../models';
import { RavenAppState } from '../raven-store';
import { toRavenEpochs } from '../util';

@Injectable()
export class EpochsEffects {
  constructor(
    private http: HttpClient,
    private actions$: Actions,
    private store$: Store<RavenAppState>,
  ) {}

  @Effect()
  fetchEpochs$: Observable<Action> = this.actions$.pipe(
    ofType<FetchEpochs>(EpochsActionTypes.FetchEpochs),
    concatMap(action =>
      concat(
        of(
          new sourceExplorerActions.UpdateSourceExplorer({
            fetchPending: true,
          }),
        ),
        this.http.get(action.url).pipe(
          map((mpsServerEpochs: MpsServerEpoch[]) =>
            toRavenEpochs(mpsServerEpochs),
          ),
          map((ravenEpochs: RavenEpoch[]) =>
            action.replaceAction === 'AppendAndReplace'
              ? new epochsActions.AppendAndReplaceEpochs(ravenEpochs)
              : new epochsActions.AddEpochs(ravenEpochs),
          ),
        ),
        of(
          new sourceExplorerActions.UpdateSourceExplorer({
            fetchPending: false,
          }),
        ),
      ),
    ),
    catchError((e: Error) => {
      console.error('EpochsEffects - fetchEpochs$: ', e);
      return [
        new toastActions.ShowToast('warning', 'Failed to fetch epochs', ''),
      ];
    }),
  );

  @Effect()
  saveNewEpochFile$: Observable<Action> = this.actions$.pipe(
    ofType<SaveNewEpochFile>(EpochsActionTypes.SaveNewEpochFile),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) =>
      this.saveNewEpochFile(state, action.filePathName),
    ),
  );

  @Effect()
  updateProjectEpochs$: Observable<Action> = this.actions$.pipe(
    ofType<UpdateProjectEpochs>(EpochsActionTypes.UpdateProjectEpochs),
    withLatestFrom(this.store$),
    map(([, state]) => ({ state })),
    concatMap(({ state }) => this.updateProjectEpochs(state)),
  );

  saveNewEpochFile(state: RavenAppState, fileUrl: string) {
    const headers = new HttpHeaders().set('Content-Type', `application/json`);
    const responseType = 'text';
    const url = `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}/${fileUrl}?timeline_type=epoch`;

    return this.http
      .put(url, JSON.stringify(state.raven.epochs.epochs), {
        headers,
        responseType,
      })
      .pipe(
        concatMap(() => {
          return of(new epochsActions.SaveNewEpochFileSuccess());
        }),
        catchError((e: Error) => {
          console.error('EpochEffects - saveNewEpochFile$: ', e);
          return [
            new toastActions.ShowToast(
              'warning',
              'Failed To Save New Epoch File',
              '',
            ),
          ];
        }),
      );
  }

  updateProjectEpochs(state: RavenAppState) {
    if (
      state.config.mpsServer.epochsUrl &&
      state.config.mpsServer.epochsUrl.length > 0
    ) {
      const headers = new HttpHeaders().set('Content-Type', `application/json`);
      const responseType = 'text';
      const url = `${state.config.app.baseUrl}/${state.config.mpsServer.epochsUrl}?timeline_type=epoch`;

      return this.http
        .put(url, JSON.stringify(state.raven.epochs.epochs), {
          headers,
          responseType,
        })
        .pipe(
          concatMap(() => {
            return of(new epochsActions.UpdateProjectEpochsSuccess());
          }),
          catchError((e: Error) => {
            console.error('EpochEffects - updateEProjectEpochs$: ', e);
            return [
              new toastActions.ShowToast(
                'warning',
                'Failed To Update Project Epochs',
                '',
              ),
            ];
          }),
        );
    } else {
      return [
        new toastActions.ShowToast(
          'warning',
          'Project Epochs has not been defined. Update failed',
          '',
        ),
      ];
    }
  }
}
