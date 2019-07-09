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
import { ActivityType, Adaptation } from '../../shared/models';

export interface AdaptationServiceInterface {
  getActivityTypes(
    planServiceBaseUrl: string,
    adaptationServiceBaseUrl: string,
    planId: string,
  ): Observable<ActivityType[]>;
  getActivityTypesWithActions(
    planServiceBaseUrl: string,
    adaptationServiceBaseUrl: string,
    planId: string,
  ): Observable<Action>;
  getAdaptations(baseUrl: string): Observable<Adaptation[]>;
  getAdaptationsWithActions(baseUrl: string): Observable<Action>;
}
