/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { ToastrService } from 'ngx-toastr';
import { mergeMap } from 'rxjs/operators';
import { ToastActions } from '../actions';

// TODO: Provide individual options: https://www.npmjs.com/package/ngx-toastr
const defaultIndividualConfig = {};

@Injectable()
export class ToastEffects {
  constructor(private actions: Actions, private toastr: ToastrService) {}

  showToast = createEffect(
    () =>
      this.actions.pipe(
        ofType(ToastActions.showToast),
        mergeMap(action => {
          this.toastr[action.toastType](action.message, action.title, {
            ...action.config,
            ...defaultIndividualConfig,
          });
          return [];
        }),
      ),
    { dispatch: false },
  );
}
