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
import { europaCommands, mpsCommands } from '../mocks';

export const mockCommandDictionaryList: any[] = [
  {
    id: 'TEST_1',
    name: 'Test 1',
    version: '1.0.0',
  },
  {
    id: 'EUROPA_CLIPPER_1',
    name: 'Europa Clipper',
    version: '1.0.0',
  },
];

@Injectable({
  providedIn: 'root',
})
export class CommandDictionaryMockService {
  getCommandDictionaryList(): Observable<any[]> {
    return new Observable((o: Observer<any[]>) => {
      o.next(mockCommandDictionaryList);
      o.complete();
    });
  }

  getCommandDictionary(id: string): Observable<any[]> {
    return new Observable((o: Observer<any[]>) => {
      switch (id) {
        case 'TEST_1':
          o.next(mpsCommands);
          break;
        case 'EUROPA_CLIPPER_1':
          o.next(europaCommands);
          break;
        default:
          o.next(mpsCommands);
          break;
      }
      o.complete();
    });
  }
}
