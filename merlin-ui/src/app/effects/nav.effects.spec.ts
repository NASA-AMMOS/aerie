import { HttpClientModule } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Actions } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { RouterNavigatedAction } from '@ngrx/router-store';
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { MerlinActions, ToastActions } from '../actions';
import { RouterState } from '../app-routing.module';
import {
  cActivityInstanceMap,
  cActivityTypeMap,
  cAdaptationMap,
  cPlan,
  cPlanMap,
  planId,
} from '../mocks';
import { ApiMockService, ApiService } from '../services';
import { NavEffects } from './nav.effects';

function getRouterNavigatedAction(url: string, path?: string, params = {}) {
  const action: RouterNavigatedAction<RouterState> = {
    payload: {
      event: {
        id: 1,
        url: `/${url}`,
        urlAfterRedirects: `/${url}`,
      },
      routerState: {
        params,
        path: path || `${url}`,
        queryParams: {},
        url: `/${url}`,
      },
    },
    type: '@ngrx/router-store/navigated',
  };
  return action;
}

describe('nav effects', () => {
  let actions: Observable<Action>;
  let apiService: ApiService;
  let effects: NavEffects;
  let testScheduler: TestScheduler;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientModule],
      providers: [
        NavEffects,
        provideMockActions(() => actions),
        {
          provide: ApiService,
          useValue: new ApiMockService(),
        },
      ],
    });
    apiService = TestBed.inject(ApiService);
    actions = TestBed.inject(Actions);
    effects = TestBed.inject(NavEffects);
    testScheduler = new TestScheduler((actual, expected) => {
      expect(actual).toEqual(expected);
    });
  });

  describe('navAdaptations', () => {
    it('should dispatch the appropriate actions when navigating to /adaptations', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        const action = getRouterNavigatedAction('adaptations');
        actions = hot('-a', { a: action });
        expectObservable(effects.navAdaptations).toBe('-(bcd)', {
          b: MerlinActions.setLoading({ loading: true }),
          c: MerlinActions.setAdaptations({ adaptations: cAdaptationMap }),
          d: MerlinActions.setLoading({ loading: false }),
        });
      });
    });

    it('should dispatch the appropriate actions when navigating to /adaptations and getAdaptations fails', () => {
      testScheduler.run(({ cold, hot, expectObservable }) => {
        const action = getRouterNavigatedAction('adaptations');
        actions = hot('-a', { a: action });
        spyOn(apiService, 'getAdaptations').and.returnValue(
          cold('#|', null, ''),
        );
        expectObservable(effects.navAdaptations).toBe('-(bcd)', {
          b: MerlinActions.setLoading({ loading: true }),
          c: ToastActions.showToast({
            message: 'Fetch adaptations failed',
            toastType: 'error',
          }),
          d: MerlinActions.setLoading({ loading: false }),
        });
      });
    });
  });

  describe('navPlans', () => {
    it('should dispatch the appropriate actions when navigating to /plans', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        const action = getRouterNavigatedAction('plans');
        actions = hot('-a', { a: action });
        expectObservable(effects.navPlans).toBe('-(bcde)', {
          b: MerlinActions.setLoading({ loading: true }),
          c: MerlinActions.setAdaptations({ adaptations: cAdaptationMap }),
          d: MerlinActions.setPlans({ plans: cPlanMap }),
          e: MerlinActions.setLoading({ loading: false }),
        });
      });
    });

    it('should dispatch the appropriate actions when navigating to /plans and getAdaptations and getPlans fails', () => {
      testScheduler.run(({ cold, hot, expectObservable }) => {
        const action = getRouterNavigatedAction('plans');
        actions = hot('-a', { a: action });
        spyOn(apiService, 'getAdaptations').and.returnValue(
          cold('#|', null, ''),
        );
        spyOn(apiService, 'getPlans').and.returnValue(cold('#|', null, ''));
        expectObservable(effects.navPlans).toBe('-(bcde)', {
          b: MerlinActions.setLoading({ loading: true }),
          c: ToastActions.showToast({
            message: 'Fetch adaptations failed',
            toastType: 'error',
          }),
          d: ToastActions.showToast({
            message: 'Fetch plans failed',
            toastType: 'error',
          }),
          e: MerlinActions.setLoading({ loading: false }),
        });
      });
    });
  });

  describe('navPlansWithId', () => {
    it('should dispatch the appropriate actions when navigating to /plans/:id', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        const action = getRouterNavigatedAction(
          `plans/${planId}`,
          'plans/:id',
          { id: planId },
        );
        actions = hot('-a', { a: action });
        expectObservable(effects.navPlansWithId).toBe('-(bcde)', {
          b: MerlinActions.setLoading({ loading: true }),
          c: MerlinActions.setSelectedPlanAndActivityTypes({
            activityTypes: cActivityTypeMap,
            selectedPlan: cPlan,
          }),
          d: MerlinActions.setActivityInstances({
            activityInstances: cActivityInstanceMap,
            planId,
          }),
          e: MerlinActions.setLoading({ loading: false }),
        });
      });
    });

    it(`
      should dispatch the appropriate actions when navigating to /plans/:id
      and getPlanAndActivityTypes and getActivityInstances fails
    `, () => {
      testScheduler.run(({ cold, hot, expectObservable }) => {
        const action = getRouterNavigatedAction(
          `plans/${planId}`,
          'plans/:id',
          { id: planId },
        );
        actions = hot('-a', { a: action });
        spyOn(apiService, 'getPlanAndActivityTypes').and.returnValue(
          cold('#|', null, ''),
        );
        spyOn(apiService, 'getActivityInstances').and.returnValue(
          cold('#|', null, ''),
        );
        expectObservable(effects.navPlansWithId).toBe('-(bcde)', {
          b: MerlinActions.setLoading({ loading: true }),
          c: ToastActions.showToast({
            message: 'Fetch plan and activity types failed',
            toastType: 'error',
          }),
          d: ToastActions.showToast({
            message: 'Fetch activity instances failed',
            toastType: 'error',
          }),
          e: MerlinActions.setLoading({ loading: false }),
        });
      });
    });
  });
});
