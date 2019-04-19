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
import { EffectsMetadata, getEffectsMetadata } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { cold, hot } from 'jasmine-marbles';
import { Observable, of } from 'rxjs';
import { RouterNavigation } from '../../../../libs/ngrx-router';
import { reducers as rootReducers } from '../../app-store';
import {
  ActivityInstance,
  ActivityType,
  Adaptation,
  Plan,
} from '../../shared/models';
import {
  FetchActivityTypesFailure,
  FetchAdaptationsFailure,
  SetActivityTypes,
  SetAdaptations,
} from '../actions/adaptation.actions';
import { CloseAllDrawers, LoadingBarHide } from '../actions/layout.actions';
import {
  ClearSelectedActivity,
  ClearSelectedPlan,
  FetchActivitiesFailure,
  FetchPlansFailure,
  SetActivities,
  SetActivitiesAndSelectedActivity,
  SetPlans,
  SetPlansAndSelectedPlan,
} from '../actions/plan.actions';
import { reducers } from '../planning-store';
import {
  AdaptationMockService,
  getMockActivityTypes,
  getMockAdaptations,
} from '../services/adaptation-mock.service';
import { AdaptationService } from '../services/adaptation.service';
import {
  getMockActivities,
  getMockPlans,
  PlanMockService,
} from '../services/plan-mock.service';
import { PlanService } from '../services/plan.service';
import { NavEffects } from './nav.effects';

