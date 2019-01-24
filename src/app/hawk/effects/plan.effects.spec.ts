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
import { Router } from '@angular/router';
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
  SaveActivity,
  SaveActivityDetail,
  SaveActivityDetailFailure,
  SaveActivityDetailSuccess,
  SaveActivityFailure,
  SaveActivitySuccess,
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
  let mockRouter: any;

  beforeEach(() => {
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

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
        {
          provide: Router,
          useValue: mockRouter,
        },
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

    it('should return a FetchPlanDetailSuccess action with data on success', () => {
      const action = new FetchPlanList();
      const success = new FetchPlanListSuccess(PlanMockService.getMockPlans());

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

  describe('fetchPlanDetail$', () => {
    it('should return a FetchPlanDetailFailure if the plan is undefined', () => {
      const action = new FetchPlanDetail('spider_cat');
      const planFailure = new FetchPlanDetailFailure(
        new Error('UndefinedPlan'),
      );

      actions$ = hot('--a', { a: action });
      const expected = cold('--b', { b: planFailure });

      expect(effects.fetchPlanDetail$).toBeObservable(expected);
    });

    it('should return a FetchPlanDetailFailure and FetchAdaptationSuccess', () => {
      const adaptationId = 'ops';
      const plans = [PlanMockService.getMockPlan(adaptationId, 'spider_cat')];
      const adaptations = AdaptationMockService.getMockData();
      const adaptation = AdaptationMockService.getMockAdaptation(
        adaptationId,
      ) as RavenAdaptationDetail;

      store.dispatch(new FetchPlanListSuccess(plans));
      store.dispatch(new FetchAdaptationListSuccess(adaptations));

      const error = new Error('MOCK_FAILURE');
      const action = new FetchPlanDetail('spider_cat');
      const planFailure = new FetchPlanDetailFailure(error);
      const adaptationSuccess = new FetchAdaptationSuccess(adaptation);

      const service = TestBed.get(PlanMockService);
      spyOn(service, 'getPlanDetail').and.returnValue(cold('-#|', null, error));

      actions$ = hot('--a', { a: action });
      const expected = cold('--cb', { b: planFailure, c: adaptationSuccess });

      expect(effects.fetchPlanDetail$).toBeObservable(expected);
    });

    it('should return a FetchPlanDetailSuccess and FetchAdaptationFailure', () => {
      const adaptationId = 'test';
      const plan = PlanMockService.getMockPlan(adaptationId, 'spider_cat');
      const plans = [plan];
      const planDetail = {
        ...plan,
        activities: PlanMockService.getMockActivities(),
      };
      const adaptations = AdaptationMockService.getMockData();

      store.dispatch(new FetchPlanListSuccess(plans));
      store.dispatch(new FetchAdaptationListSuccess(adaptations));

      const error = new Error('MOCK_FAILURE');
      const action = new FetchPlanDetail('spider_cat');
      const planSuccess = new FetchPlanDetailSuccess(planDetail);
      const adaptationFailure = new FetchAdaptationFailure(error);

      const service = TestBed.get(AdaptationMockService);
      spyOn(service, 'getAdaptation').and.returnValue(cold('-#|', null, error));

      actions$ = hot('--a', { a: action });
      const expected = cold('--bc', { b: planSuccess, c: adaptationFailure });

      expect(effects.fetchPlanDetail$).toBeObservable(expected);
    });

    it('should return a FetchPlanDetailFailure and FetchAdaptationFailure', () => {
      const plans = [PlanMockService.getMockPlan('test', 'spider_cat')];
      const adaptations = AdaptationMockService.getMockData();

      store.dispatch(new FetchPlanListSuccess(plans));
      store.dispatch(new FetchAdaptationListSuccess(adaptations));

      const error = new Error('MOCK_FAILURE');
      const action = new FetchPlanDetail('spider_cat');
      const planFailure = new FetchPlanDetailFailure(error);
      const adaptationFailure = new FetchAdaptationFailure(error);

      const planService = TestBed.get(PlanMockService);
      spyOn(planService, 'getPlanDetail').and.returnValue(
        cold('-#|', null, error),
      );

      const adaptationService = TestBed.get(AdaptationMockService);
      spyOn(adaptationService, 'getAdaptation').and.returnValue(
        cold('-#|', null, error),
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('--(bc)', { b: planFailure, c: adaptationFailure });

      expect(effects.fetchPlanDetail$).toBeObservable(expected);
    });

    it('should return a FetchPlanDetailSuccess and FetchAdaptationSuccess', () => {
      const adaptationId = 'ops';
      const plan = PlanMockService.getMockPlan(adaptationId, 'spider_cat');
      const plans = [plan];
      const planDetail = {
        ...plan,
        activities: PlanMockService.getMockActivities(),
      };
      const adaptations = AdaptationMockService.getMockData();
      const adaptation = AdaptationMockService.getMockAdaptation(
        adaptationId,
      ) as RavenAdaptationDetail;

      store.dispatch(new FetchPlanListSuccess(plans));
      store.dispatch(new FetchAdaptationListSuccess(adaptations));

      const action = new FetchPlanDetail('spider_cat');
      const planSuccess = new FetchPlanDetailSuccess(planDetail);
      const adaptationSuccess = new FetchAdaptationSuccess(adaptation);

      actions$ = hot('--a', { a: action });
      const expected = cold('--(bc)', { b: planSuccess, c: adaptationSuccess });

      expect(effects.fetchPlanDetail$).toBeObservable(expected);
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
      const b = new SavePlanSuccess(result);

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
      const b = new SavePlanSuccess(initial);

      actions$ = hot('-a', { a });

      // **IMPORTANT**:
      // Manually create an instance of the effects and pass it our initialized classes
      effects = new PlanEffects(
        new Actions(actions$),
        store,
        new PlanMockService(),
        new AdaptationMockService(),
        dialog,
        mockRouter,
      );

      const expected = cold('-b', { b });
      expect(effects.openUpdatePlanFormDialog$).toBeObservable(expected);
    });
  });

  describe('saveActivity$', () => {
    it('should register saveActivity$ that dispatches an action', () => {
      expect(metadata.saveActivity$).toEqual({ dispatch: true });
    });

    it('should return a SaveActivitySuccess action with data on success', () => {
      const activity = PlanMockService.getMockActivities()
        .SetArrayTrackingMode_25788;
      const action = new SaveActivity(activity);
      const success = new SaveActivitySuccess(
        PlanMockService.getMockActivityDetail(activity.id),
      );

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.saveActivity$).toBeObservable(expected);
    });

    it('should return a SaveActivityFailure action with an error on failure', () => {
      const activity = PlanMockService.getMockActivities()
        .SetArrayTrackingMode_25788;
      const action = new SaveActivity(activity);
      const error = new Error('MOCK_FAILURE');
      const failure = new SaveActivityFailure(error);

      const service = TestBed.get(PlanMockService);
      spyOn(service, 'saveActivity').and.returnValue(cold('--#|', null, error));

      actions$ = hot('--a-', { a: action });
      const expected = cold('----b', { b: failure });

      expect(effects.saveActivity$).toBeObservable(expected);
    });
  });

  describe('saveActivityDetail$', () => {
    it('should register saveActivityDetail$ that dispatches an action', () => {
      expect(metadata.saveActivityDetail$).toEqual({ dispatch: true });
    });

    it('should return a SaveActivityDetailSuccess action with data on success and properly route', () => {
      const id = 'SetArrayTrackingMode_25788';
      const activityDetail = PlanMockService.getMockActivityDetail(id);
      const action = new SaveActivityDetail(activityDetail);
      const success = new SaveActivityDetailSuccess(activityDetail);

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.saveActivityDetail$).toBeObservable(expected);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/hawk'], {
        queryParams: { activityId: id },
      });
    });

    it('should return a SaveActivityDetailFailure action with an error on failure and properly route', () => {
      const id = 'SetArrayTrackingMode_25788';
      const activityDetail = PlanMockService.getMockActivityDetail(id);
      const action = new SaveActivityDetail(activityDetail);
      const error = new Error('MOCK_FAILURE');
      const failure = new SaveActivityDetailFailure(error);

      const service = TestBed.get(PlanMockService);
      spyOn(service, 'saveActivity').and.returnValue(cold('--#|', null, error));

      actions$ = hot('--a-', { a: action });
      const expected = cold('----b', { b: failure });

      expect(effects.saveActivityDetail$).toBeObservable(expected);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/hawk'], {
        queryParams: {
          activityId: id,
          err: 'ActivityDetailSaveError',
        },
      });
    });
  });
});
