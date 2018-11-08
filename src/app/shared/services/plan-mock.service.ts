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
      parameters: [],
    };
  }

  static getMockActivities(): StringTMap<RavenActivity> {
    return {
      inst1actXYZ: {
        activityTypeId: 'test1',
        duration: '00:20',
        id: 'inst1actXYZ',
        intent: 'What is the sound of one hand clapping?',
        name: 'Instrument 1, Activity ABC',
        sequenceId: 'inst00035.0000.a',
        start: '2022-10-29T14:55:00',
      },
      inst2actXYZ: {
        activityTypeId: 'test2',
        duration: '00:02',
        id: 'inst2actXYZ',
        intent: 'Science intent for this activity is blah blah',
        name: 'Instrument 2, Activity XYZ',
        sequenceId: 'inst00036.0000.b',
        start: '2022-10-29T15:15:00',
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
