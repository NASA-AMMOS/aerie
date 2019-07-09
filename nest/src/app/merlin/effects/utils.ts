/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { concat, Observable, of } from 'rxjs';
import { LayoutActions } from '../actions';

/**
 * Wraps a list of actions around loading bar show/hide actions.
 * Uses `concat` so the actions happen in the given order (synchronously).
 */
export function withLoadingBar(
  actions: Observable<Action>[],
): Observable<Action> {
  return concat(
    of(LayoutActions.loadingBarShow()),
    ...actions,
    of(LayoutActions.loadingBarHide()),
  );
}
