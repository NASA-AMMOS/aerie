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

import { PlanEffects } from './plan.effects';

import {
  FetchPlanList,
  FetchPlanListFailure,
  FetchPlanListSuccess,
  OpenPlanFormDialog,
  RemovePlan,
  RemovePlanSuccess,
  SavePlanSuccess,
} from '../actions/plan.actions';

import { RavenPlan } from '../../shared/models/raven-plan';
import { PlanMockService } from '../../shared/services/plan-mock.service';

import { reducers } from '../hawk-store';

describe('PlanEffects', () => {
  let actions$: Observable<any>;
  let effects: PlanEffects;
  let metadata: EffectsMetadata<PlanEffects>;
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
        PlanEffects,
        provideMockActions(() => actions$),
        PlanMockService,
        Store,
      ],
    });

    effects = TestBed.get(PlanEffects);
    metadata = getEffectsMetadata(effects);
    dialog = TestBed.get(MatDialog);
    store = TestBed.get(Store);
  });

  describe('fetchPlanList$', () => {
    it('should register fetchPlanList$ that dispatches an action', () => {
      expect(metadata.fetchPlanList$).toEqual({ dispatch: true });
    });

    it('should return a FetchPlanSuccess action with data on success', () => {
      const action = new FetchPlanList();
      const success = new FetchPlanListSuccess(PlanMockService.getMockData());

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.fetchPlanList$).toBeObservable(expected);
    });

    it('should return a FetchPlanListFailure action with an error on failure', () => {
      const action = new FetchPlanList();
      const error = new Error('MOCK_FAILURE');
      const failure = new FetchPlanListFailure(error);

      const service = TestBed.get(PlanMockService);
      spyOn(service, 'getPlans').and.returnValue(cold('--#|', null, error));

      actions$ = hot('--a-', { a: action });
      const expected = cold('----b', { b: failure });

      expect(effects.fetchPlanList$).toBeObservable(expected);
    });
  });

  /**
   * There are currently no failure states. The return is always successful, so
   * tests for these aspects are not currently implemented.
   */
  describe('removePlan$', () => {
    it('should return a RemovePlanSuccess action with an ID on success', () => {
      const id = 'test0';
      const a = new RemovePlan(id);
      const b = new RemovePlanSuccess(id);

      actions$ = hot('--a-', { a });
      const expected = cold('--b', { b });

      expect(effects.removePlan$).toBeObservable(expected);
    });
  });

  /**
   * There aren't any failure states at this time and cancel does nothing, so
   * tests for these aspects are currently not implemented.
   */
  describe('openUpdatePlanFormDialog$', () => {
    it('should return a SavePlanSuccess with the new plan and creation flag on save', () => {
      const result: RavenPlan = {
        end: '1995-12-17T03:28:00',
        hsoc: 30,
        id: 'make_sandwich',
        mpow: 24,
        msoc: 10,
        name: 'Make me a sandwich',
        start: '1995-12-17T03:24:00',
      };

      spyOn(dialog, 'open').and.returnValue({
        afterClosed() {
          return of(result);
        },
      });

      const a = new OpenPlanFormDialog(null);
      const b = new SavePlanSuccess(result, true);

      actions$ = hot('-a', { a });
      const expected = cold('-b', { b });

      expect(effects.openUpdatePlanFormDialog$).toBeObservable(expected);
    });

    /**
     * In order to verify that the data that was passed into the dialog can
     * be updated and passed back, the store's select method needs to be
     * mocked so that we can verify the data before and after.
     */
    it('should return a SavePlanSuccess with the updated plan and creation flag on save', () => {
      const initial: RavenPlan = {
        end: '1995-12-17T03:28:00',
        hsoc: 30,
        id: 'make_sandwich',
        mpow: 24,
        msoc: 10,
        name: 'Make me a sandwich',
        start: '1995-12-17T03:24:00',
      };

      // Dispatch to set up initial conditions of the store
      store.dispatch(new FetchPlanListSuccess([initial]));

      // Overwrite the dialog.open method so that we can pass `data`
      // back to `afterClosed` and verify that the whole process worked.
      dialog.open = function(component: any, data: any) {
        return {
          afterClosed() {
            return of(data.data);
          },
        };
      };

      // Select an plan from the mock store which we populated above
      const a = new OpenPlanFormDialog('make_sandwich');

      // Verify that the same object we passed in comes back without any
      // modification (since no dialog/form was displayed and no use
      // input was accepted the result should be the same as the input).
      const b = new SavePlanSuccess(initial, false);

      actions$ = hot('-a', { a });

      // **IMPORTANT**:
      // Manually create an instance of the effects and pass it our initialized classes
      effects = new PlanEffects(
        new Actions(actions$),
        store,
        new PlanMockService(),
        dialog,
      );

      const expected = cold('-b', { b });
      expect(effects.openUpdatePlanFormDialog$).toBeObservable(expected);
    });
  });
});
