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

import { RavenActivityType } from '../models/raven-activity-type';
import { ActivityTypeService } from './activity-type.service';

@Injectable({
  providedIn: 'root',
})
export class ActivityTypeMockService implements ActivityTypeService {
  static getMockData(): RavenActivityType[] {
    const activityTypes: RavenActivityType[] = [];
    for (let i = 0, len = 50; i < len; ++i) {
      activityTypes.push({
        id: `test${i}`,
        name: `Test ${i}`,
        start: '',
      });
    }
    return activityTypes;
  }

  getActivityTypes(): Observable<RavenActivityType[]> {
    return Observable.create((o: Observer<RavenActivityType[]>) => {
      o.next(ActivityTypeMockService.getMockData());
      o.complete();
    });
  }

  removeActivityType(id: string): Observable<boolean> {
    return Observable.create((o: Observer<boolean>) => {
      o.next(true);
      o.complete();
    });
  }
}
