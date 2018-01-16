/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import {
  MpsServerGraphData,
  MpsServerSource,
} from './../models';

@Injectable()
export class MpsServerApiService {
  constructor(private http: HttpClient) {}

  fetchGraphData(url: string): Observable<MpsServerGraphData> {
    return this.http.get<MpsServerGraphData>(url);
  }

  fetchSources(url: string): Observable<MpsServerSource[]> {
    return this.http.get<MpsServerSource[]>(url);
  }
}
