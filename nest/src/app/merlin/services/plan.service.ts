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
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ToastActions } from '../../shared/actions';
import { ActivityInstance, Plan } from '../../shared/models';
import { PlanActions } from '../actions';
import { PlanServiceInterface } from './plan-service-interface';

@Injectable({
  providedIn: 'root',
})
export class PlanService implements PlanServiceInterface {
  constructor(private http: HttpClient) {}

  createActivity(
    baseUrl: string,
    planId: string,
    data: ActivityInstance,
  ): Observable<string[]> {
    return this.http.post<string[]>(
      `${baseUrl}/plans/${planId}/activity_instances/`,
      [data],
    );
  }

  createPlan(baseUrl: string, plan: Plan): Observable<Plan> {
    return this.http.post<Plan>(`${baseUrl}/plans/`, plan);
  }

  deleteActivity(
    baseUrl: string,
    planId: string,
    activityId: string,
  ): Observable<{}> {
    return this.http.delete(
      `${baseUrl}/plans/${planId}/activity_instances/${activityId}/`,
    );
  }

  deletePlan(baseUrl: string, planId: string): Observable<{}> {
    return this.http.delete(`${baseUrl}/plans/${planId}/`);
  }

  getActivities(
    baseUrl: string,
    planId: string,
  ): Observable<ActivityInstance[]> {
    return this.http.get<ActivityInstance[]>(
      `${baseUrl}/plans/${planId}/activity_instances/`,
    );
  }

  getActivitiesWithActions(
    baseUrl: string,
    planId: string,
    activityId: string | null,
  ): Observable<Action> {
    return this.getActivities(baseUrl, planId).pipe(
      map(activities =>
        activityId
          ? PlanActions.setActivities({
              activities,
              activityId,
            })
          : PlanActions.setActivities({ activities }),
      ),
      catchError(error => [
        PlanActions.fetchActivitiesFailure({ error: new Error(error) }),
        ToastActions.showToast({
          message: error.message,
          title: 'Fetching Activities Failed',
          toastType: 'error',
        }),
      ]),
    );
  }

  getPlans(baseUrl: string): Observable<Plan[]> {
    return this.http.get<Plan[]>(`${baseUrl}/plans/`);
  }

  getPlansWithActions(
    baseUrl: string,
    planId: string | null,
  ): Observable<Action> {
    return this.getPlans(baseUrl).pipe(
      map(
        plans =>
          planId
            ? PlanActions.setPlansAndSelectedPlan({ plans, planId })
            : PlanActions.setPlans({ plans }),
        catchError(error => [
          PlanActions.fetchPlansFailure({ error: new Error(error) }),
          ToastActions.showToast({
            message: error.message,
            title: 'Fetching Plans Failed',
            toastType: 'error',
          }),
        ]),
      ),
    );
  }

  updateActivity(
    baseUrl: string,
    planId: string,
    activityId: string,
    activityInstance: ActivityInstance,
  ): Observable<null> {
    return this.http.patch<null>(
      `${baseUrl}/plans/${planId}/activity_instances/${activityId}/`,
      activityInstance,
    );
  }
}
