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

import { StringTMap } from '../models/map';
import { RavenActivityType } from '../models/raven-activity-type';
import { RavenAdaptation } from '../models/raven-adaptation';
import { RavenAdaptationDetail } from '../models/raven-adaptation-detail';
import { AdaptationService } from './adaptation.service';

@Injectable({
  providedIn: 'root',
})
export class AdaptationMockService implements AdaptationService {
  static getMockData(): RavenAdaptation[] {
    const adaptations: RavenAdaptation[] = [
      { id: 'ops', name: 'Ops', version: '1.0.0' },
      { id: 'dev', name: 'Dev', version: '1.0.0' },
      { id: 'test1', name: 'Test 1', version: '1.2.3' },
      { id: 'test2', name: 'Test 2', version: '2.3.4' },
    ];
    return adaptations;
  }

  static getMockActivityTypes(name: string): StringTMap<RavenActivityType> {
    const activityTypes: StringTMap<RavenActivityType> = {};
    const id = name.toLowerCase().replace(/[^\w]/gi, '');
    for (let i = 0, len = 50; i < len; ++i) {
      activityTypes[`${id}${i}`] = AdaptationMockService.getMockActivityType(
        name,
        i,
      );
    }
    return activityTypes;
  }

  static getMockActivityType(name: string, i: number): RavenActivityType {
    const id = name.toLowerCase().replace(/[^\w]/gi, '');
    return {
      id: `${id}${i}`,
      name: `${name} Activity Type ${i}`,
      start: '',
    };
  }

  static getMockAdaptation(id: string): RavenAdaptationDetail | null {
    const adaptation: RavenAdaptation | null =
      AdaptationMockService.getMockData().find(a => a.id === id) || null;

    if (adaptation) {
      return {
        ...adaptation,
        activityTypes: AdaptationMockService.getMockActivityTypes(
          adaptation.name,
        ),
      };
    }

    return null;
  }

  getAdaptation(id: string): Observable<RavenAdaptationDetail> {
    return Observable.create((o: Observer<RavenAdaptationDetail>) => {
      const a = AdaptationMockService.getMockAdaptation(id);

      if (!a) return o.error(new Error('UndefinedAdaptationId'));

      o.next(a);
      o.complete();
    });
  }

  getAdaptations(): Observable<RavenAdaptation[]> {
    return Observable.create((o: Observer<RavenAdaptation[]>) => {
      o.next(AdaptationMockService.getMockData());
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
