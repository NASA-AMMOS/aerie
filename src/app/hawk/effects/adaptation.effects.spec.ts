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

import { AdaptationEffects } from './adaptation.effects';

import {
  FetchAdaptationList,
  FetchAdaptationListFailure,
  FetchAdaptationListSuccess,
  FetchAdaptationSuccess,
  OpenActivityTypeFormDialog,
  RemoveActivityType,
  RemoveActivityTypeSuccess,
  SaveActivityTypeFailure,
  SaveActivityTypeSuccess,
} from '../actions/adaptation.actions';

import { RavenActivityType } from '../../shared/models/raven-activity-type';
import { RavenAdaptationDetail } from '../../shared/models/raven-adaptation-detail';
import { AdaptationMockService } from '../../shared/services/adaptation-mock.service';
import { reducers } from '../hawk-store';

describe('AdaptationEffects', () => {
  let actions$: Observable<any>;
  let effects: AdaptationEffects;
  let metadata: EffectsMetadata<AdaptationEffects>;
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
        AdaptationEffects,
        provideMockActions(() => actions$),
        AdaptationMockService,
        Store,
      ],
    });

    effects = TestBed.get(AdaptationEffects);
    metadata = getEffectsMetadata(effects);
    dialog = TestBed.get(MatDialog);
    store = TestBed.get(Store);
  });

  describe('fetchAdaptationList$', () => {
    it('should register fetchAdaptationList$ that dispatches an action', () => {
      expect(metadata.fetchAdaptationList$).toEqual({ dispatch: true });
    });

    it('should return a FetchAdaptationSuccess action with data on success', () => {
      const action = new FetchAdaptationList();
      const success = new FetchAdaptationListSuccess(
        AdaptationMockService.getMockData(),
      );

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.fetchAdaptationList$).toBeObservable(expected);
    });

    it('should return a FetchAdaptationListFailure action with an error on failure', () => {
      const action = new FetchAdaptationList();
      const error = new Error('MOCK_FAILURE');
      const failure = new FetchAdaptationListFailure(error);

      const service = TestBed.get(AdaptationMockService);
      spyOn(service, 'getAdaptations').and.returnValue(
        cold('--#|', null, error),
      );

      actions$ = hot('--a-', { a: action });
      const expected = cold('----b', { b: failure });

      expect(effects.fetchAdaptationList$).toBeObservable(expected);
    });
  });

  describe('activityTypes', () => {
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
      it('should return an error when an adaptation is not selected', () => {
        const error = new Error('NoSelectedAdaptation');
        const a = new OpenActivityTypeFormDialog(null);
        const b = new SaveActivityTypeFailure(error);

        actions$ = hot('-a', { a });
        const expected = cold('-b', { b });

        expect(effects.openUpdateActivityTypeFormDialog$).toBeObservable(
          expected,
        );
      });

      it('should return a SaveActivityTypeSuccess with the new activity type and creation flag on save', () => {
        const result: RavenActivityType = {
          description: 'SAMMICH TIME',
          id: 'make_sandwich',
          name: 'Make me a sandwich',
          start: 'now',
        };

        const adaptation: RavenAdaptationDetail = {
          activityTypes: {},
          id: 'foods',
          name: 'Foods',
          version: '1.0.0',
        };

        // Dispatch to set up initial conditions of the store
        store.dispatch(new FetchAdaptationSuccess(adaptation));

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
        const initial: RavenAdaptationDetail = {
          activityTypes: {
            make_sandwich: {
              description: 'SAMMICH TIME',
              id: 'make_sandwich',
              name: 'Make me a sandwich',
              start: 'now',
            },
          },
          id: 'foods',
          name: 'Foods',
          version: '1.0.0',
        };

        // Dispatch to set up initial conditions of the store
        store.dispatch(new FetchAdaptationSuccess(initial));

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
        const b = new SaveActivityTypeSuccess(
          initial.activityTypes['make_sandwich'],
          false,
        );

        actions$ = hot('-a', { a });

        // **IMPORTANT**:
        // Manually create an instance of the effects and pass it our initialized classes
        effects = new AdaptationEffects(
          new Actions(actions$),
          store,
          new AdaptationMockService(),
          dialog,
        );

        const expected = cold('-b', { b });
        expect(effects.openUpdateActivityTypeFormDialog$).toBeObservable(
          expected,
        );
      });
    });
  });
});
