/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';

import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';

import { MatDialog } from '@angular/material';

import {
  Observable,
} from 'rxjs';

import {
  switchMap,
} from 'rxjs/operators';

import {
  RavenConfirmDialogComponent,
} from './../shared/raven/components';

import {
  ConfirmDialogOpen,
  DialogActionTypes,
} from './../actions/dialog';

@Injectable()
export class DialogEffects {
  /**
   * Effect for ConfirmDialogOpen.
   */
  @Effect({ dispatch: false })
  confirmDialogOpen$: Observable<Action> = this.actions$.pipe(
    ofType<ConfirmDialogOpen>(DialogActionTypes.ConfirmDialogOpen),
    switchMap(action => {
      this.dialog.open(RavenConfirmDialogComponent, {
        data: {
          cancelText: action.cancelText,
          message: action.message,
        },
        width: action.width,
      });
      return [];
    },
  ));

  constructor(
    private actions$: Actions,
    private dialog: MatDialog,
  ) {}
}
