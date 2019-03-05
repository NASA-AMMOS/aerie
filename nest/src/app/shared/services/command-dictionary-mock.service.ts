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
import { Command, CommandDictionary } from '../../../../../schemas';
import { commands } from '../mocks/commands';

export const mockCommandDictionaryList: CommandDictionary[] = [
  {
    id: 'TEST_1',
    name: 'Test 1',
    selected: false,
    version: '1.0.0',
  },
];

@Injectable({
  providedIn: 'root',
})
export class CommandDictionaryMockService {
  getCommandDictionaryList(): Observable<CommandDictionary[]> {
    return Observable.create((o: Observer<CommandDictionary[]>) => {
      o.next(mockCommandDictionaryList);
      o.complete();
    });
  }

  getCommandDictionary(id: string): Observable<Command[]> {
    return Observable.create((o: Observer<Command[]>) => {
      o.next(commands);
      o.complete();
    });
  }
}
