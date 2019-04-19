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
import { ShowToast } from '../../shared/actions/toast.actions';
import { ActivityInstance, Plan } from '../../shared/models';
import {
  FetchActivitiesFailure,
  FetchPlansFailure,
  SetActivities,
  SetActivitiesAndSelectedActivity,
  SetPlans,
  SetPlansAndSelectedPlan,
} from '../actions/plan.actions';
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
  ): Observable<ActivityInstance> {
    return this.http.post<ActivityInstance>(
      `${baseUrl}/plans/${planId}/activity_instances/`,
      data,
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
      map(
        activities =>
          activityId
            ? new SetActivitiesAndSelectedActivity(activities, activityId)
            : new SetActivities(activities),
      ),
      catchError(error => [
        new FetchActivitiesFailure(new Error(error)),
        new ShowToast('error', error.message, 'Fetching Activities Failed'),
      ]),
    );
  }

  getPlans(baseUrl: string): Observable<Plan[]> {
    return this.http.get<Plan[]>(`${baseUrl}/plans/`).pipe(
      map(plans =>
        // Map _id to id.
        // TODO: Do this on the server.
        plans.map(plan => ({
          ...plan,
          id: (plan as any)._id,
        })),
      ),
    );
  }

  getPlansWithActions(
    baseUrl: string,
    planId: string | null,
  ): Observable<Action> {
    return this.getPlans(baseUrl).pipe(
      map(
        plans =>
          planId
            ? new SetPlansAndSelectedPlan(plans, planId)
            : new SetPlans(plans),
        catchError(error => [
          new FetchPlansFailure(new Error(error)),
          new ShowToast('error', error.message, 'Fetching Plans Failed'),
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
