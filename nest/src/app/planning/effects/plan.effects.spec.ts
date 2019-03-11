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
import { keyBy } from 'lodash';
import { Observable, of } from 'rxjs';
import { ActivityInstance, Adaptation, Plan } from '../../../../../schemas';
import { reducers as rootReducers } from '../../app-store';
import { ShowToast } from '../../shared/actions/toast.actions';
import { StringTMap } from '../../shared/models';
import { RavenPlanFormDialogData } from '../../shared/models/raven-plan-form-dialog-data';
import {
  AdaptationMockService,
  getMockAdaptations,
} from '../../shared/services/adaptation-mock.service';
import { AdaptationService } from '../../shared/services/adaptation.service';
import {
  getMockActivities,
  getMockPlan,
  getMockPlans,
  PlanMockService,
} from '../../shared/services/plan-mock.service';
import { PlanService } from '../../shared/services/plan.service';
import { FetchAdaptationsSuccess } from '../actions/adaptation.actions';
import { LoadingBarHide, LoadingBarShow } from '../actions/layout.actions';
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
  FetchActivities,
  FetchActivitiesFailure,
  FetchActivitiesSuccess,
  FetchPlans,
  FetchPlansFailure,
  FetchPlansSuccess,
  OpenPlanFormDialog,
  UpdateActivity,
  UpdateActivityFailure,
  UpdateActivitySuccess,
} from '../actions/plan.actions';
import { reducers } from '../planning-store';
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
      const action = new CreateActivity(plan.id || '', activity);
      const success = new CreateActivitySuccess(plan.id || '');
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
      const action = new CreateActivity(plan.id || '', activity);
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
      const action = new DeleteActivity(plan.id || '', activity.activityId);
      const success = new DeleteActivitySuccess();

      actions$ = hot('-a', { a: action });
      const expected = cold('-b', { b: success });

      expect(effects.deleteActivity$).toBeObservable(expected);
    });

    it('should return a DeleteActivityFailure action with an error upon failure', () => {
      const action = new DeleteActivity(plan.id || '', activity.activityId);
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
      const action = new DeletePlan(plan.id || '');
      const success = new DeletePlanSuccess();

      actions$ = hot('-a', { a: action });
      const expected = cold('-b', { b: success });

      expect(effects.deletePlan$).toBeObservable(expected);
    });

    it('should return a DeletePlanFailure action with an error upon failure', () => {
      const action = new DeletePlan(plan.id || '');
      const error = new Error('DeletePlanFailure');
      const failure = new DeletePlanFailure(error);

      const service = TestBed.get(PlanService);
      spyOn(service, 'deletePlan').and.returnValue(cold('-#|', null, error));

      actions$ = hot('-a', { a: action });
      const expected = cold('--b', { b: failure });

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

  describe('openUpdatePlanFormDialog$', () => {
    it('should return a CreatePlan with the new plan and creation flag on save', () => {
      const result: Plan = {
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
      const initial: Plan = {
        adaptationId: 'ops',
        endTimestamp: '1995-12-17T03:28:00',
        id: 'make_sandwich',
        name: 'Make me a sandwich',
        startTimestamp: '1995-12-17T03:24:00',
      };

      // Dispatch to set up initial conditions of the store
      store.dispatch(new FetchPlansSuccess([initial]));

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
      // Manually create an activity of the effects and pass it our initialized classes
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
      const selectedActivityId = 'SetArrayTrackingMode_25788';
      store.dispatch(new FetchPlansSuccess(plans));
      store.dispatch(new FetchAdaptationsSuccess(adaptations));
      store.dispatch(
        new FetchActivitiesSuccess(
          plan.id || '',
          selectedActivityId,
          activities,
        ),
      );
      activity = activitiesMap[selectedActivityId];

      const action = new UpdateActivity(activity.activityId, activity);

      // Success case should be empty since UpdateActivity patches, and
      // since we just passed in the original unchanged activity, there should
      // be no update.
      const success = new UpdateActivitySuccess(activity.activityId, {});

      actions$ = hot('--a-', { a: action });
      const expected = cold('--b', { b: success });

      expect(effects.updateActivity$).toBeObservable(expected);
    });

    it('should return a UpdateActivityFailure action with a NoSelectedPlan error on failure', () => {
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
      store.dispatch(new FetchPlansSuccess(plans));
      store.dispatch(
        new FetchActivitiesSuccess(plan.id || '', null, activities),
      );

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
