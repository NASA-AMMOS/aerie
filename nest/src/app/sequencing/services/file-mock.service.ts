/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import keyBy from 'lodash-es/keyBy';
import { Observable, Observer } from 'rxjs';
import { SequenceFile } from '../../../../../sequencing/src/models';
import * as mocks from '../../../../../sequencing/test/mocks';
import { FileServiceInterface } from './file-service-interface';

const mockFiles = mocks.getFiles();
const mockFilesById = keyBy(mockFiles, 'id');

export function getChildren(fileId: string): SequenceFile[] {
  const { childIds = [] } = mockFilesById[fileId];
  const children = childIds.reduce((files: SequenceFile[], id: string) => {
    const file = mockFilesById[id];
    if (file) {
      files.push(file);
    }
    return files;
  }, []);
  return children;
}

@Injectable({
  providedIn: 'root',
})
export class FileMockService implements FileServiceInterface {
  fetchChildren(
    baseUrl: string = '',
    fileId: string,
  ): Observable<SequenceFile[]> {
    return new Observable((o: Observer<SequenceFile[]>) => {
      const children = getChildren(fileId);
      o.next(children);
      o.complete();
    });
  }
}
