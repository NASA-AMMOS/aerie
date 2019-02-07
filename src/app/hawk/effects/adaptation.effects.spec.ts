/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { OverlayModule } from '@angular/cdk/overlay';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MatDialogModule } from '@angular/material/dialog';
import { EffectsMetadata, getEffectsMetadata } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { cold, hot } from 'jasmine-marbles';
import { Observable } from 'rxjs';
import { reducers as rootReducers } from '../../app-store';
import { AdaptationService } from '../../shared/services/adaptation.service';
import { reducers } from '../hawk-store';
import { AdaptationEffects } from './adaptation.effects';

import {
  FetchActivityTypes,
  FetchActivityTypesFailure,
  FetchActivityTypesSuccess,
  FetchAdaptationList,
  FetchAdaptationListFailure,
  FetchAdaptationListSuccess,
} from '../actions/adaptation.actions';

import {
  AdaptationMockService,
  getMockActivityTypes,
  getMockAdaptations,
} from '../../shared/services/adaptation-mock.service';

describe('AdaptationEffects', () => {
  let actions$: Observable<any>;
  let effects: AdaptationEffects;
  let metadata: EffectsMetadata<AdaptationEffects>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientModule,
        MatDialogModule,
        OverlayModule,
        StoreModule.forRoot(rootReducers),
        StoreModule.forFeature('hawk', reducers),
      ],
      providers: [
        AdaptationEffects,
        provideMockActions(() => actions$),
        {
          provide: AdaptationService,
          useValue: new AdaptationMockService(),
        },
        Store,
        HttpClient,
      ],
    });

    effects = TestBed.get(AdaptationEffects);
    metadata = getEffectsMetadata(effects);
  });

  describe('fetchAdaptationList$', () => {
    it('should register fetchAdaptationList$ that dispatches an action', () => {
      expect(metadata.fetchAdaptationList$).toEqual({ dispatch: true });
    });

    it('should return a FetchAdaptationsSuccess action with data on success', () => {
      const action = new FetchAdaptationList();
      const success = new FetchAdaptationListSuccess(getMockAdaptations());

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.fetchAdaptationList$).toBeObservable(expected);
    });

    it('should return a FetchAdaptationListFailure action with an error on failure', () => {
      const action = new FetchAdaptationList();
      const error = new Error('MOCK_FAILURE');
      const failure = new FetchAdaptationListFailure(error);

      const service = TestBed.get(AdaptationService);
      spyOn(service, 'getAdaptations').and.returnValue(
        cold('--#|', null, error),
      );

      actions$ = hot('--a-', { a: action });
      const expected = cold('----b', { b: failure });

      expect(effects.fetchAdaptationList$).toBeObservable(expected);
    });
  });

  describe('fetchActivityTypes$', () => {
    it('should register fetchActivityTypes$ that dispatches an action', () => {
      expect(metadata.fetchActivityTypes$).toEqual({ dispatch: true });
    });

    it('should return a FetchActivityTypesSuccess action with data on success', () => {
      const action = new FetchActivityTypes('foo');
      const success = new FetchActivityTypesSuccess(getMockActivityTypes());

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.fetchActivityTypes$).toBeObservable(expected);
    });

    it('should return a FetchActivityTypesFailure action with an error on failure', () => {
      const action = new FetchActivityTypes('bar');
      const error = new Error('MOCK_FAILURE');
      const failure = new FetchActivityTypesFailure(error);

      const service = TestBed.get(AdaptationService);
      spyOn(service, 'getActivityTypes').and.returnValue(
        cold('--#|', null, error),
      );

      actions$ = hot('--a-', { a: action });
      const expected = cold('----b', { b: failure });

      expect(effects.fetchActivityTypes$).toBeObservable(expected);
    });
  });
});
