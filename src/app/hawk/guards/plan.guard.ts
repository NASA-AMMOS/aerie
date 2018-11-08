/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { map, withLatestFrom } from 'rxjs/operators';
import { HawkAppState } from '../hawk-store';

@Injectable({
  providedIn: 'root',
})
export class PlanGuard implements CanActivate {
  constructor(private store$: Store<HawkAppState>) {}

  /**
   * Ensure that a plan has been selected
   */
  canActivate(): Observable<boolean> | Promise<boolean> | boolean {
    return this.store$.pipe(
      withLatestFrom(this.store$),
      map(([, s]) => s.hawk.plan.selectedPlan !== null),
    );
  }
}
