/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { TestBed } from '@angular/core/testing';
import { EffectsMetadata, getEffectsMetadata } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { cold, hot } from 'jasmine-marbles';
import { Observable, of } from 'rxjs';
import { commands } from '../../shared/mocks';
import { SetText } from '../actions/editor.actions';
import { reducers } from '../hummingbird-store';
import { CommandDictionaryEffects } from './command-dictionary.effects';

import {
  CommandDictionaryMockService,
  mockCommandDictionaryList,
} from '../../shared/services/command-dictionary-mock.service';

import {
  FetchCommandDictionary,
  FetchCommandDictionaryFailure,
  FetchCommandDictionaryList,
  FetchCommandDictionaryListFailure,
  FetchCommandDictionaryListSuccess,
  FetchCommandDictionarySuccess,
  SelectCommand,
  SelectCommandDictionary,
} from '../actions/command-dictionary.actions';

describe('CommandDictionaryEffects', () => {
  let effects: CommandDictionaryEffects;
  let metadata: EffectsMetadata<CommandDictionaryEffects>;
  let commandDictionaryMockService: any;
  let actions: Observable<any> = of();
  let store: any;

  beforeEach(() => {
    commandDictionaryMockService = jasmine.createSpyObj(
      'CommandDictionaryMockService',
      ['getCommandDictionaryList', 'getCommandDictionary'],
    );

    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot({}),
        StoreModule.forFeature('hummingbird', reducers),
      ],
      providers: [
        CommandDictionaryEffects,
        provideMockActions(() => actions),
        {
          provide: CommandDictionaryMockService,
          useValue: commandDictionaryMockService,
        },
        Store,
      ],
    });

    effects = TestBed.get(CommandDictionaryEffects);
    metadata = getEffectsMetadata(effects);
    store = TestBed.get(Store);
  });

  describe('fetchCommandDictionaryList$', () => {
    it('should register fetchCommandDictionaryList$ that dispatches an action', () => {
      expect(metadata.fetchCommandDictionaryList$).toEqual({ dispatch: true });
    });

    it('should return a FetchCommandDictionaryListSuccess with data on success', () => {
      const action = new FetchCommandDictionaryList();
      const success = new FetchCommandDictionaryListSuccess(
        mockCommandDictionaryList,
      );

      commandDictionaryMockService.getCommandDictionaryList.and.returnValue(
        of(mockCommandDictionaryList),
      );

      actions = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.fetchCommandDictionaryList$).toBeObservable(expected);
    });

    it('should return a FetchCommandDictionaryListFailure with error on failure', () => {
      const action = new FetchCommandDictionaryList();
      const error = new Error('MOCK_FAILURE');
      const failure = new FetchCommandDictionaryListFailure(error);

      // Make the service return a fake error observable
      commandDictionaryMockService.getCommandDictionaryList.and.returnValue(
        cold('--#|', {}, error),
      );

      actions = hot('-a--', { a: action });

      // We expect the service to return the failure because we have forced the
      // service to fail with the spy
      const expected = cold('---b', { b: failure });

      expect(effects.fetchCommandDictionaryList$).toBeObservable(expected);
    });
  });

  describe('fetchCommandDictionary$', () => {
    it('should register fetchCommandDictionary$ that dispatches an action', () => {
      expect(metadata.fetchCommandDictionary$).toEqual({ dispatch: true });
    });

    it('should return a FetchCommandDictionarySuccess with data on success', () => {
      const name = mockCommandDictionaryList[0].id;
      const action = new FetchCommandDictionary(name);
      const success = new FetchCommandDictionarySuccess(
        commandDictionaryMockService.getCommandDictionary(),
      );

      commandDictionaryMockService.getCommandDictionary.and.returnValue(
        of(commandDictionaryMockService.getCommandDictionary()),
      );

      actions = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.fetchCommandDictionary$).toBeObservable(expected);
    });

    it('should return a FetchCommandDictionaryFailure with error on failure', () => {
      const action = new FetchCommandDictionary(
        mockCommandDictionaryList[0].id,
      );
      const error = new Error('MOCK_FAILURE');
      const failure = new FetchCommandDictionaryFailure(error);

      // Make the service return a fake error observable
      commandDictionaryMockService.getCommandDictionary.and.returnValue(
        cold('--#|', {}, error),
      );

      actions = hot('-a--', { a: action });

      // We expect the service to return the failure because we have forced the
      // service to fail with the spy
      const expected = cold('---b', { b: failure });

      expect(effects.fetchCommandDictionary$).toBeObservable(expected);
    });
  });

  describe('selectCommand$', () => {
    it('should register selectCommand$ that dispatches an action', () => {
      expect(metadata.selectCommand$).toEqual({ dispatch: true });
    });

    it('should return a SetText with data on success', () => {
      // Dispatch to set up initial conditions of the store
      store.dispatch(new FetchCommandDictionarySuccess(commands));

      const command = commands[0];
      const action = new SelectCommand(command.name);
      const success = new SetText(`${command.name} 0`); // Build the template manually since we dont have the data.

      actions = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.selectCommand$).toBeObservable(expected);
    });
  });

  describe('selectCommandDictionary$', () => {
    it('should register selectCommandDictionary$ that dispatches an action', () => {
      expect(metadata.selectCommandDictionary$).toEqual({ dispatch: true });
    });

    it('should return a FetchCommandDictionary with an id', () => {
      const id = '42';
      const action = new SelectCommandDictionary(id);
      const success = new FetchCommandDictionary(id);

      actions = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.selectCommandDictionary$).toBeObservable(expected);
    });
  });
});