describe('NavEffects', () => {
  let actions$: Observable<any>;
  let effects: NavEffects;
  let metadata: EffectsMetadata<NavEffects>;

  // Mock data.
  const activities: ActivityInstance[] = getMockActivities();
  const activityTypes: ActivityType[] = getMockActivityTypes();
  const adaptations: Adaptation[] = getMockAdaptations();
  const plans: Plan[] = getMockPlans();

  // Failure actions.
  const activitiesFailure = new FetchActivitiesFailure(
    new Error('FetchActivitiesFailure'),
  );
  const activityTypesFailure = new FetchActivityTypesFailure(
    new Error('FetchActivityTypesFailure'),
  );
  const adaptationFailure = new FetchAdaptationsFailure(
    new Error('FetchAdaptationsFailure'),
  );
  const plansFailure = new FetchPlansFailure(new Error('FetchPlansFailure'));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientModule,
        StoreModule.forRoot(rootReducers),
        StoreModule.forFeature('planning', reducers),
      ],
      providers: [
        NavEffects,
        HttpClient,
        Store,
        provideMockActions(() => actions$),
        {
          provide: PlanService,
          useValue: new PlanMockService(),
        },
        {
          provide: AdaptationService,
          useValue: new AdaptationMockService(),
        },
      ],
    });

    effects = TestBed.get(NavEffects);
    metadata = getEffectsMetadata(effects);
  });

  describe('navPlans$', () => {
    it('should register navPlans$ that dispatches an action', () => {
      expect(metadata.navPlans$).toEqual({ dispatch: true });
    });

    it('should dispatch the appropriate actions when navigating to /plans', () => {
      const action = new RouterNavigation({ path: 'plans' });

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcdefg)', {
        b: new SetAdaptations(adaptations),
        c: new SetPlans(plans),
        d: new CloseAllDrawers(),
        e: new ClearSelectedPlan(),
        f: new ClearSelectedActivity(),
        g: new LoadingBarHide(),
      });

      expect(effects.navPlans$).toBeObservable(expected);
    });

    it('should dispatch the appropriate actions when navigating to /plans when getting adaptations and plans fails', () => {
      const action = new RouterNavigation({ path: 'plans' });

      const adaptationService = TestBed.get(AdaptationService);
      spyOn(adaptationService, 'getAdaptationsWithActions').and.returnValue(
        of(adaptationFailure),
      );
      const planService = TestBed.get(PlanService);
      spyOn(planService, 'getPlansWithActions').and.returnValue(
        of(plansFailure),
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcdefg)', {
        b: adaptationFailure,
        c: plansFailure,
        d: new CloseAllDrawers(),
        e: new ClearSelectedPlan(),
        f: new ClearSelectedActivity(),
        g: new LoadingBarHide(),
      });

      expect(effects.navPlans$).toBeObservable(expected);
    });
  });

  describe('navPlansWithId$', () => {
    it('should register navPlansWithId$ that dispatches an action', () => {
      expect(metadata.navPlansWithId$).toEqual({ dispatch: true });
    });

    it('should dispatch the appropriate actions when navigating to plans/:planId', () => {
      const planId = '42';
      const action = new RouterNavigation({
        params: { planId },
        path: 'plans/:planId',
      });

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcdefg)', {
        b: new SetActivityTypes(activityTypes),
        c: new SetPlansAndSelectedPlan(plans, planId),
        d: new SetActivities(activities),
        e: new CloseAllDrawers(),
        f: new ClearSelectedActivity(),
        g: new LoadingBarHide(),
      });

      expect(effects.navPlansWithId$).toBeObservable(expected);
    });

    it('should dispatch the appropriate actions when navigating to plans/:planId when getting activity types, plans, and activities fails', () => {
      const planId = '42';
      const action = new RouterNavigation({
        params: { planId },
        path: 'plans/:planId',
      });

      const adaptationService = TestBed.get(AdaptationService);
      spyOn(adaptationService, 'getActivityTypesWithActions').and.returnValue(
        of(activityTypesFailure),
      );

      const planService = TestBed.get(PlanService);
      spyOn(planService, 'getPlansWithActions').and.returnValue(
        of(plansFailure),
      );
      spyOn(planService, 'getActivitiesWithActions').and.returnValue(
        of(activitiesFailure),
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcdefg)', {
        b: activityTypesFailure,
        c: plansFailure,
        d: activitiesFailure,
        e: new CloseAllDrawers(),
        f: new ClearSelectedActivity(),
        g: new LoadingBarHide(),
      });

      expect(effects.navPlansWithId$).toBeObservable(expected);
    });
  });

  describe('navActivity$', () => {
    it('should register navActivity$ that dispatches an action', () => {
      expect(metadata.navActivity$).toEqual({ dispatch: true });
    });

    it('should dispatch the appropriate actions when navigating to plans/:planId/activity', () => {
      const planId = '42';
      const action = new RouterNavigation({
        params: { planId },
        path: 'plans/:planId/activity',
      });

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcdef)', {
        b: new SetActivityTypes(activityTypes),
        c: new SetPlansAndSelectedPlan(plans, planId),
        d: new CloseAllDrawers(),
        e: new ClearSelectedActivity(),
        f: new LoadingBarHide(),
      });

      expect(effects.navActivity$).toBeObservable(expected);
    });

    it('should dispatch the appropriate actions when navigating to plans/:planId/activity when getting activity types and plans fails', () => {
      const planId = '42';
      const action = new RouterNavigation({
        params: { planId },
        path: 'plans/:planId/activity',
      });

      const adaptationService = TestBed.get(AdaptationService);
      spyOn(adaptationService, 'getActivityTypesWithActions').and.returnValue(
        of(activityTypesFailure),
      );

      const planService = TestBed.get(PlanService);
      spyOn(planService, 'getPlansWithActions').and.returnValue(
        of(plansFailure),
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcdef)', {
        b: activityTypesFailure,
        c: plansFailure,
        d: new CloseAllDrawers(),
        e: new ClearSelectedActivity(),
        f: new LoadingBarHide(),
      });

      expect(effects.navActivity$).toBeObservable(expected);
    });
  });

  describe('navActivityWithId$', () => {
    it('should register navActivityWithId$ that dispatches an action', () => {
      expect(metadata.navActivityWithId$).toEqual({ dispatch: true });
    });

    it('should dispatch the appropriate actions when navigating to plans/:planId/activity/:activityId', () => {
      const planId = '42';
      const activityId = '52';
      const action = new RouterNavigation({
        params: { activityId, planId },
        path: 'plans/:planId/activity/:activityId',
      });

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcdef)', {
        b: new SetActivityTypes(activityTypes),
        c: new SetPlansAndSelectedPlan(plans, planId),
        d: new SetActivitiesAndSelectedActivity(activities, activityId),
        e: new CloseAllDrawers(),
        f: new LoadingBarHide(),
      });

      expect(effects.navActivityWithId$).toBeObservable(expected);
    });

    it('should dispatch the appropriate actions when navigating to plans/:planId/activity/:activityId when getting activity types, plans, and activities fails', () => {
      const planId = '42';
      const activityId = '52';
      const action = new RouterNavigation({
        params: { activityId, planId },
        path: 'plans/:planId/activity/:activityId',
      });

      const adaptationService = TestBed.get(AdaptationService);
      spyOn(adaptationService, 'getActivityTypesWithActions').and.returnValue(
        of(activityTypesFailure),
      );

      const planService = TestBed.get(PlanService);
      spyOn(planService, 'getPlansWithActions').and.returnValue(
        of(plansFailure),
      );
      spyOn(planService, 'getActivitiesWithActions').and.returnValue(
        of(activitiesFailure),
      );

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcdef)', {
        b: activityTypesFailure,
        c: plansFailure,
        d: activitiesFailure,
        e: new CloseAllDrawers(),
        f: new LoadingBarHide(),
      });

      expect(effects.navActivityWithId$).toBeObservable(expected);
    });
  });
});
