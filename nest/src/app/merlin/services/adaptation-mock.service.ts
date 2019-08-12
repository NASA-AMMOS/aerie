/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Action } from '@ngrx/store';
import { Observable, Observer } from 'rxjs';
import { ActivityType, Adaptation } from '../../shared/models';
import { AdaptationActions } from '../actions';
import { AdaptationServiceInterface } from './adaptation-service-interface';

export function getMockAdaptations(): Adaptation[] {
  const adaptations: Adaptation[] = [
    {
      id: 'ops',
      location: '',
      mission: '',
      name: 'Ops',
      owner: '',
      version: '1.0.0',
    },
    {
      id: 'dev',
      location: '',
      mission: '',
      name: 'Dev',
      owner: '',
      version: '1.0.0',
    },
    {
      id: 'test1',
      location: '',
      mission: '',
      name: 'Test 1',
      owner: '',
      version: '1.2.3',
    },
    {
      id: 'test2',
      location: '',
      mission: '',
      name: 'Test 2',
      owner: '',
      version: '2.3.4',
    },
  ];
  return adaptations;
}

export function getMockActivityTypes(): ActivityType[] {
  const activityTypes: ActivityType[] = [];

  for (let i = 0, len = 10; i < len; ++i) {
    activityTypes.push({
      name: `some.adaptation.DoSomething.${i}`,
      parameters: [],
      typeName: '',
    });
  }

  return activityTypes;
}

@Injectable({
  providedIn: 'root',
})
export class AdaptationMockService implements AdaptationServiceInterface {
  getActivityTypes(
    planServiceBaseUrl: string,
    adaptationServiceBaseUrl: string,
    planId: string,
  ): Observable<ActivityType[]> {
    return new Observable((o: Observer<ActivityType[]>) => {
      o.next(getMockActivityTypes());
      o.complete();
    });
  }

  getActivityTypesWithActions(
    planServiceBaseUrl: string,
    adaptationServiceBaseUrl: string,
    planId: string,
  ): Observable<Action> {
    return new Observable((o: Observer<Action>) => {
      o.next(
        AdaptationActions.setActivityTypes({
          activityTypes: getMockActivityTypes(),
        }),
      );
      o.complete();
    });
  }

  getAdaptations(baseUrl: string = ''): Observable<Adaptation[]> {
    return new Observable((o: Observer<Adaptation[]>) => {
      o.next(getMockAdaptations());
      o.complete();
    });
  }

  getAdaptationsWithActions(baseUrl: string): Observable<Action> {
    return new Observable((o: Observer<Action>) => {
      o.next(
        AdaptationActions.setAdaptations({ adaptations: getMockAdaptations() }),
      );
      o.complete();
    });
  }
}
