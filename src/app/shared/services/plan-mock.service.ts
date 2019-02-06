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
import { PlanServiceInterface } from './plan-service-interface';

import {
  RavenActivity,
  RavenPlan,
  RavenPlanDetail,
  StringTMap,
} from '../models';

export function getMockActivityInstances(): StringTMap<RavenActivity> {
  return {
    SetArrayTrackingMode_25788: {
      activityId: 'SetArrayTrackingMode_25788',
      activityType: 'SetArrayTrackingMode',
      color: '#ffffff',
      constraints: [
        {
          default: 'Some Event',
          locked: false,
          name: 'Latest Start',
          type: 'list',
          value: 'Local Noon',
          values: ['Local Noon', 'Half Past noon', 'Some Future Time'],
        },
        {
          default: 'State Constraint',
          locked: true,
          name: 'Some Enum State',
          type: 'enum',
          value: 'SOME_ENUM_VAL',
          values: [],
        },
        {
          default: 'MOON',
          locked: true,
          name: 'Geometric Constraint',
          type: 'enum',
          value: 'NADIR_SUN',
          values: [],
        },
      ],
      duration: 580,
      end: 1656459714.184,
      endTimestamp: '2022-179T23:41:54.184',
      intent: '...',
      name: 'SetArrayTrackingMode_FIXED',
      parameters: [
        {
          name: 'Image Resolution',
        },
      ],
      start: 1656459134.184,
      startTimestamp: '2022-179T23:32:14.184',
      subActivityIds: [],
      y: null,
    },
    SetArrayTrackingMode_25920: {
      activityId: 'SetArrayTrackingMode_25920',
      activityType: 'SetArrayTrackingMode',
      color: '#ffffff',
      constraints: [
        {
          default: 'At Start',
          locked: false,
          name: 'Time',
          type: 'list',
          value: 'During',
          values: ['At Start', 'During', 'At End'],
        },
        {
          default: 'Some Event',
          locked: false,
          name: 'Earliest Start',
          type: 'list',
          value: 'Local Noon',
          values: ['Local Noon', 'Half Past noon', 'Some Future Time'],
        },
      ],
      duration: 580,
      end: 1656462679.447,
      endTimestamp: '2022-180T00:31:19.447',
      intent: '...',
      name: 'SetArrayTrackingMode_FIXED',
      parameters: [
        {
          name: 'Image Resolution',
        },
      ],
      start: 1656462099.447,
      startTimestamp: '2022-180T00:21:39.447',
      subActivityIds: [],
      y: null,
    },
  };
}

export function getMockPlan(planId: string): RavenPlan {
  return {
    adaptationId: 'ops',
    endTimestamp: '1995-12-18T03:28:00',
    id: planId,
    name: `Plan ${planId}`,
    startTimestamp: '1995-12-17T03:24:00',
  };
}

export function getMockPlans(): RavenPlan[] {
  const plans: RavenPlan[] = [];
  for (let i = 0, len = 10; i < len; ++i) {
    plans.push(getMockPlan(`${i}`));
  }
  return plans;
}

export function getMockPlanDetail(planId: string): RavenPlanDetail {
  return {
    ...getMockPlan(planId),
    activityInstances: getMockActivityInstances(),
  };
}

@Injectable({
  providedIn: 'root',
})
export class PlanMockService implements PlanServiceInterface {
  createActivity(
    apiBaseUrl: string = '',
    planId: string,
    data: RavenActivity,
  ): Observable<RavenActivity> {
    return Observable.create((o: Observer<RavenActivity>) => {
      const instances = getMockActivityInstances();
      const activityId = data.activityId || Date.now().toString();
      const mockDetail = instances[activityId];
      o.next({ ...mockDetail, ...data, activityId });
    });
  }

  createPlan(apiBaseUrl: string, plan: RavenPlan): Observable<RavenPlan> {
    return Observable.create((o: Observer<RavenPlan>) => {
      o.next(plan);
      o.complete();
    });
  }

  deleteActivity(
    apiBaseUrl: string,
    planId: string,
    activityId: string,
  ): Observable<{}> {
    return Observable.create((o: Observer<{}>) => {
      o.next({});
      o.complete();
    });
  }

  deletePlan(apiBaseUrl: string, planId: string): Observable<{}> {
    return Observable.create((o: Observer<{}>) => {
      o.next({});
      o.complete();
    });
  }

  getActivityInstance(
    apiBaseUrl: string = '',
    planId: string = '',
    activityId: string = '',
  ): Observable<RavenActivity> {
    const instances = getMockActivityInstances();
    return Observable.create((o: Observer<RavenActivity>) => {
      o.next(instances[activityId]);
      o.complete();
    });
  }

  getPlanDetail(
    apiBaseUrl: string = '',
    planId: string,
  ): Observable<RavenPlanDetail> {
    return Observable.create((o: Observer<RavenPlanDetail>) => {
      o.next(getMockPlanDetail(planId));
      o.complete();
    });
  }

  getPlans(apiBaseUrl: string = ''): Observable<RavenPlan[]> {
    return Observable.create((o: Observer<RavenPlan[]>) => {
      o.next(getMockPlans());
      o.complete();
    });
  }

  updateActivityInstance(
    apiBaseUrl: string = '',
    planId: string = '',
    activityId: string = '',
    activityInstance: RavenActivity,
  ): Observable<null> {
    return Observable.create((o: Observer<null>) => {
      o.next(null);
      o.complete();
    });
  }
}
