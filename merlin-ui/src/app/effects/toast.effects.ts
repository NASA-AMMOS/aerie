import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { ToastrService } from 'ngx-toastr';
import { mergeMap } from 'rxjs/operators';
import { ToastActions } from '../actions';

@Injectable()
export class ToastEffects {
  constructor(private actions: Actions, private toastr: ToastrService) {}

  showToast = createEffect(
    () =>
      this.actions.pipe(
        ofType(ToastActions.showToast),
        mergeMap(action => {
          this.toastr[action.toastType](action.message, '', {
            positionClass: 'toast-bottom-center',
            timeOut: 3500,
          });
          return [];
        }),
      ),
    { dispatch: false },
  );
}
