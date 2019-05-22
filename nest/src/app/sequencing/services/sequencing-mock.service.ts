/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { keyBy } from 'lodash';
import { Observable, Observer } from 'rxjs';
import { SequenceFile } from '../../../../../sequencing/src/models';
import * as mocks from '../../../../../sequencing/test/mocks';
import { SequencingServiceInterface } from './sequencing-service-interface';

const mockFiles = mocks.getFiles();
const mockFilesById = keyBy(mockFiles, 'id');

@Injectable({
  providedIn: 'root',
})
export class SequencingMockService implements SequencingServiceInterface {
  readChildren(
    baseUrl: string = '',
    fileId: string,
  ): Observable<SequenceFile[]> {
    return Observable.create((o: Observer<SequenceFile[]>) => {
      const { childIds = [] } = mockFilesById[fileId];
      const children = childIds.reduce((files: SequenceFile[], id: string) => {
        const file = mockFilesById[id];
        if (file) {
          files.push(file);
        }
        return files;
      }, []);
      o.next(children);
    });
  }
}
