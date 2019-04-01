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
import { MatDialog, MatDialogModule } from '@angular/material';
import { Router } from '@angular/router';
import { EffectsMetadata, getEffectsMetadata } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { cold, hot } from 'jasmine-marbles';
import { keyBy } from 'lodash';
import { Observable, of } from 'rxjs';
import { reducers as rootReducers } from '../../app-store';
import { ShowToast } from '../../shared/actions/toast.actions';
import {
  ActivityInstance,
  Adaptation,
  Plan,
  StringTMap,
} from '../../shared/models';
import { FetchAdaptationsSuccess } from '../actions/adaptation.actions';
import { LoadingBarHide, LoadingBarShow } from '../actions/layout.actions';
import {
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
  FetchActivities,
  FetchActivitiesFailure,
  FetchActivitiesSuccess,
  FetchPlans,
  FetchPlansFailure,
  FetchPlansSuccess,
  UpdateActivity,
  UpdateActivityFailure,
  UpdateActivitySuccess,
} from '../actions/plan.actions';
import { reducers } from '../planning-store';
import {
  AdaptationMockService,
  getMockAdaptations,
} from '../services/adaptation-mock.service';
import { AdaptationService } from '../services/adaptation.service';
import {
  getMockActivities,
  getMockPlan,
  getMockPlans,
  PlanMockService,
} from '../services/plan-mock.service';
import { PlanService } from '../services/plan.service';
import { AdaptationEffects } from './adaptation.effects';
import { PlanEffects } from './plan.effects';

