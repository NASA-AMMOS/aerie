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
  FetchAdaptationFailure,
  FetchAdaptationListSuccess,
  FetchAdaptationSuccess,
} from '../actions/adaptation.actions';

import {
  FetchPlanDetail,
  FetchPlanDetailFailure,
  FetchPlanDetailSuccess,
  FetchPlanList,
  FetchPlanListFailure,
  FetchPlanListSuccess,
  OpenPlanFormDialog,
  RemovePlan,
  RemovePlanSuccess,
  SavePlanSuccess,
} from '../actions/plan.actions';

import { RavenAdaptationDetail } from '../../shared/models/raven-adaptation-detail';
import { RavenPlan } from '../../shared/models/raven-plan';
import { RavenPlanFormDialogData } from '../../shared/models/raven-plan-form-dialog-data';
import { AdaptationMockService } from '../../shared/services/adaptation-mock.service';
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

  describe('fetchPlan$', () => {
    it('should return a FetchPlanDetailFailure if the plan is undefined', () => {
      const action = new FetchPlanDetail('spider_cat');
      const planFailure = new FetchPlanDetailFailure(
        new Error('UndefinedPlan'),
      );

      actions$ = hot('--a', { a: action });
      const expected = cold('--b', { b: planFailure });

      expect(effects.fetchPlan$).toBeObservable(expected);
    });

    it('should return a FetchPlanDetailFailure and FetchAdaptationSuccess', () => {
      const plans = [PlanMockService.getMockPlan('spider_cat')];
      const adaptations = AdaptationMockService.getMockData();
      const adaptation = AdaptationMockService.getMockAdaptation(
        'ops',
      ) as RavenAdaptationDetail;

      store.dispatch(new FetchPlanListSuccess(plans));
      store.dispatch(new FetchAdaptationListSuccess(adaptations));

      const error = new Error('MOCK_FAILURE');
      const action = new FetchPlanDetail('spider_cat');
      const planFailure = new FetchPlanDetailFailure(error);
      const adaptationSuccess = new FetchAdaptationSuccess(adaptation);

      const service = TestBed.get(PlanMockService);
      spyOn(service, 'getPlan').and.returnValue(cold('-#|', null, error));

      actions$ = hot('--a', { a: action });
      const expected = cold('--cb', { b: planFailure, c: adaptationSuccess });

      expect(effects.fetchPlan$).toBeObservable(expected);
    });

    it('should return a FetchPlanDetailSuccess and FetchAdaptationFailure', () => {
      const plans = [PlanMockService.getMockPlan('spider_cat')];
      const adaptations = AdaptationMockService.getMockData();

      store.dispatch(new FetchPlanListSuccess(plans));
      store.dispatch(new FetchAdaptationListSuccess(adaptations));

      const error = new Error('MOCK_FAILURE');
      const action = new FetchPlanDetail('spider_cat');
      const planSuccess = new FetchPlanDetailSuccess(plans[0]);
      const adaptationFailure = new FetchAdaptationFailure(error);

      const service = TestBed.get(AdaptationMockService);
      spyOn(service, 'getAdaptation').and.returnValue(cold('-#|', null, error));

      actions$ = hot('--a', { a: action });
      const expected = cold('--bc', { b: planSuccess, c: adaptationFailure });

      expect(effects.fetchPlan$).toBeObservable(expected);
    });

    it('should return a FetchPlanDetailFailure and FetchAdaptationFailure', () => {
      const plans = [PlanMockService.getMockPlan('spider_cat')];
      const adaptations = AdaptationMockService.getMockData();

      store.dispatch(new FetchPlanListSuccess(plans));
      store.dispatch(new FetchAdaptationListSuccess(adaptations));

      const error = new Error('MOCK_FAILURE');
      const action = new FetchPlanDetail('spider_cat');
      const planFailure = new FetchPlanDetailFailure(error);
      const adaptationFailure = new FetchAdaptationFailure(error);

      const planService = TestBed.get(PlanMockService);
      spyOn(planService, 'getPlan').and.returnValue(cold('-#|', null, error));

      const adaptationService = TestBed.get(AdaptationMockService);
      spyOn(adaptationService, 'getAdaptation').and.returnValue(cold('-#|', null, error));

      actions$ = hot('-a', { a: action });
      const expected = cold('--(bc)', { b: planFailure, c: adaptationFailure });

      expect(effects.fetchPlan$).toBeObservable(expected);
    });

    it('should return a FetchPlanDetailSuccess and FetchAdaptationSuccess', () => {
      const plans = [PlanMockService.getMockPlan('spider_cat')];
      const adaptations = AdaptationMockService.getMockData();
      const adaptation = AdaptationMockService.getMockAdaptation(
        'ops',
      ) as RavenAdaptationDetail;

      store.dispatch(new FetchPlanListSuccess(plans));
      store.dispatch(new FetchAdaptationListSuccess(adaptations));

      const action = new FetchPlanDetail('spider_cat');
      const planSuccess = new FetchPlanDetailSuccess(plans[0]);
      const adaptationSuccess = new FetchAdaptationSuccess(adaptation);

      actions$ = hot('--a', { a: action });
      const expected = cold('--(bc)', { b: planSuccess, c: adaptationSuccess });

      expect(effects.fetchPlan$).toBeObservable(expected);
    });
  });

  describe('fetchPlanList$', () => {
    it('should register fetchPlanList$ that dispatches an action', () => {
      expect(metadata.fetchPlanList$).toEqual({ dispatch: true });
    });

    it('should return a FetchPlanDetailSuccess action with data on success', () => {
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
        adaptationId: 'ops',
        end: '1995-12-17T03:28:00',
        id: 'make_sandwich',
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
        adaptationId: 'ops',
        end: '1995-12-17T03:28:00',
        id: 'make_sandwich',
        name: 'Make me a sandwich',
        start: '1995-12-17T03:24:00',
      };

      // Dispatch to set up initial conditions of the store
      store.dispatch(new FetchPlanListSuccess([initial]));

      // Overwrite the dialog.open method so that we can pass `data`
      // back to `afterClosed` and verify that the whole process worked.
      dialog.open = function(
        component: any,
        // This is Typescript's weird way of typing destructured object params,
        // see https://blog.mariusschulz.com/2015/11/13/typing-destructured-object-parameters-in-typescript
        { data }: { data: RavenPlanFormDialogData },
      ) {
        return {
          afterClosed() {
            return of(data.selectedPlan);
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
        new AdaptationMockService(),
        dialog,
      );

      const expected = cold('-b', { b });
      expect(effects.openUpdatePlanFormDialog$).toBeObservable(expected);
    });
  });
});
