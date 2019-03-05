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
import { ActivityType, Adaptation } from '../../../../../schemas';
import { StringTMap } from '../models';
import { AdaptationServiceInterface } from './adaptation-service-interface';

@Injectable({
  providedIn: 'root',
})
export class AdaptationService implements AdaptationServiceInterface {
  constructor(private http: HttpClient) {}

  getActivityTypes(
    apiBaseUrl: string,
    id: string,
  ): Observable<StringTMap<ActivityType>> {
    return this.http.get<StringTMap<ActivityType>>(
      `${apiBaseUrl}/adaptations/${id}/activities/`,
    );
  }

  getAdaptations(apiBaseUrl: string): Observable<Adaptation[]> {
    return this.http.get<Adaptation[]>(`${apiBaseUrl}/adaptations/`);
  }
}
