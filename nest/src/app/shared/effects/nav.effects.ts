/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { tap } from 'rxjs/operators';
import { RouterNavigation } from '../../../../libs/ngrx-router';

@Injectable()
export class NavEffects {
  constructor(private actions: Actions, private titleService: Title) {}

  updateTitle = createEffect(
    () =>
      this.actions.pipe(
        ofType('[router] navigation'),
        tap((action: RouterNavigation) => {
          const title = action.payload.data.title;
          this.titleService.setTitle(title);
        }),
      ),
    { dispatch: false },
  );
}
