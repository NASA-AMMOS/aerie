/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { OverlayModule } from '@angular/cdk/overlay';
import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Actions, EffectsMetadata, getEffectsMetadata } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { cold, hot } from 'jasmine-marbles';
import { Observable, of } from 'rxjs';

import { ActivityTypeEffects } from './activity-type.effects';

import {
  FetchActivityTypeList,
  FetchActivityTypeListFailure,
  FetchActivityTypeListSuccess,
  OpenActivityTypeFormDialog,
  RemoveActivityType,
  RemoveActivityTypeSuccess,
  SaveActivityTypeSuccess,
} from '../actions/activity-type.actions';

import { RavenActivityType } from '../../shared/models/raven-activity-type';
import { ActivityTypeMockService } from '../../shared/services/activity-type-mock.service';

import { reducers } from '../hawk-store';
// import * as activityTypeReducer from '../reducers/activity-type.reducer';

describe('ActivityTypeEffects', () => {
  let actions$: Observable<any>;
  let effects: ActivityTypeEffects;
  let metadata: EffectsMetadata<ActivityTypeEffects>;
  let dialog: any;
  let store: any;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        MatDialogModule,
        OverlayModule,
        StoreModule.forRoot({}),
        StoreModule.forFeature('hawk', reducers),
      ],
      providers: [
        ActivityTypeEffects,
        provideMockActions(() => actions$),
        ActivityTypeMockService,
        Store,
      ],
    });

    effects = TestBed.get(ActivityTypeEffects);
    metadata = getEffectsMetadata(effects);
    dialog = TestBed.get(MatDialog);
    store = TestBed.get(Store);
  });

  describe('fetchActivityTypeList$', () => {
    it('should register fetchActivityTypeList$ that dispatches an action', () => {
      expect(metadata.fetchActivityTypeList$).toEqual({ dispatch: true });
    });

    it('should return a FetchActivityTypeSuccess action with data on success', () => {
      const action = new FetchActivityTypeList();
      const success = new FetchActivityTypeListSuccess(
        ActivityTypeMockService.getMockData(),
      );

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.fetchActivityTypeList$).toBeObservable(expected);
    });

    it('should return a FetchActivityTypeListFailure action with an error on failure', () => {
      const action = new FetchActivityTypeList();
      const error = new Error('MOCK_FAILURE');
      const failure = new FetchActivityTypeListFailure(error);

      const service = TestBed.get(ActivityTypeMockService);
      spyOn(service, 'getActivityTypes').and.returnValue(
        cold('--#|', null, error),
      );

      actions$ = hot('--a-', { a: action });
      const expected = cold('----b', { b: failure });

      expect(effects.fetchActivityTypeList$).toBeObservable(expected);
    });
  });

  /**
   * There are currently no failure states. The return is always successful, so
   * tests for these aspects are not currently implemented.
   */
  describe('removeActivityType$', () => {
    it('should return a RemoveActivityTypeSuccess action with an ID on success', () => {
      const id = 'test0';
      const a = new RemoveActivityType(id);
      const b = new RemoveActivityTypeSuccess(id);

      actions$ = hot('--a-', { a });
      const expected = cold('--b', { b });

      expect(effects.removeActivityType$).toBeObservable(expected);
    });
  });

  /**
   * There aren't any failure states at this time and cancel does nothing, so
   * tests for these aspects are currently not implemented.
   */
  describe('openUpdateActivityTypeFormDialog$', () => {
    it('should return a SaveActivityTypeSuccess with the new activity type and creation flag on save', () => {
      const result: RavenActivityType = {
        id: 'make_sandwich',
        name: 'Make me a sandwich',
        start: 'now',
      };

      // const dialog = TestBed.get(MatDialog);
      spyOn(dialog, 'open').and.returnValue({
        afterClosed() {
          return of(result);
        },
      });

      const a = new OpenActivityTypeFormDialog(null);
      const b = new SaveActivityTypeSuccess(result, true);

      actions$ = hot('-a', { a });
      const expected = cold('-b', { b });

      expect(effects.openUpdateActivityTypeFormDialog$).toBeObservable(
        expected,
      );
    });

    /**
     * In order to verify that the data that was passed into the dialog can
     * be updated and passed back, the store's select method needs to be
     * mocked so that we can verify the data before and after.
     */
    it('should return a SaveActivityTypeSuccess with the updated activity type and creation flag on save', () => {
      const initial: RavenActivityType = {
        id: 'make_sandwich',
        name: 'Make me a sandwich',
        start: 'now',
      };

      // Dispatch to set up initial conditions of the store
      store.dispatch(new FetchActivityTypeListSuccess([initial]));

      // Overwrite the dialog.open method so that we can pass `data`
      // back to `afterClosed` and verify that the whole process worked.
      dialog.open = function(component: any, data: any) {
        return {
          afterClosed() {
            return of(data.data);
          },
        };
      };

      // Select an activityType from the mock store which we populated above
      const a = new OpenActivityTypeFormDialog('make_sandwich');

      // Verify that the same object we passed in comes back without any
      // modification (since no dialog/form was displayed and no use
      // input was accepted the result should be the same as the input).
      const b = new SaveActivityTypeSuccess(initial, false);

      actions$ = hot('-a', { a });

      // **IMPORTANT**:
      // Manually create an instance of the effects and pass it our initialized classes
      effects = new ActivityTypeEffects(
        new Actions(actions$),
        store,
        new ActivityTypeMockService(),
        dialog,
      );

      const expected = cold('-b', { b });
      expect(effects.openUpdateActivityTypeFormDialog$).toBeObservable(
        expected,
      );
    });
  });
});
