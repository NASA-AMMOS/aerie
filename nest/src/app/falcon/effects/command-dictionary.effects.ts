/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, concatMap, map } from 'rxjs/operators';
import { CommandDictionaryActions } from '../actions';
import { CommandDictionaryMockService } from '../services/command-dictionary-mock.service';

@Injectable()
export class CommandDictionaryEffects {
  constructor(
    private actions: Actions,
    private commandDictionaryMockService: CommandDictionaryMockService,
  ) {}

  fetchCommandDictionaries = createEffect(() =>
    this.actions.pipe(
      ofType(CommandDictionaryActions.fetchCommandDictionaries),
      concatMap(() =>
        this.commandDictionaryMockService.getCommandDictionaryList().pipe(
          map(data =>
            CommandDictionaryActions.fetchCommandDictionariesSuccess({ data }),
          ),
          catchError((error: Error) => {
            console.error(
              'CommandDictionaryEffect - fetchCommandDictionaries: ',
              error.message,
            );
            return of(
              CommandDictionaryActions.fetchCommandDictionariesFailure({
                error,
              }),
            );
          }),
        ),
      ),
    ),
  );

  fetchCommandDictionary = createEffect(() =>
    this.actions.pipe(
      ofType(CommandDictionaryActions.fetchCommandDictionary),
      concatMap(({ name }) =>
        this.commandDictionaryMockService.getCommandDictionary(name).pipe(
          map(data =>
            CommandDictionaryActions.fetchCommandDictionarySuccess({ data }),
          ),
          catchError((error: Error) => {
            console.error(
              'CommandDictionaryEffect - fetchCommandDictionary: ',
              error.message,
            );
            return of(
              CommandDictionaryActions.fetchCommandDictionaryFailure({
                error,
              }),
            );
          }),
        ),
      ),
    ),
  );

  selectCommandDictionary = createEffect(() =>
    this.actions.pipe(
      ofType(CommandDictionaryActions.selectCommandDictionary),
      map(({ selectedId }) =>
        CommandDictionaryActions.fetchCommandDictionary({ name: selectedId }),
      ),
    ),
  );
}
