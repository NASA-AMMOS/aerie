/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Observable, Observer } from 'rxjs';

import { RavenPlan } from '../models/raven-plan';
import { PlanService } from './plan.service';

@Injectable({
  providedIn: 'root',
})
export class PlanMockService implements PlanService {
  /**
   * Get a list of mock plans
   */
  static getMockData(): RavenPlan[] {
    const plans: RavenPlan[] = [];
    for (let i = 0, len = 50; i < len; ++i) {
      plans.push({
        adaptationId: 'ops',
        end: '1995-12-18T03:28:00',
        id: `plan${i}`,
        name: `Plan ${i}`,
        start: '1995-12-17T03:24:00',
      });
    }
    return plans;
  }

  /**
   * Get an observable list of plans
   */
  getPlans(): Observable<RavenPlan[]> {
    return Observable.create((o: Observer<RavenPlan[]>) => {
      o.next(PlanMockService.getMockData());
      o.complete();
    });
  }

  /**
   * Remove a plan
   */
  removePlan(id: string): Observable<boolean> {
    return Observable.create((o: Observer<boolean>) => {
      o.next(true);
      o.complete();
    });
  }
}
