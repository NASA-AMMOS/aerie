import { Injectable } from '@angular/core';
import { Observable, Observer } from 'rxjs';
import {
  activityInstanceId,
  adaptationId,
  cActivityInstanceMap,
  cActivityTypeMap,
  cAdaptationMap,
  cPlan,
  cPlanMap,
  planId,
} from '../mocks';
import {
  CActivityInstanceMap,
  CActivityTypeMap,
  CAdaptationMap,
  CPlan,
  CPlanMap,
  Id,
} from '../types';

@Injectable()
export class ApiMockService {
  getActivityInstances(): Observable<CActivityInstanceMap> {
    return new Observable((o: Observer<CActivityInstanceMap>) => {
      o.next(cActivityInstanceMap);
      o.complete();
    });
  }

  getPlanAndActivityTypes(): Observable<{
    activityTypes: CActivityTypeMap;
    plan: CPlan;
  }> {
    return new Observable(
      (o: Observer<{ activityTypes: CActivityTypeMap; plan: CPlan }>) => {
        o.next({
          activityTypes: cActivityTypeMap,
          plan: cPlan,
        });
        o.complete();
      },
    );
  }

  getAdaptations(): Observable<CAdaptationMap> {
    return new Observable((o: Observer<CAdaptationMap>) => {
      o.next(cAdaptationMap);
      o.complete();
    });
  }

  createActivityInstances(): Observable<string[]> {
    return new Observable((o: Observer<string[]>) => {
      o.next([activityInstanceId]);
      o.complete();
    });
  }

  createAdaptation(): Observable<Id> {
    return new Observable((o: Observer<Id>) => {
      o.next({ id: adaptationId });
      o.complete();
    });
  }

  createPlan(): Observable<Id> {
    return new Observable((o: Observer<Id>) => {
      o.next({ id: planId });
      o.complete();
    });
  }

  deleteActivityInstance(): Observable<{}> {
    return new Observable((o: Observer<{}>) => {
      o.next({});
      o.complete();
    });
  }

  deleteAdaptation(): Observable<{}> {
    return new Observable((o: Observer<{}>) => {
      o.next({});
      o.complete();
    });
  }

  deletePlan(): Observable<{}> {
    return new Observable((o: Observer<{}>) => {
      o.next({});
      o.complete();
    });
  }

  getPlans(): Observable<CPlanMap> {
    return new Observable((o: Observer<CPlanMap>) => {
      o.next(cPlanMap);
      o.complete();
    });
  }

  getPlan(): Observable<CPlan> {
    return new Observable((o: Observer<CPlan>) => {
      o.next(cPlan);
      o.complete();
    });
  }
}
