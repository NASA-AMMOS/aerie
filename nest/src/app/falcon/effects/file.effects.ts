/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable, Injector } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, switchMap } from 'rxjs/operators';
import { ToastActions } from '../../shared/actions';
import { FileActions } from '../actions';
import { SequenceFile } from '../models';
import { FileMockService } from '../services/file-mock.service';
import { FileService } from '../services/file.service';
import { withLoadingBar } from './utils';

@Injectable()
export class FileEffects {
  private fileService: FileService;
  private useMockFileService = true;

  constructor(private actions: Actions, private injector: Injector) {
    if (this.useMockFileService) {
      this.fileService = this.injector.get(FileMockService) as FileService;
    } else {
      this.fileService = this.injector.get(FileService);
    }
  }

  fetchChildren = createEffect(() =>
    this.actions.pipe(
      ofType(FileActions.fetchChildren),
      switchMap(action =>
        withLoadingBar([
          this.fileService.fetchChildren('', action.parentId).pipe(
            map((children: SequenceFile[]) =>
              FileActions.updateChildren({
                children,
                options: action.options,
                parentId: action.parentId,
              }),
            ),
            catchError((error: Error) => {
              console.error('FileEffects - fetchChildren$: ', error.message);
              return [
                FileActions.fetchChildrenFailure({ error }),
                ToastActions.showToast({
                  message: error.message,
                  title: 'Fetch Children Failed',
                  toastType: 'error',
                }),
              ];
            }),
          ),
        ]),
      ),
    ),
  );
}
