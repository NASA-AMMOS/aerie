/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { Observable } from 'rxjs';
import { ActivityInstance, Plan } from '../../shared/models';

export interface PlanServiceInterface {
  createActivity(
    baseUrl: string,
    planId: string,
    data: ActivityInstance,
  ): Observable<ActivityInstance[]>;
  createPlan(baseUrl: string, plan: Plan): Observable<Plan>;
  deleteActivity(
    baseUrl: string,
    planId: string,
    activityId: string,
  ): Observable<{}>;
  deletePlan(baseUrl: string, planId: string): Observable<{}>;
  getActivities(
    baseUrl: string,
    planId: string,
  ): Observable<ActivityInstance[]>;
  getActivitiesWithActions(
    baseUrl: string,
    planId: string,
    activityId: string | null,
  ): Observable<Action>;
  getPlans(baseUrl: string): Observable<Plan[]>;
  getPlansWithActions(
    baseUrl: string,
    planId: string | null,
  ): Observable<Action>;
  updateActivity(
    baseUrl: string,
    planId: string,
    activityId: string,
    activityInstance: ActivityInstance,
  ): Observable<null>;
}
