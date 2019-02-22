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
import { Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import {
  LayoutActionTypes,
  Resize,
  ToggleActivityTypesDrawer,
  ToggleEditActivityDrawer,
} from '../actions/layout.actions';

@Injectable()
export class LayoutEffects {
  constructor(private actions$: Actions) {}

  @Effect({ dispatch: false })
  resize$: Observable<Action> = this.actions$.pipe(
    ofType<Resize>(LayoutActionTypes.Resize),
    switchMap(action => {
      setTimeout(() => dispatchEvent(new Event('resize')), action.timeout || 0);
      return [];
    }),
  );

  @Effect()
  toggleDrawer$: Observable<Action> = this.actions$.pipe(
    ofType<ToggleActivityTypesDrawer | ToggleEditActivityDrawer>(
      LayoutActionTypes.ToggleActivityTypesDrawer,
      LayoutActionTypes.ToggleEditActivityDrawer,
    ),
    map(_ => new Resize()),
  );
}
