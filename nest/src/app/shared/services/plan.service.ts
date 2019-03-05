/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ActivityInstance, Plan } from '../../../../../schemas';
import { PlanServiceInterface } from './plan-service-interface';

@Injectable({
  providedIn: 'root',
})
export class PlanService implements PlanServiceInterface {
  constructor(private http: HttpClient) {}

  createActivity(
    apiBaseUrl: string,
    planId: string,
    data: ActivityInstance,
  ): Observable<ActivityInstance> {
    return this.http.post<ActivityInstance>(
      `${apiBaseUrl}/plans/${planId}/activity_instances/`,
      data,
    );
  }

  createPlan(apiBaseUrl: string, plan: Plan): Observable<Plan> {
    return this.http.post<Plan>(`${apiBaseUrl}/plans/`, plan);
  }

  deleteActivity(
    apiBaseUrl: string,
    planId: string,
    activityId: string,
  ): Observable<{}> {
    return this.http.delete(
      `${apiBaseUrl}/plans/${planId}/activity_instances/${activityId}/`,
    );
  }

  deletePlan(apiBaseUrl: string, planId: string): Observable<{}> {
    return this.http.delete(`${apiBaseUrl}/plans/${planId}/`);
  }

  getActivities(
    apiBaseUrl: string,
    planId: string,
  ): Observable<ActivityInstance[]> {
    return this.http.get<ActivityInstance[]>(
      `${apiBaseUrl}/plans/${planId}/activity_instances/`,
    );
  }

  getPlans(apiBaseUrl: string): Observable<Plan[]> {
    return this.http.get<any[]>(`${apiBaseUrl}/plans/`).pipe(
      map(plans =>
        // Map _id to id.
        // TODO: Do this on the server.
        plans.map(plan => ({
          ...plan,
          id: plan._id,
        })),
      ),
    );
  }

  updateActivity(
    apiBaseUrl: string,
    planId: string,
    activityId: string,
    activityInstance: ActivityInstance,
  ): Observable<null> {
    return this.http.patch<null>(
      `${apiBaseUrl}/plans/${planId}/activity_instances/${activityId}/`,
      activityInstance,
    );
  }
}
