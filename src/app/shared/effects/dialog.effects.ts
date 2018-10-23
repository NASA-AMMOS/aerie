/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { exhaustMap, map, withLatestFrom } from 'rxjs/operators';
import { AppState } from '../../app-store';
import { DialogActionTypes, OpenAboutDialog } from '../actions/dialog.actions';
import { NestAboutDialogComponent } from '../components/nest-about-dialog/nest-about-dialog.component';

@Injectable()
export class DialogEffects {
  constructor(
    private actions$: Actions,
    private dialog: MatDialog,
    private store$: Store<AppState>,
  ) {}

  /**
   * Effect for OpenAboutDialog.
   */
  @Effect({ dispatch: false })
  openAboutDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenAboutDialog>(DialogActionTypes.OpenAboutDialog),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    exhaustMap(({ action, state }) => {
      const { branch, commit, version } = state.config.app;

      this.dialog.open(NestAboutDialogComponent, {
        data: {
          modules: state.config.appModules,
          version: `Nest ${version} - ${branch} - ${commit}`,
        },
        width: action.width,
      });

      return [];
    }),
  );
}
