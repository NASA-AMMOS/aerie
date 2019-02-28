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
import { Action, Store } from '@ngrx/store';
import { Observable, of } from 'rxjs';
import { catchError, concatMap, map, withLatestFrom } from 'rxjs/operators';
import { CommandDictionaryMockService } from '../../shared/services/command-dictionary-mock.service';
import {
  CommandDictionaryActionTypes,
  FetchCommandDictionary,
  FetchCommandDictionaryFailure,
  FetchCommandDictionaryList,
  FetchCommandDictionaryListFailure,
  FetchCommandDictionaryListSuccess,
  FetchCommandDictionarySuccess,
  SelectCommand,
  SelectCommandDictionary,
} from '../actions/command-dictionary.actions';
import { SetText } from '../actions/editor.actions';
import { SequencingAppState } from '../sequencing-store';

@Injectable()
export class CommandDictionaryEffects {
  constructor(
    private actions$: Actions,
    private store$: Store<SequencingAppState>,
    private commandDictionaryMockService: CommandDictionaryMockService,
  ) {}

  @Effect()
  fetchCommandDictionaryList$: Observable<Action> = this.actions$.pipe(
    ofType<FetchCommandDictionaryList>(
      CommandDictionaryActionTypes.FetchCommandDictionaryList,
    ),
    concatMap(() =>
      this.commandDictionaryMockService.getCommandDictionaryList().pipe(
        map(data => new FetchCommandDictionaryListSuccess(data)),
        catchError((e: Error) => {
          console.error(
            'CommandDictionaryEffect - fetchCommandDictionaryList$: ',
            e,
          );
          return of(new FetchCommandDictionaryListFailure(e));
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
  selectCommand$: Observable<Action> = this.actions$.pipe(
    ofType<SelectCommand>(CommandDictionaryActionTypes.SelectCommand),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    concatMap(({ action, state }) => {
      const { commandsByName } = state.sequencing.commandDictionary;
      const { line, text } = state.sequencing.editor;

      if (commandsByName) {
        const command = commandsByName[action.command].template || '';

        if (text === '') {
          return [new SetText(command)];
        } else {
          // Simply add the new command after the current cursor line.
          // TODO: This will probably have to be updated to be more robust.
          const lines = text.split('\n');
          lines.splice(line + 1, 0, `${command}`);
          const newText = lines.join('\n');
          return [new SetText(newText)];
        }
      }

      return [];
    }),
  );

  @Effect()
  selectCommandDictionary$: Observable<Action> = this.actions$.pipe(
    ofType<SelectCommandDictionary>(
      CommandDictionaryActionTypes.SelectCommandDictionary,
    ),
    map(action => new FetchCommandDictionary(action.selectedId)),
  );
}
