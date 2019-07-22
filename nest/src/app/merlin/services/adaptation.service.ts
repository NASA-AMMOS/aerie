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
import { catchError, map, switchMap } from 'rxjs/operators';
import { ToastActions } from '../../shared/actions';
import { ActivityType, Adaptation, Plan } from '../../shared/models';
import { AdaptationActions } from '../actions';
import { AdaptationServiceInterface } from './adaptation-service-interface';

@Injectable({
  providedIn: 'root',
})
export class AdaptationService implements AdaptationServiceInterface {
  constructor(private http: HttpClient) {}

  /**
   * Get a list of activity types for a given plan id.
   * We are doing two requests here:
   * 1. Get the plan so we can get the corresponding adaptation id.
   * 2. Get the activity types for the given adaptation id.
   * @todo Build this logic in the plan service so we only have to do a single request.
   */
  getActivityTypes(
    planServiceBaseUrl: string,
    adaptationServiceBaseUrl: string,
    planId: string,
  ): Observable<ActivityType[]> {
    return this.http
      .get<Plan>(`${planServiceBaseUrl}/plans/${planId}/`)
      .pipe(
        switchMap((plan: Plan) =>
          this.http.get<ActivityType[]>(
            `${adaptationServiceBaseUrl}/adaptations/${plan.adaptationId}/activities/`,
          ),
        ),
      );
  }

  getActivityTypesWithActions(
    planServiceBaseUrl: string,
    adaptationServiceBaseUrl: string,
    planId: string,
  ): Observable<Action> {
    return this.getActivityTypes(
      planServiceBaseUrl,
      adaptationServiceBaseUrl,
      planId,
    ).pipe(
      map(
        activityTypes => AdaptationActions.setActivityTypes({ activityTypes }),
        catchError(error => [
          AdaptationActions.fetchActivityTypesFailure({
            error: new Error(error),
          }),
          ToastActions.showToast({
            message: error.message,
            title: 'Fetching Activity Types Failed',
            toastType: 'error',
          }),
        ]),
      ),
    );
  }

  getAdaptations(baseUrl: string): Observable<Adaptation[]> {
    return this.http.get<Adaptation[]>(`${baseUrl}/adaptations/`);
  }

  getAdaptationsWithActions(baseUrl: string): Observable<Action> {
    return this.getAdaptations(baseUrl).pipe(
      map(
        adaptations => AdaptationActions.setAdaptations({ adaptations }),
        catchError(error => [
          AdaptationActions.fetchAdaptationsFailure({
            error: new Error(error),
          }),
          ToastActions.showToast({
            message: error.message,
            title: 'Fetching Adaptations Failed',
            toastType: 'error',
          }),
        ]),
      ),
    );
  }
}