describe('PlanEffects', () => {
  let actions$: Observable<any>;
  let effects: PlanEffects;
  let metadata: EffectsMetadata<PlanEffects>;
  let dialog: any;
  let store: any;
  let mockRouter: any;

  let adaptations: Adaptation[];
  let plan: Plan;
  let plans: Plan[];
  let activities: ActivityInstance[];
  let activitiesMap: StringTMap<ActivityInstance>;
  let activity: ActivityInstance;

  const loadingBarShow = new LoadingBarShow();
  const loadingBarHide = new LoadingBarHide();

  beforeEach(() => {
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [
        HttpClientModule,
        MatDialogModule,
        OverlayModule,
        StoreModule.forRoot(rootReducers),
        StoreModule.forFeature('planning', reducers),
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
    activities = getMockActivities();
    activitiesMap = keyBy(activities, 'activityId');
    activity = activitiesMap['SetArrayTrackingMode_25788'];
  });

  describe('createActivity$', () => {
    it('should register createActivity$ that dispatches an action', () => {
      expect(metadata.createActivity$).toEqual({ dispatch: true });
    });

    it('should return a CreateActivitySuccess action with data upon success', () => {
      const planId = plan.id || '';
      const action = new CreateActivity(planId, activity);
      const success = new CreateActivitySuccess(planId, activity);
      const showToast = new ShowToast(
        'success',
        'New activity has been successfully created and saved.',
        'Create Activity Success',
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bc)', { b: success, c: showToast });

      expect(effects.createActivity$).toBeObservable(expected);
    });

    it('should return a CreateActivityFailure action with an error upon failure', () => {
      const planId = plan.id || '';
      const action = new CreateActivity(planId, activity);
      const error = new Error('CreateActivityFailure');
      const failure = new CreateActivityFailure(error);
      const showToast = new ShowToast(
        'error',
        error.message,
        'Create Activity Failed',
      );

      const service = TestBed.get(PlanService);
      spyOn(service, 'createActivity').and.returnValue(
        cold('-#|', null, error),
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('--(bc)', { b: failure, c: showToast });

      expect(effects.createActivity$).toBeObservable(expected);
    });
  });

  describe('createActivitySuccess$', () => {
    it('should register createActivitySuccess$ that does not dispatch an action', () => {
      expect(metadata.createActivitySuccess$).toEqual({ dispatch: false });
    });

    it('should navigate to the actions given planId', () => {
      const action = new CreateActivitySuccess('foo', activity);

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
      const showToast = new ShowToast(
        'success',
        'New plan has been successfully created and saved.',
        'Create Plan Success',
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bc)', { b: success, c: showToast });

      expect(effects.createPlan$).toBeObservable(expected);
    });

    it('should return a CreatePlanFailure action with an error upon failure', () => {
      const action = new CreatePlan(plan);
      const error = new Error('CreatePlanFailure');
      const failure = new CreatePlanFailure(error);
      const showToast = new ShowToast(
        'error',
        error.message,
        'Create Plan Failure',
      );

      const service = TestBed.get(PlanService);
      spyOn(service, 'createPlan').and.returnValue(cold('-#|', null, error));

      actions$ = hot('-a', { a: action });
      const expected = cold('--(bc)', { b: failure, c: showToast });

      expect(effects.createPlan$).toBeObservable(expected);
    });
  });

  describe('deleteActivity$', () => {
    it('should register deleteActivity$ that dispatches an action', () => {
      expect(metadata.deleteActivity$).toEqual({ dispatch: true });
    });

    it('should return a DeleteActivitySuccess action with data upon success', () => {
      spyOn(dialog, 'open').and.returnValue({
        afterClosed() {
          return of({ confirm: true });
        },
      });

      const planId = plan.id || '';
      const action = new DeleteActivity(planId, activity.activityId);
      const success = new DeleteActivitySuccess(activity.activityId);
      const showToast = new ShowToast(
        'success',
        'Activity has been successfully deleted.',
        'Delete Activity Success',
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcde)', {
        b: loadingBarShow,
        c: success,
        d: showToast,
        e: loadingBarHide,
      });

      expect(effects.deleteActivity$).toBeObservable(expected);
    });

    it('should return a DeleteActivityFailure action with an error upon failure', () => {
      spyOn(dialog, 'open').and.returnValue({
        afterClosed() {
          return of({ confirm: true });
        },
      });

      const planId = plan.id || '';
      const action = new DeleteActivity(planId, activity.activityId);
      const error = new Error('DeleteActivityFailure');
      const failure = new DeleteActivityFailure(error);
      const showToast = new ShowToast(
        'error',
        error.message,
        'Delete Activity Failure',
      );

      const service = TestBed.get(PlanService);
      spyOn(service, 'deleteActivity').and.returnValue(cold('#|', null, error));
      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcde)', {
        b: loadingBarShow,
        c: failure,
        d: showToast,
        e: loadingBarHide,
      });

      expect(effects.deleteActivity$).toBeObservable(expected);
    });
  });

  describe('deletePlan$', () => {
    it('should register deletePlan$ that dispatches an action', () => {
      expect(metadata.deletePlan$).toEqual({ dispatch: true });
    });

    it('should return a DeletePlanSuccess action with data upon success', () => {
      spyOn(dialog, 'open').and.returnValue({
        afterClosed() {
          return of({ confirm: true });
        },
      });

      const planId = plan.id || '';
      const action = new DeletePlan(planId);
      const success = new DeletePlanSuccess(planId);
      const showToast = new ShowToast(
        'success',
        'Plan has been successfully deleted.',
        'Delete Plan Success',
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcde)', {
        b: loadingBarShow,
        c: success,
        d: showToast,
        e: loadingBarHide,
      });

      expect(effects.deletePlan$).toBeObservable(expected);
    });

    it('should return a DeletePlanFailure action with an error upon failure', () => {
      spyOn(dialog, 'open').and.returnValue({
        afterClosed() {
          return of({ confirm: true });
        },
      });

      const planId = plan.id || '';
      const action = new DeletePlan(planId);
      const error = new Error('DeletePlanFailure');
      const failure = new DeletePlanFailure(error);
      const showToast = new ShowToast(
        'error',
        error.message,
        'Delete Plan Failure',
      );

      const service = TestBed.get(PlanService);
      spyOn(service, 'deletePlan').and.returnValue(cold('#|', null, error));

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcde)', {
        b: loadingBarShow,
        c: failure,
        d: showToast,
        e: loadingBarHide,
      });

      expect(effects.deletePlan$).toBeObservable(expected);
    });
  });

  describe('fetchActivities$', () => {
    it('should register fetchActivities$ that dispatches an action', () => {
      expect(metadata.fetchActivities$).toEqual({ dispatch: true });
    });

    it('should return a FetchActivitiesSuccess action with data upon success', () => {
      const planId = 'someId';
      const activityId = null;
      const action = new FetchActivities(planId, activityId);
      const success = new FetchActivitiesSuccess(
        planId,
        activityId,
        getMockActivities(),
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcd)', {
        b: loadingBarShow,
        c: success,
        d: loadingBarHide,
      });

      expect(effects.fetchActivities$).toBeObservable(expected);
    });

    it('should return a FetchActivitiesFailure action with an error upon failure', () => {
      const planId = 'someOtherId';
      const activityId = null;
      const action = new FetchActivities(planId, activityId);
      const error = new Error('FetchActivitiesFailure');
      const failure = new FetchActivitiesFailure(error);

      const service = TestBed.get(PlanService);
      spyOn(service, 'getActivities').and.returnValue(cold('#|', null, error));

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcd)', {
        b: loadingBarShow,
        c: failure,
        d: loadingBarHide,
      });

      expect(effects.fetchActivities$).toBeObservable(expected);
    });
  });

  describe('fetchPlans$', () => {
    it('should register fetchPlans$ that dispatches an action', () => {
      expect(metadata.fetchPlans$).toEqual({ dispatch: true });
    });

    it('should return a FetchPlansSuccess action with data upon success', () => {
      const action = new FetchPlans();
      const success = new FetchPlansSuccess(getMockPlans());

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcd)', {
        b: loadingBarShow,
        c: success,
        d: loadingBarHide,
      });

      expect(effects.fetchPlans$).toBeObservable(expected);
    });

    it('should return a FetchPlansFailure action with an error upon failure', () => {
      const action = new FetchPlans();
      const error = new Error('FetchPlansFailure');
      const failure = new FetchPlansFailure(error);

      const service = TestBed.get(PlanService);
      spyOn(service, 'getPlans').and.returnValue(cold('#|', null, error));

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcd)', {
        b: loadingBarShow,
        c: failure,
        d: loadingBarHide,
      });

      expect(effects.fetchPlans$).toBeObservable(expected);
    });
  });

  describe('updateActivity$', () => {
    it('should register updateActivity$ that dispatches an action', () => {
      expect(metadata.updateActivity$).toEqual({ dispatch: true });
    });

    it('should return a UpdateActivitySuccess action with data on success', () => {
      const planId = plan.id || '';
      const selectedActivityId = 'SetArrayTrackingMode_25788';
      store.dispatch(new FetchPlansSuccess(plans));
      store.dispatch(new FetchAdaptationsSuccess(adaptations));
      store.dispatch(
        new FetchActivitiesSuccess(planId, selectedActivityId, activities),
      );
      activity = activitiesMap[selectedActivityId];

      const action = new UpdateActivity(planId, activity.activityId, activity);

      // Success case should be empty since UpdateActivity patches, and
      // since we just passed in the original unchanged activity, there should
      // be no update.
      const success = new UpdateActivitySuccess(activity.activityId, {});

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.updateActivity$).toBeObservable(expected);
    });

    it('should return a UpdateActivityFailure action with a NoActivities error on failure', () => {
      const action = new UpdateActivity('', '', activity);
      const error = new Error(
        'UpdateActivity: UpdateActivityFailure: NoActivities',
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
      const planId = plan.id || '';
      store.dispatch(new FetchPlansSuccess(plans));
      store.dispatch(new FetchActivitiesSuccess(planId, null, activities));

      const action = new UpdateActivitySuccess('foo', {});
      actions$ = hot('-a', { a: action });
      const expected = cold('-');

      expect(effects.updateActivitySuccess$).toBeObservable(expected);
      expect(mockRouter.navigate).toHaveBeenCalledWith([`/plans/${plan.id}`]);
    });

    it('should not route at all if no selected plan exists', () => {
      const action = new UpdateActivitySuccess('foo', {});

      actions$ = hot('-a', { a: action });
      const expected = cold('-');

      expect(effects.updateActivitySuccess$).toBeObservable(expected);
      expect(mockRouter.navigate).not.toHaveBeenCalledWith([
        `/plans/${plan.id}`,
      ]);
    });
  });
});
