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
import { map, tap } from 'rxjs/operators';
import { LayoutActions } from '../actions';

@Injectable()
export class LayoutEffects {
  constructor(private actions: Actions) {}

  resize = createEffect(
    () =>
      this.actions.pipe(
        ofType(LayoutActions.resize),
        tap(({ timeout }) => {
          setTimeout(() => dispatchEvent(new Event('resize')), timeout || 0);
        }),
      ),
    { dispatch: false },
  );

  toggleDrawer = createEffect(() =>
    this.actions.pipe(
      ofType(
        LayoutActions.toggleActivityTypesDrawer,
        LayoutActions.toggleCreatePlanDrawer,
        LayoutActions.toggleEditActivityDrawer,
        LayoutActions.toggleCreatePlanDrawer,
      ),
      map(_ => LayoutActions.resize({})),
    ),
  );
}
