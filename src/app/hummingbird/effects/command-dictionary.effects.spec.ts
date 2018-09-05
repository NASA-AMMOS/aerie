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
import { StoreModule } from '@ngrx/store';
import { cold, hot } from 'jasmine-marbles';
import { Observable, of } from 'rxjs';
import { MpsServerService } from '../../shared/services/mps-server.service';
import { CommandDictionaryEffects } from './command-dictionary.effects';

import {
  FetchCommandDictionary,
  FetchCommandDictionaryFailure,
  FetchCommandDictionaryList,
  FetchCommandDictionaryListFailure,
  FetchCommandDictionaryListSuccess,
  FetchCommandDictionarySuccess,
} from '../actions/command-dictionary.actions';

import * as mpsServerMocks from '../../shared/mocks/mps-server';

describe('CommandDictionaryEffects', () => {
  let effects: CommandDictionaryEffects;
  let metadata: EffectsMetadata<CommandDictionaryEffects>;
  let mpsServerService: any;
  let actions: Observable<any> = of();

  beforeEach(() => {
    mpsServerService = jasmine.createSpyObj('MpsServerService', [
      'getCommandDictionaryList',
      'getCommandDictionary',
    ]);

    TestBed.configureTestingModule({
      imports: [StoreModule.forRoot({})],
      providers: [
        CommandDictionaryEffects,
        provideMockActions(() => actions),
        {
          provide: MpsServerService,
          useValue: mpsServerService,
        },
      ],
    });

    effects = TestBed.get(CommandDictionaryEffects);
    metadata = getEffectsMetadata(effects);
  });

  describe('fetchCommandDictionaryList$', () => {
    it('should register fetchCommandDictionaryList$ that dispatches an action', () => {
      expect(metadata.fetchCommandDictionaryList$).toEqual({ dispatch: true });
    });

    it('should return a FetchCommandDictionaryListSuccess with data on success', () => {
      const action = new FetchCommandDictionaryList();
      const success = new FetchCommandDictionaryListSuccess(
        mpsServerMocks.commandDictionaryList
      );

      mpsServerService.getCommandDictionaryList.and.returnValue(
        of(mpsServerMocks.commandDictionaryList)
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
      mpsServerService.getCommandDictionaryList.and.returnValue(
        cold('--#|', {}, error)
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
      const name = mpsServerMocks.commandDictionaryList[0].id;
      const action = new FetchCommandDictionary(name);
      const success = new FetchCommandDictionarySuccess(
        mpsServerMocks.getCommandList()
      );

      mpsServerService.getCommandDictionary.and.returnValue(
        of(mpsServerMocks.getCommandList())
      );

      actions = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.fetchCommandDictionary$).toBeObservable(expected);
    });

    it('should return a FetchCommandDictionaryFailure with error on failure', () => {
      const action = new FetchCommandDictionary(
        mpsServerMocks.commandDictionaryList[0].id
      );
      const error = new Error('MOCK_FAILURE');
      const failure = new FetchCommandDictionaryFailure(error);

      // Make the service return a fake error observable
      mpsServerService.getCommandDictionary.and.returnValue(
        cold('--#|', {}, error)
      );

      actions = hot('-a--', { a: action });

      // We expect the service to return the failure because we have forced the
      // service to fail with the spy
      const expected = cold('---b', { b: failure });

      expect(effects.fetchCommandDictionary$).toBeObservable(expected);
    });
  });
});
