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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Actions, EffectsMetadata, getEffectsMetadata } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { cold, hot } from 'jasmine-marbles';
import { Observable, of } from 'rxjs';
import { RavenPlanFormDialogData } from '../../shared/models/raven-plan-form-dialog-data';
import { AdaptationService } from '../../shared/services/adaptation.service';
import { AdaptationEffects } from './adaptation.effects';

import { reducers as rootReducers } from '../../app-store';
import { PlanService } from '../../shared/services/plan.service';
import { FetchAdaptationListSuccess } from '../actions/adaptation.actions';
import { reducers } from '../hawk-store';
import { PlanEffects } from './plan.effects';

import {
  RavenActivity,
  RavenAdaptation,
  RavenPlan,
  RavenPlanDetail,
} from '../../shared/models';

import {
  getMockActivityInstances,
  getMockPlan,
  getMockPlans,
  PlanMockService,
} from '../../shared/services/plan-mock.service';

import {
  AdaptationMockService,
  getMockAdaptations,
} from '../../shared/services/adaptation-mock.service';

import {
  ClearSelectedActivity,
  CreateActivity,
  CreateActivityFailure,
  CreateActivitySuccess,
  CreatePlan,
  CreatePlanFailure,
  CreatePlanSuccess,
  DeleteActivity,
  DeleteActivityFailure,
  DeleteActivitySuccess,
  DeletePlan,
  DeletePlanFailure,
  DeletePlanSuccess,
  FetchPlanDetailSuccess,
  FetchPlanList,
  FetchPlanListFailure,
  FetchPlanListSuccess,
  OpenPlanFormDialog,
  SelectActivity,
  UpdateActivity,
  UpdateActivityFailure,
  UpdateActivitySuccess,
} from '../actions/plan.actions';

