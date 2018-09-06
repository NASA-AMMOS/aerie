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
import { ToastrService } from 'ngx-toastr';
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { ShowToast, ToastActionTypes } from '../actions/toast.actions';

@Injectable()
export class ToastEffects {
  @Effect({ dispatch: false })
  showToast$: Observable<Action> = this.actions$.pipe(
    ofType<ShowToast>(ToastActionTypes.ShowToast),
    mergeMap(action => {
      this.toastr[action.toastType](
        action.message,
        action.title,
        action.config,
      );
      return [];
    }),
  );

  constructor(private actions$: Actions, private toastr: ToastrService) {}
}
