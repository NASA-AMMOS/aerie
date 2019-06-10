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
import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { addMatchers, cold, hot, initTestScheduler } from 'jasmine-marbles';
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
import { SetAdaptations } from '../actions/adaptation.actions';
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
  SetActivities,
  SetPlans,
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
  PlanMockService,
} from '../services/plan-mock.service';
import { PlanService } from '../services/plan.service';
import { PlanEffects } from './plan.effects';

describe('PlanEffects', () => {
  let actions$: Observable<any>;
  let effects: PlanEffects;
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

    initTestScheduler();
    addMatchers();

    effects = TestBed.get(PlanEffects);
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
    it('should return a CreateActivitySuccess action with data upon success', () => {
      const planId = plan.id || '';
      const action = new CreateActivity(planId, activity);
      const success = new CreateActivitySuccess(planId, {
        ...activity,
        constraints: [],
        listeners: [],
        parameters: [],
        y: 0,
      });
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
    it('should navigate to the actions given planId', () => {
      const action = new CreateActivitySuccess('foo', activity);

      actions$ = hot('-a', { a: action });
      const expected = cold('-');

      expect(effects.createActivitySuccess$).toBeObservable(expected);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/plans/foo']);
    });
  });

  describe('createPlan$', () => {
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

  describe('updateActivity$', () => {
    it('should return a UpdateActivitySuccess action with data on success', () => {
      // Setup.
      const planId = plan.id || '';
      const selectedActivityId = 'SetArrayTrackingMode_25788';
      store.dispatch(new SetPlans(plans));
      store.dispatch(new SetAdaptations(adaptations));
      store.dispatch(new SetActivities(activities));
      activity = activitiesMap[selectedActivityId];

      // Test.
      const action = new UpdateActivity(planId, activity.activityId, activity);
      const success = new UpdateActivitySuccess(activity.activityId, activity);
      const showToast = new ShowToast(
        'success',
        'Activity has been successfully updated.',
        'Update Activity Success',
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcde)', {
        b: loadingBarShow,
        c: success,
        d: showToast,
        e: loadingBarHide,
      });

      expect(effects.updateActivity$).toBeObservable(expected);
    });

    it('should return a UpdateActivityFailure action with a NoActivities error on failure', () => {
      // Setup.
      const planId = plan.id || '';
      const selectedActivityId = 'SetArrayTrackingMode_25788';
      store.dispatch(new SetPlans(plans));
      store.dispatch(new SetAdaptations(adaptations));
      store.dispatch(new SetActivities(activities));
      activity = activitiesMap[selectedActivityId];

      // Test.
      const action = new UpdateActivity(planId, activity.activityId, activity);
      const error = new Error('UpdateActivity: UpdateActivityFailure');
      const failure = new UpdateActivityFailure(error);
      const showToast = new ShowToast(
        'error',
        error.message,
        'Update Activity Failure',
      );

      const service = TestBed.get(PlanService);
      spyOn(service, 'updateActivity').and.returnValue(cold('#|', null, error));

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcde)', {
        b: loadingBarShow,
        c: failure,
        d: showToast,
        e: loadingBarHide,
      });

      expect(effects.updateActivity$).toBeObservable(expected);
    });
  });
});
