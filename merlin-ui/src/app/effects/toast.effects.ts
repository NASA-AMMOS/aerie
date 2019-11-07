import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { ToastrService } from 'ngx-toastr';
import { mergeMap } from 'rxjs/operators';
import { ToastActions } from '../actions';

const defaultIndividualConfig = {
  positionClass: 'toast-bottom-center',
  timeOut: 3500,
};

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
