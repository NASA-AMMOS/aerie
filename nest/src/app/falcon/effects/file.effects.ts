/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable, Injector } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { catchError, map, switchMap, withLatestFrom } from 'rxjs/operators';
import { SequenceFile } from '../../../../../sequencing/src/models';
import { ShowToast } from '../../shared/actions/toast.actions';
import {
  FetchChildren,
  FetchChildrenFailure,
  FileActionTypes,
  UpdateChildren,
} from '../actions/file.actions';
import { FalconAppState } from '../falcon-store';
import { FileMockService } from '../services/file-mock.service';
import { FileService } from '../services/file.service';
import { withLoadingBar } from './utils';

@Injectable()
export class FileEffects {
  private fileService: FileService;
  private useMockFileService = true;

  constructor(
    private actions$: Actions,
    private injector: Injector,
    private store$: Store<FalconAppState>,
  ) {
    if (this.useMockFileService) {
      this.fileService = this.injector.get(FileMockService) as FileService;
    } else {
      this.fileService = this.injector.get(FileService);
    }
  }

  @Effect()
  fetchChildren$: Observable<Action> = this.actions$.pipe(
    ofType<FetchChildren>(FileActionTypes.FetchChildren),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    switchMap(({ action, state }) =>
      withLoadingBar([
        this.fileService
          .fetchChildren(
            state.config.app.sequencingServiceBaseUrl,
            action.parentId,
          )
          .pipe(
            map(
              (children: SequenceFile[]) =>
                new UpdateChildren(action.parentId, children, action.options),
            ),
            catchError((e: Error) => {
              console.error('FileEffects - fetchChildren$: ', e);
              return [
                new FetchChildrenFailure(e),
                new ShowToast('error', e.message, 'Fetch Children Failed'),
              ];
            }),
          ),
      ]),
    ),
  );
}
