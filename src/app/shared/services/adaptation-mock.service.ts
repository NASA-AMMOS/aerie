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

import { RavenAdaptation } from '../models/raven-adaptation';
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
  getAdaptations(): Observable<RavenAdaptation[]> {
    return Observable.create((o: Observer<RavenAdaptation[]>) => {
      o.next(AdaptationMockService.getMockData());
      o.complete();
    });
  }
}
