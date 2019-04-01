/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { TestBed } from '@angular/core/testing';
import { EffectsMetadata, getEffectsMetadata } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { cold, hot } from 'jasmine-marbles';
import { Observable } from 'rxjs';
import { RouterNavigation } from '../../../../libs/ngrx-router';
import {
  FetchActivityTypes,
  FetchAdaptations,
} from '../actions/adaptation.actions';
import {
  ClearSelectedActivity,
  FetchActivities,
  FetchPlans,
  SelectActivity,
  SelectPlan,
} from '../actions/plan.actions';
import { NavEffects } from './nav.effects';

describe('NavEffects', () => {
  let actions$: Observable<any>;
  let effects: NavEffects;
  let metadata: EffectsMetadata<NavEffects>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [NavEffects, provideMockActions(() => actions$)],
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
      const expected = cold('-(bc)', {
        b: new FetchPlans(),
        c: new FetchAdaptations(),
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
      const expected = cold('-(bcdef)', {
        b: new FetchPlans(),
        c: new FetchAdaptations(),
        d: new FetchActivityTypes(planId),
        e: new FetchActivities(planId, null),
        f: new SelectPlan(planId),
      });

      expect(effects.navPlansWithId$).toBeObservable(expected);
    });
  });

  describe('navActivities$', () => {
    it('should register navActivities$ that dispatches an action', () => {
      expect(metadata.navActivities$).toEqual({ dispatch: true });
    });

    it('should dispatch the appropriate actions when navigating to plans/:planId/activity', () => {
      const planId = '42';
      const action = new RouterNavigation({
        params: { planId },
        path: 'plans/:planId/activity',
      });

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcdef)', {
        b: new FetchPlans(),
        c: new FetchAdaptations(),
        d: new FetchActivityTypes(planId),
        e: new SelectPlan(planId),
        f: new ClearSelectedActivity(),
      });

      expect(effects.navActivities$).toBeObservable(expected);
    });
  });

  describe('navActivitiesWithId$', () => {
    it('should register navActivitiesWithId$ that dispatches an action', () => {
      expect(metadata.navActivitiesWithId$).toEqual({ dispatch: true });
    });

    it('should dispatch the appropriate actions when navigating to plans/:planId/activity/:activityId', () => {
      const planId = '42';
      const activityId = '52';
      const action = new RouterNavigation({
        params: { activityId, planId },
        path: 'plans/:planId/activity/:activityId',
      });

      actions$ = hot('-a', { a: action });
      const expected = cold('-(bcdefg)', {
        b: new FetchPlans(),
        c: new FetchAdaptations(),
        d: new FetchActivityTypes(planId),
        e: new FetchActivities(planId, activityId),
        f: new SelectPlan(planId),
        g: new SelectActivity(activityId),
      });

      expect(effects.navActivitiesWithId$).toBeObservable(expected);
    });
  });
});
