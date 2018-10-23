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
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs';
import { concatMap, map } from 'rxjs/operators';
import * as stripJsonComments from 'strip-json-comments';
import * as timeCursorActions from '../../raven/actions/time-cursor.actions';
import * as configActions from '../actions/config.actions';

import {
  ConfigActionTypes,
  FetchProjectConfig,
} from '../actions/config.actions';

@Injectable()
export class ConfigEffects {
  constructor(private http: HttpClient, private actions$: Actions) {}

  @Effect()
  fetchProjectConfig$: Observable<Action> = this.actions$.pipe(
    ofType<FetchProjectConfig>(ConfigActionTypes.FetchProjectConfig),
    concatMap(action =>
      this.http.get(action.url, { responseType: 'text' }).pipe(
        map((mpsServerConfig: any) =>
          JSON.parse(stripJsonComments(mpsServerConfig)),
        ),
        concatMap(projectConfig => {
          const actions: Action[] = [];
          Object.keys(projectConfig).forEach(key => {
            if (key === 'itarMessage') {
              actions.push(
                new configActions.UpdateRavenSettings({
                  [key]: projectConfig[key],
                }),
              );
            } else if (
              key === 'showTimeCursor' &&
              projectConfig['showTimeCursor']
            ) {
              actions.push(new timeCursorActions.ShowTimeCursor());
            } else {
              actions.push(
                new configActions.UpdateDefaultBandSettings({
                  [key]: projectConfig[key],
                }),
              );
            }
          });
          return actions;
        }),
      ),
    ),
  );
}
