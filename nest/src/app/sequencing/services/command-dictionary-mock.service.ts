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
import { CommandDictionary, MpsCommand } from '../../shared/models';
import { mpsCommands } from '../mocks/mps-commands';

export const mockCommandDictionaryList: CommandDictionary[] = [
  {
    id: 'TEST_1',
    name: 'Test 1',
    version: '1.0.0',
  },
];

@Injectable({
  providedIn: 'root',
})
export class CommandDictionaryMockService {
  getCommandDictionaryList(): Observable<CommandDictionary[]> {
    return new Observable((o: Observer<CommandDictionary[]>) => {
      o.next(mockCommandDictionaryList);
      o.complete();
    });
  }

  getCommandDictionary(id: string): Observable<MpsCommand[]> {
    return new Observable((o: Observer<MpsCommand[]>) => {
      o.next(mpsCommands);
      o.complete();
    });
  }
}
