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
import { PlanService } from './plan.service';

import {
  RavenActivity,
  RavenActivityDetail,
  RavenPlan,
  RavenPlanDetail,
  StringTMap,
} from '../models';

@Injectable({
  providedIn: 'root',
})
export class PlanMockService implements PlanService {
  static getMockActivityDetail(id: string): RavenActivityDetail {
    const activities = PlanMockService.getMockActivities();

    return {
      ...activities[id],
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
      parameters: [
        {
          name: 'Image Resolution',
        },
      ],
      subActivityIds: [],
    };
  }

  static getMockActivities(): StringTMap<RavenActivity> {
    return {
      SetArrayTrackingMode_25788: {
        activityTypeId: 'SetArrayTrackingMode',
        color: '#ffffff',
        duration: 580,
        end: 1656459714.184,
        endTimestamp: '2022-179T23:41:54.184',
        id: 'SetArrayTrackingMode_25788',
        intent: '...',
        name: 'SetArrayTrackingMode_FIXED',
        sequenceId: 'inst00035.0000.a',
        start: 1656459134.184,
        startTimestamp: '2022-179T23:32:14.184',
        y: null,
      },
      SetArrayTrackingMode_25920: {
        activityTypeId: 'SetArrayTrackingMode',
        color: '#ffffff',
        duration: 580,
        end: 1656462679.447,
        endTimestamp: '2022-180T00:31:19.447',
        id: 'SetArrayTrackingMode_25920',
        intent: '...',
        name: 'SetArrayTrackingMode_FIXED',
        sequenceId: 'inst00036.0000.b',
        start: 1656462099.447,
        startTimestamp: '2022-180T00:21:39.447',
        y: null,
      },
    };
  }

  static getMockPlan(adaptationId: string, id: string): RavenPlan {
    return {
      adaptationId,
      end: '1995-12-18T03:28:00',
      id,
      name: `Plan ${id}`,
      start: '1995-12-17T03:24:00',
    };
  }

  static getMockPlans(): RavenPlan[] {
    const plans: RavenPlan[] = [];
    const adaptationIds = ['ops', 'dev', 'test1', 'test2'];
    for (let i = 0, len = 50; i < len; ++i) {
      plans.push(PlanMockService.getMockPlan(adaptationIds[i % 4], `${i}`));
    }
    return plans;
  }

  static getMockPlanDetail(adaptationId: string, id: string): RavenPlanDetail {
    return {
      ...PlanMockService.getMockPlan(adaptationId, id),
      activities: PlanMockService.getMockActivities(),
    };
  }

  getActivityDetail(id: string): Observable<RavenActivityDetail> {
    return Observable.create((o: Observer<RavenActivityDetail>) => {
      o.next(PlanMockService.getMockActivityDetail(id));
      o.complete();
    });
  }

  getPlanDetail(adaptationId: string, id: string): Observable<RavenPlanDetail> {
    return Observable.create((o: Observer<RavenPlanDetail>) => {
      o.next(PlanMockService.getMockPlanDetail(adaptationId, id));
      o.complete();
    });
  }

  getPlans(): Observable<RavenPlan[]> {
    return Observable.create((o: Observer<RavenPlan[]>) => {
      o.next(PlanMockService.getMockPlans());
      o.complete();
    });
  }

  removePlan(id: string): Observable<boolean> {
    return Observable.create((o: Observer<boolean>) => {
      o.next(true);
      o.complete();
    });
  }

  saveActivity(
    data: RavenActivity | RavenActivityDetail,
  ): Observable<RavenActivityDetail> {
    return Observable.create((o: Observer<RavenActivityDetail>) => {
      const id = data.id || Date.now().toString();
      const mockDetail = PlanMockService.getMockActivityDetail(id);
      o.next({ ...mockDetail, ...data, id });
    });
  }
}
