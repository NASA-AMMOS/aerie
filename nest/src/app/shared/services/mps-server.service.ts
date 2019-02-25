/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map } from 'rxjs/operators';
import { MpsServerSource, RavenSource, StringTMap } from '../models';
import { importState, toRavenSources } from '../util';

@Injectable({
  providedIn: 'root',
})
export class MpsServerService {
  constructor(private http: HttpClient) {}

  /**
   * etches sources from MPS Server and maps them to Raven sources.
   */
  fetchNewSources(
    url: string,
    parentId: string,
    isServer: boolean,
    tree: StringTMap<RavenSource> | null,
  ) {
    return this.http
      .get<MpsServerSource[]>(url)
      .pipe(
        map((mpsServerSources: MpsServerSource[]) =>
          toRavenSources(parentId, isServer, mpsServerSources, tree),
        ),
      );
  }

  /**
   * Fetches saved state from MPS Server.
   * Imports state after fetching.
   */
  fetchState(url: string) {
    return this.http.get(url).pipe(map(res => importState(res[0])));
  }

  /**
   * Import mapping file into MPS Server for a given source URL.
   */
  importMappingFile(sourceUrl: string, name: string, mapping: string) {
    const url = sourceUrl.replace('fs-mongodb', 'metadata-mongodb');
    return this.http.post(`${url}/${name}`, mapping, { responseType: 'text' });
  }

  /**
   * Deletes a source from MPS Server.
   */
  removeSource(sourceUrl: string, sourceId: string) {
    const url = sourceUrl.replace(/list_.*-mongodb/i, 'fs-mongodb');
    return this.http.delete(url, { responseType: 'text' });
  }

  /**
   * Save state to an MPS Server source.
   * state is in exported form.
   */
  saveState(sourceUrl: string, name: string, state: any) {
    return this.http.put(`${sourceUrl}/${name}?timeline_type=state`, state);
  }

  /**
   * Update state to an MPS Server source.
   * state is in exported form.
   */
  updateState(stateUrl: string, state: any) {
    return this.http.put(`${stateUrl}?timeline_type=state`, state);
  }
}
