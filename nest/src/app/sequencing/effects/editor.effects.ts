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
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { NestConfirmDialogComponent } from '../../shared/components/nest-confirm-dialog/nest-confirm-dialog.component';
import {
  AddText,
  EditorActionTypes,
  OpenEditorHelpDialog,
} from '../actions/editor.actions';
import { SeqEditorService } from '../services/seq-editor.service';

@Injectable()
export class EditorEffects {
  constructor(
    private actions$: Actions,
    private dialog: MatDialog,
    private seqEditorService: SeqEditorService,
  ) {}

  @Effect({ dispatch: false })
  addText$: Observable<Action> = this.actions$.pipe(
    ofType<AddText>(EditorActionTypes.AddText),
    switchMap(action => {
      this.seqEditorService.addText(action.text);
      this.seqEditorService.focusEditor();
      return [];
    }),
  );

  @Effect({ dispatch: false })
  openEditorHelpDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenEditorHelpDialog>(EditorActionTypes.OpenEditorHelpDialog),
    switchMap(() => {
      this.dialog.open(NestConfirmDialogComponent, {
        data: {
          cancelText: 'CLOSE',
          message: 'Help text goes here',
        },
        width: '400px',
      });

      return [];
    }),
  );
}
