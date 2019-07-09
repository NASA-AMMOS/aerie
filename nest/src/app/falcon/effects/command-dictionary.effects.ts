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
import {
  CommandDictionaryActionTypes,
  FetchCommandDictionaries,
  FetchCommandDictionariesFailure,
  FetchCommandDictionariesSuccess,
  FetchCommandDictionary,
  FetchCommandDictionaryFailure,
  FetchCommandDictionarySuccess,
  SelectCommandDictionary,
} from '../actions/command-dictionary.actions';
import { CommandDictionaryMockService } from '../services/command-dictionary-mock.service';

@Injectable()
export class CommandDictionaryEffects {
  constructor(
    private actions$: Actions,
    private commandDictionaryMockService: CommandDictionaryMockService,
  ) {}

  @Effect()
  fetchCommandDictionaries$: Observable<Action> = this.actions$.pipe(
    ofType<FetchCommandDictionaries>(
      CommandDictionaryActionTypes.FetchCommandDictionaries,
    ),
    concatMap(() =>
      this.commandDictionaryMockService.getCommandDictionaryList().pipe(
        map(data => new FetchCommandDictionariesSuccess(data)),
        catchError((e: Error) => {
          console.error(
            'CommandDictionaryEffect - fetchCommandDictionaries$: ',
            e,
          );
          return of(new FetchCommandDictionariesFailure(e));
        }),
      ),
    ),
  );

  @Effect()
  fetchCommandDictionary$: Observable<Action> = this.actions$.pipe(
    ofType<FetchCommandDictionary>(
      CommandDictionaryActionTypes.FetchCommandDictionary,
    ),
    concatMap(action =>
      this.commandDictionaryMockService.getCommandDictionary(action.name).pipe(
        map(data => new FetchCommandDictionarySuccess(data)),
        catchError((e: Error) => {
          console.error(
            'CommandDictionaryEffect - fetchCommandDictionary$: ',
            e,
          );
          return of(new FetchCommandDictionaryFailure(e));
        }),
      ),
    ),
  );

  @Effect()
  selectCommandDictionary$: Observable<Action> = this.actions$.pipe(
    ofType<SelectCommandDictionary>(
      CommandDictionaryActionTypes.SelectCommandDictionary,
    ),
    map(action => new FetchCommandDictionary(action.selectedId)),
  );
}
