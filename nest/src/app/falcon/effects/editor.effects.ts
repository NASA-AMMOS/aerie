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
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { switchMap } from 'rxjs/operators';
import { NestConfirmDialogComponent } from '../../shared/components/nest-confirm-dialog/nest-confirm-dialog.component';
import { EditorActions } from '../actions';
import { SeqEditorService } from '../services/seq-editor.service';

@Injectable()
export class EditorEffects {
  constructor(
    private actions: Actions,
    private dialog: MatDialog,
    private seqEditorService: SeqEditorService,
  ) {}

  addText = createEffect(
    () =>
      this.actions.pipe(
        ofType(EditorActions.addText),
        switchMap(action => {
          this.seqEditorService.addText(action.text, action.editorId);
          this.seqEditorService.focusEditor(action.editorId);
          return [];
        }),
      ),
    { dispatch: false },
  );

  openEditorHelpDialog = createEffect(
    () =>
      this.actions.pipe(
        ofType(EditorActions.openEditorHelpDialog),
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
      ),
    { dispatch: false },
  );
}
