/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClient, HttpClientModule } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { addMatchers, cold, hot, initTestScheduler } from 'jasmine-marbles';
import { Observable } from 'rxjs';
import { reducers as rootReducers } from '../../app-store';
import { ShowToast } from '../../shared/actions/toast.actions';
import {
  FetchChildren,
  FetchChildrenFailure,
  UpdateChildren,
} from '../actions/file.actions';
import { LoadingBarHide, LoadingBarShow } from '../actions/layout.actions';
import { reducers } from '../sequencing-store';
import { FileMockService, getChildren } from '../services/file-mock.service';
import { FileService } from '../services/file.service';
import { FileEffects } from './file.effects';

describe('FileEffects', () => {
  let actions$: Observable<any>;
  let effects: FileEffects;

  const loadingBarShow = new LoadingBarShow();
  const loadingBarHide = new LoadingBarHide();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientModule,
        StoreModule.forRoot(rootReducers),
        StoreModule.forFeature('sequencing', reducers),
      ],
      providers: [
        FileEffects,
        provideMockActions(() => actions$),
        {
          provide: FileService,
          useValue: new FileMockService(),
        },
        HttpClient,
        Store,
      ],
    });

    initTestScheduler();
    addMatchers();
    effects = TestBed.get(FileEffects);
  });

  describe('fetchChildren$', () => {
    it('should return a UpdateChildren action with data upon success', () => {
      const parentId = 'root';
      const children = getChildren(parentId);
      const action = new FetchChildren(parentId);
      const success = new UpdateChildren(parentId, children);

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcd)', {
        b: loadingBarShow,
        c: success,
        d: loadingBarHide,
      });

      expect(effects.fetchChildren$).toBeObservable(expected);
    });

    it('should return a FetchChildrenFailure action with an error upon failure', () => {
      const parentId = 'root';
      const action = new FetchChildren(parentId);
      const error = new Error('FetchChildrenFailed');
      const failure = new FetchChildrenFailure(error);
      const showToast = new ShowToast(
        'error',
        error.message,
        'Fetch Children Failed',
      );

      const service = TestBed.get(FileMockService);
      spyOn(service, 'fetchChildren').and.returnValue(cold('#|', null, error));
      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcde)', {
        b: loadingBarShow,
        c: failure,
        d: showToast,
        e: loadingBarHide,
      });

      expect(effects.fetchChildren$).toBeObservable(expected);
    });
  });
});
