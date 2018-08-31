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
import * as mpsServerMocks from '../mocks/mps-server';
import { HBCommand } from '../models/hb-command';
import { HBCommandDictionary } from '../models/hb-command-dictionary';

@Injectable({
  providedIn: 'root',
})
export class MpsServerService {
  /**
   * Get a list of available command dictionaries and populate an Observable
   * with mock/static data until MPS Server has an endpoint for this.
   *
   * @todo When MPS Server gets an endpoint for listing command dictionaries
   * get this list with an http.get request
   *
   * @todo Once the Raven config reducer has been moved to the app level, this
   * should be replaced with something like the following:
   * return this.store.select(fromConfig.getUrls).pipe(
   *  switchMap(config =>
   *    http.get(config.baseUrl) // ...
   *  )
   * )
   * @todo Update spec with HTTP tests once this has been updated
   * https://angular.io/guide/testing#testing-http-services
   */
  getCommandDictionaryList(): Observable<HBCommandDictionary[]> {
    return Observable.create((o: Observer<HBCommandDictionary[]>) => {
      o.next(mpsServerMocks.commandDictionaryList);
      o.complete();
    });
  }

  /**
   * Retrieve a command dictionary by name and populate an Observable with
   * mock/static data until MPS has an endpoint for this.
   *
   * @todo When MPS Server gets an endpoint for fetching command dictionaries
   * get this list with an http.get request
   *
   * @todo Once the Raven config reducer has been moved to the app level, this
   * should be replaced with something like the following:
   * return this.store.select(fromConfig.getUrls).pipe(
   *  switchMap(config =>
   *    http.get(`${config.baseUrl}/${name}`) // ...
   *  )
   * )
   * @todo Update spec with HTTP tests once this has been updated
   * https://angular.io/guide/testing#testing-http-services
   *
   * @param name The name of a command dictionary to retrieve
   */
  getCommandDictionary(name: string): Observable<HBCommand[]> {
    return Observable.create((o: Observer<HBCommand[]>) => {
      o.next(mpsServerMocks.getCommandList(1, name));
      o.complete();
    });
  }
}
