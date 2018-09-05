/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { Observable, of } from 'rxjs';
import { catchError, concatMap, map } from 'rxjs/operators';
import { MpsServerService } from '../../shared/services/mps-server.service';

import {
  CommandDictionaryActionTypes,
  FetchCommandDictionary,
  FetchCommandDictionaryFailure,
  FetchCommandDictionaryList,
  FetchCommandDictionaryListFailure,
  FetchCommandDictionaryListSuccess,
  FetchCommandDictionarySuccess,
  SelectCommandDictionary,
} from '../actions/command-dictionary.actions';

@Injectable()
export class CommandDictionaryEffects {
  @Effect()
  fetchCommandDictionaryList$: Observable<Action> = this.actions$.pipe(
    ofType<FetchCommandDictionaryList>(
      CommandDictionaryActionTypes.FetchCommandDictionaryList
    ),
    concatMap(action =>
      this.mpsServerService.getCommandDictionaryList().pipe(
        map(data => new FetchCommandDictionaryListSuccess(data)),
        catchError((e: Error) => {
          console.error(
            'CommandDictionaryEffect - fetchCommandDictionaryList$: ',
            e
          );
          return of(new FetchCommandDictionaryListFailure(e));
        })
      )
    )
  );

  @Effect()
  fetchCommandDictionary$: Observable<Action> = this.actions$.pipe(
    ofType<FetchCommandDictionary>(
      CommandDictionaryActionTypes.FetchCommandDictionary
    ),
    concatMap(action =>
      this.mpsServerService.getCommandDictionary(action.name).pipe(
        map(data => new FetchCommandDictionarySuccess(data)),
        catchError((e: Error) => {
          console.error(
            'CommandDictionaryEffect - fetchCommandDictionary$: ',
            e
          );
          return of(new FetchCommandDictionaryFailure(e));
        })
      )
    )
  );

  /**
   * Dispatch the FetchCommandAction whenever a new selection has been made
   */
  @Effect()
  selectCommandDictionary$: Observable<Action> = this.actions$.pipe(
    ofType<SelectCommandDictionary>(
      CommandDictionaryActionTypes.SelectCommandDictionary
    ),
    map(action => new FetchCommandDictionary(action.selectedId))
  );

  constructor(
    private actions$: Actions,
    private mpsServerService: MpsServerService
  ) {}
}