describe('PlanEffects', () => {
  let actions$: Observable<any>;
  let effects: PlanEffects;
  let metadata: EffectsMetadata<PlanEffects>;
  let dialog: any;
  let store: any;
  let mockRouter: any;

  let adaptations: RavenAdaptation[];
  let plan: RavenPlan;
  let plans: RavenPlan[];
  let planDetail: RavenPlanDetail;
  let instance: RavenActivity;

  beforeEach(() => {
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [
        HttpClientModule,
        MatDialogModule,
        OverlayModule,
        StoreModule.forRoot(rootReducers),
        StoreModule.forFeature('hawk', reducers),
      ],
      providers: [
        PlanEffects,
        AdaptationEffects,
        provideMockActions(() => actions$),
        {
          provide: PlanService,
          useValue: new PlanMockService(),
        },
        {
          provide: AdaptationService,
          useValue: new AdaptationMockService(),
        },
        HttpClient,
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

    adaptations = getMockAdaptations();
    plan = getMockPlan('spider_cat');
    plans = [plan];
    planDetail = {
      ...plan,
      activityInstances: getMockActivityInstances(),
    };
    instance = planDetail.activityInstances['SetArrayTrackingMode_25788'];
  });

  describe('createActivity$', () => {
    it('should register createActivity$ that dispatches an action', () => {
      expect(metadata.createActivity$).toEqual({ dispatch: true });
    });

    it('should return a CreateActivitySuccess action with data upon success', () => {
      const action = new CreateActivity(plan.id, instance);
      const success = new CreateActivitySuccess(plan.id);

      actions$ = hot('-a', { a: action });
      const expected = cold('-b', { b: success });

      expect(effects.createActivity$).toBeObservable(expected);
    });

    it('should return a CreateActivityFailure action with an error upon failure', () => {
      const action = new CreateActivity(plan.id, instance);
      const error = new Error('CreateActivityFailure');
      const failure = new CreateActivityFailure(error);

      const service = TestBed.get(PlanService);
      spyOn(service, 'createActivity').and.returnValue(
        cold('-#|', null, error),
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('--b', { b: failure });

      expect(effects.createActivity$).toBeObservable(expected);
    });
  });

  describe('createActivitySuccess$', () => {
    it('should register createActivitySuccess$ that does not dispatch an action', () => {
      expect(metadata.createActivitySuccess$).toEqual({ dispatch: false });
    });

    it('should navigate to the actions given planId', () => {
      const action = new CreateActivitySuccess('foo');

      actions$ = hot('-a', { a: action });
      const expected = cold('-');

      expect(effects.createActivitySuccess$).toBeObservable(expected);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/plans/foo']);
    });
  });

  describe('createPlan$', () => {
    it('should register createPlan$ that dispatches an action', () => {
      expect(metadata.createPlan$).toEqual({ dispatch: true });
    });

    it('should return a CreatePlanSuccess action with data upon success', () => {
      const action = new CreatePlan(plan);
      const success = new CreatePlanSuccess(plan);

      actions$ = hot('-a', { a: action });
      const expected = cold('-b', { b: success });

      expect(effects.createPlan$).toBeObservable(expected);
    });

    it('should return a CreatePlanFailure action with an error upon failure', () => {
      const action = new CreatePlan(plan);
      const error = new Error('CreatePlanFailure');
      const failure = new CreatePlanFailure(error);

      const service = TestBed.get(PlanService);
      spyOn(service, 'createPlan').and.returnValue(cold('-#|', null, error));

      actions$ = hot('-a', { a: action });
      const expected = cold('--b', { b: failure });

      expect(effects.createPlan$).toBeObservable(expected);
    });
  });

  describe('createPlanSuccess$', () => {
    it('should register createPlanSuccess$ that does not dispatch an action', () => {
      expect(metadata.createPlanSuccess$).toEqual({ dispatch: false });
    });

    it('should route to the actions plan.id and also clear the selected activity', () => {
      const action = new CreatePlanSuccess(plan);
      const clearSelectedActivity = new ClearSelectedActivity();

      actions$ = hot('-a', { a: action });
      const expected = cold('-b', { b: clearSelectedActivity });

      expect(effects.createPlanSuccess$).toBeObservable(expected);
      expect(mockRouter.navigate).toHaveBeenCalledWith([`/plans/${plan.id}`]);
    });
  });

  describe('deleteActivity$', () => {
    it('should register deleteActivity$ that dispatches an action', () => {
      expect(metadata.deleteActivity$).toEqual({ dispatch: true });
    });

    it('should return a DeleteActivitySuccess action with data upon success', () => {
      const action = new DeleteActivity(plan.id, instance.activityId);
      const success = new DeleteActivitySuccess();

      actions$ = hot('-a', { a: action });
      const expected = cold('-b', { b: success });

      expect(effects.deleteActivity$).toBeObservable(expected);
    });

    it('should return a DeleteActivityFailure action with an error upon failure', () => {
      const action = new DeleteActivity(plan.id, instance.activityId);
      const error = new Error('DeleteActivityFailure');
      const failure = new DeleteActivityFailure(error);

      const service = TestBed.get(PlanService);
      spyOn(service, 'deleteActivity').and.returnValue(
        cold('-#|', null, error),
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('--b', { b: failure });

      expect(effects.deleteActivity$).toBeObservable(expected);
    });
  });

  describe('deletePlan$', () => {
    it('should register deletePlan$ that dispatches an action', () => {
      expect(metadata.deletePlan$).toEqual({ dispatch: true });
    });

    it('should return a DeletePlanSuccess action with data upon success', () => {
      const action = new DeletePlan(plan.id);
      const success = new DeletePlanSuccess();

      actions$ = hot('-a', { a: action });
      const expected = cold('-b', { b: success });

      expect(effects.deletePlan$).toBeObservable(expected);
    });

    it('should return a DeletePlanFailure action with an error upon failure', () => {
      const action = new DeletePlan(plan.id);
      const error = new Error('DeletePlanFailure');
      const failure = new DeletePlanFailure(error);

      const service = TestBed.get(PlanService);
      spyOn(service, 'deletePlan').and.returnValue(cold('-#|', null, error));

      actions$ = hot('-a', { a: action });
      const expected = cold('--b', { b: failure });

      expect(effects.deletePlan$).toBeObservable(expected);
    });
  });

  describe('fetchPlanList$', () => {
    it('should register fetchPlanList$ that dispatches an action', () => {
      expect(metadata.fetchPlanList$).toEqual({ dispatch: true });
    });

    it('should return a FetchPlanListSuccess action with data upon success', () => {
      const action = new FetchPlanList();
      const success = new FetchPlanListSuccess(getMockPlans());

      actions$ = hot('-a', { a: action });
      const expected = cold('-b', { b: success });

      expect(effects.fetchPlanList$).toBeObservable(expected);
    });

    it('should return a FetchPlanListFailure action with an error upon failure', () => {
      const action = new FetchPlanList();
      const error = new Error('FetchPlanListFailure');
      const failure = new FetchPlanListFailure(error);

      const service = TestBed.get(PlanService);
      spyOn(service, 'getPlans').and.returnValue(cold('-#|', null, error));

      actions$ = hot('-a', { a: action });
      const expected = cold('--b', { b: failure });

      expect(effects.fetchPlanList$).toBeObservable(expected);
    });
  });

  describe('openUpdatePlanFormDialog$', () => {
    it('should return a CreatePlan with the new plan and creation flag on save', () => {
      const result: RavenPlan = {
        adaptationId: 'ops',
        endTimestamp: '1995-12-17T03:28:00',
        id: 'make_sandwich',
        name: 'Make me a sandwich',
        startTimestamp: '1995-12-17T03:24:00',
      };

      spyOn(dialog, 'open').and.returnValue({
        afterClosed() {
          return of(result);
        },
      });

      const a = new OpenPlanFormDialog(null);
      const b = new CreatePlan(result);

      actions$ = hot('-a', { a });
      const expected = cold('-b', { b });

      expect(effects.openUpdatePlanFormDialog$).toBeObservable(expected);
    });

    /**
     * In order to verify that the data that was passed into the dialog can
     * be updated and passed back, the store's select method needs to be
     * mocked so that we can verify the data before and after.
     */
    it('should return a CreatePlan with the updated plan and creation flag on save', () => {
      const initial: RavenPlan = {
        adaptationId: 'ops',
        endTimestamp: '1995-12-17T03:28:00',
        id: 'make_sandwich',
        name: 'Make me a sandwich',
        startTimestamp: '1995-12-17T03:24:00',
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
      const b = new CreatePlan(initial);

      actions$ = hot('-a', { a });

      // **IMPORTANT**:
      // Manually create an instance of the effects and pass it our initialized classes
      effects = new PlanEffects(
        new Actions(actions$),
        store,
        new PlanMockService() as PlanService,
        dialog,
        mockRouter as Router,
      );

      const expected = cold('-b', { b });
      expect(effects.openUpdatePlanFormDialog$).toBeObservable(expected);
    });
  });

  describe('updateActivity$', () => {
    it('should register updateActivity$ that dispatches an action', () => {
      expect(metadata.updateActivity$).toEqual({ dispatch: true });
    });

    it('should return a UpdateActivitySuccess action with data on success', () => {
      store.dispatch(new FetchPlanListSuccess(plans));
      store.dispatch(new FetchAdaptationListSuccess(adaptations));
      store.dispatch(new FetchPlanDetailSuccess(planDetail));
      store.dispatch(new SelectActivity('SetArrayTrackingMode_25788'));

      instance = planDetail.activityInstances['SetArrayTrackingMode_25788'];

      const action = new UpdateActivity(instance.activityId, instance);

      // Success case should be empty since UpdateActivity patches, and
      // since we just passed in the original unchanged activity, there should
      // be no update.
      const success = new UpdateActivitySuccess(instance.activityId, {});

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.updateActivity$).toBeObservable(expected);
    });

    it('should return a UpdateActivityFailure action with a NoSelectedPlan error on failure', () => {
      const activity = getMockActivityInstances().SetArrayTrackingMode_25788;
      const action = new UpdateActivity('', activity);
      const error = new Error(
        'UpdateActivity: UpdateActivityFailure: NoSelectedPlan',
      );
      const failure = new UpdateActivityFailure(error);

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: failure });

      expect(effects.updateActivity$).toBeObservable(expected);
    });
  });

  describe('updateActivitySuccess$', () => {
    it('should register updateActivitySuccess$ that does not dispatch an action', () => {
      expect(metadata.updateActivitySuccess$).toEqual({ dispatch: false });
    });

    it('should route to the selected plan if a selected plan exists', () => {
      store.dispatch(new FetchPlanDetailSuccess(planDetail));

      const action = new UpdateActivitySuccess('foo', {});
      actions$ = hot('-a', { a: action });
      const expected = cold('-');

      expect(effects.updateActivitySuccess$).toBeObservable(expected);
      expect(mockRouter.navigate).toHaveBeenCalledWith([
        `/plans/${planDetail.id}`,
      ]);
    });

    it('should not route at all if no selected plan exists', () => {
      const action = new UpdateActivitySuccess('foo', {});

      actions$ = hot('-a', { a: action });
      const expected = cold('-');

      expect(effects.updateActivitySuccess$).toBeObservable(expected);
      expect(mockRouter.navigate).not.toHaveBeenCalledWith([
        `/plans/${planDetail.id}`,
      ]);
    });
  });
});
