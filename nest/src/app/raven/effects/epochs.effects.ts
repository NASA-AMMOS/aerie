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
import { Store } from '@ngrx/store';
import { concat, of } from 'rxjs';
import { catchError, concatMap, map, withLatestFrom } from 'rxjs/operators';
import { EpochsActions, SourceExplorerActions, ToastActions } from '../actions';
import { MpsServerEpoch, RavenEpoch } from '../models';
import { RavenAppState } from '../raven-store';
import { toRavenEpochs } from '../util';

@Injectable()
export class EpochsEffects {
  constructor(
    private actions: Actions,
    private http: HttpClient,
    private store: Store<RavenAppState>,
  ) {}

  fetchEpochs = createEffect(() =>
    this.actions.pipe(
      ofType(EpochsActions.fetchEpochs),
      concatMap(action =>
        concat(
          of(
            SourceExplorerActions.updateSourceExplorer({
              update: { fetchPending: true },
            }),
          ),
          this.http.get(action.url).pipe(
            map((mpsServerEpochs: MpsServerEpoch[]) =>
              toRavenEpochs(mpsServerEpochs),
            ),
            map((epochs: RavenEpoch[]) =>
              action.replaceAction === 'AppendAndReplace'
                ? EpochsActions.appendAndReplaceEpochs({ epochs })
                : EpochsActions.addEpochs({ epochs }),
            ),
          ),
          of(
            SourceExplorerActions.updateSourceExplorer({
              update: { fetchPending: false },
            }),
          ),
        ),
      ),
      catchError((e: Error) => {
        console.error('EpochsEffects - fetchEpochs: ', e.message);
        return [
          ToastActions.showToast({
            message: 'Failed to fetch epochs',
            title: '',
            toastType: 'warning',
          }),
        ];
      }),
    ),
  );

  saveNewEpochFile = createEffect(() =>
    this.actions.pipe(
      ofType(EpochsActions.saveNewEpochFile),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      concatMap(({ action, state }) => {
        const headers = new HttpHeaders().set(
          'Content-Type',
          'application/json',
        );
        const responseType = 'text';
        const url = `${state.config.app.baseUrl}/${state.config.mpsServer.apiUrl}/${action.filePathName}?timeline_type=epoch`;

        return this.http
          .put(url, JSON.stringify(state.raven.epochs.epochs), {
            headers,
            responseType,
          })
          .pipe(
            concatMap(() => {
              return of(EpochsActions.saveNewEpochFileSuccess());
            }),
            catchError((e: Error) => {
              console.error('EpochEffects - saveNewEpochFile: ', e.message);
              return [
                ToastActions.showToast({
                  message: 'Failed To Save New Epoch File',
                  title: '',
                  toastType: 'warning',
                }),
              ];
            }),
          );
      }),
    ),
  );

  updateProjectEpochs = createEffect(() =>
    this.actions.pipe(
      ofType(EpochsActions.updateProjectEpochs),
      withLatestFrom(this.store),
      map(([, state]) => ({ state })),
      concatMap(({ state }) => {
        if (
          state.config.mpsServer.epochsUrl &&
          state.config.mpsServer.epochsUrl.length > 0
        ) {
          const headers = new HttpHeaders().set(
            'Content-Type',
            `application/json`,
          );
          const responseType = 'text';
          const url = `${state.config.app.baseUrl}/${state.config.mpsServer.epochsUrl}?timeline_type=epoch`;

          return this.http
            .put(url, JSON.stringify(state.raven.epochs.epochs), {
              headers,
              responseType,
            })
            .pipe(
              concatMap(() => {
                return of(EpochsActions.updateProjectEpochsSuccess());
              }),
              catchError((e: Error) => {
                console.error(
                  'EpochEffects - updateEProjectEpochs: ',
                  e.message,
                );
                return [
                  ToastActions.showToast({
                    message: 'Failed To Update Project Epochs',
                    title: '',
                    toastType: 'warning',
                  }),
                ];
              }),
            );
        } else {
          return [
            ToastActions.showToast({
              message: 'Project Epochs has not been defined. Update failed.',
              title: '',
              toastType: 'warning',
            }),
          ];
        }
      }),
    ),
  );
}
