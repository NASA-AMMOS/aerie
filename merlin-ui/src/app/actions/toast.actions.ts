import { createAction, props } from '@ngrx/store';

export const showToast = createAction(
  '[toast] showToast',
  props<{ toastType: string; message: string }>(),
);
