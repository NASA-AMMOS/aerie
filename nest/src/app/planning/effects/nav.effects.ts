/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Actions, createEffect } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { forkJoin } from 'rxjs';
import { map, switchMap, withLatestFrom } from 'rxjs/operators';
import { mapToParam, mapToParams, ofRoute } from '../../../../libs/ngrx-router';
import { LayoutActions, PlanActions } from '../actions';
import { PlanningAppState } from '../planning-store';
import { AdaptationService } from '../services/adaptation.service';
import { PlanService } from '../services/plan.service';

@Injectable()
export class NavEffects {
  constructor(
    private actions: Actions,
    private store: Store<PlanningAppState>,
    private adaptationService: AdaptationService,
    private planService: PlanService,
  ) {}

  navPlans = createEffect(() =>
    this.actions.pipe(
      ofRoute('plans'),
      withLatestFrom(this.store),
      map(([_, state]) => state),
      switchMap(state =>
        forkJoin([
          this.adaptationService.getAdaptationsWithActions(
            state.config.app.adaptationServiceBaseUrl,
          ),
          this.planService.getPlansWithActions(
            state.config.app.planServiceBaseUrl,
            null,
          ),
        ]),
      ),
      switchMap((actions: Action[]) => [
        ...actions,
        LayoutActions.closeAllDrawers(),
        PlanActions.clearSelectedPlan(),
        PlanActions.clearSelectedActivity(),
        LayoutActions.loadingBarHide(),
      ]),
    ),
  );

  navPlansWithId = createEffect(() =>
    this.actions.pipe(
      ofRoute('plans/:planId'),
      mapToParam<string>('planId'),
      withLatestFrom(this.store),
      map(([planId, state]) => ({ planId, state })),
      switchMap(({ planId, state }) =>
        forkJoin([
          this.adaptationService.getActivityTypesWithActions(
            state.config.app.planServiceBaseUrl,
            state.config.app.adaptationServiceBaseUrl,
            planId,
          ),
          this.planService.getPlansWithActions(
            state.config.app.planServiceBaseUrl,
            planId,
          ),
          this.planService.getActivitiesWithActions(
            state.config.app.planServiceBaseUrl,
            planId,
            null,
          ),
        ]),
      ),
      switchMap((actions: Action[]) => [
        ...actions,
        LayoutActions.closeAllDrawers(),
        PlanActions.clearSelectedActivity(),
        LayoutActions.loadingBarHide(),
      ]),
    ),
  );

  navActivity = createEffect(() =>
    this.actions.pipe(
      ofRoute('plans/:planId/activity'),
      mapToParam<string>('planId'),
      withLatestFrom(this.store),
      map(([planId, state]) => ({ planId, state })),
      switchMap(({ planId, state }) =>
        forkJoin([
          this.adaptationService.getActivityTypesWithActions(
            state.config.app.planServiceBaseUrl,
            state.config.app.adaptationServiceBaseUrl,
            planId,
          ),
          this.planService.getPlansWithActions(
            state.config.app.planServiceBaseUrl,
            planId,
          ),
        ]),
      ),
      switchMap((actions: Action[]) => [
        ...actions,
        LayoutActions.closeAllDrawers(),
        PlanActions.clearSelectedActivity(),
        LayoutActions.loadingBarHide(),
      ]),
    ),
  );

  navActivityWithId = createEffect(() =>
    this.actions.pipe(
      ofRoute('plans/:planId/activity/:activityId'),
      mapToParams(),
      withLatestFrom(this.store),
      map(([params, state]) => ({ params, state })),
      switchMap(({ params: { activityId, planId }, state }) =>
        forkJoin([
          this.adaptationService.getActivityTypesWithActions(
            state.config.app.planServiceBaseUrl,
            state.config.app.adaptationServiceBaseUrl,
            planId,
          ),
          this.planService.getPlansWithActions(
            state.config.app.planServiceBaseUrl,
            planId,
          ),
          this.planService.getActivitiesWithActions(
            state.config.app.planServiceBaseUrl,
            planId,
            activityId,
          ),
        ]),
      ),
      switchMap((actions: Action[]) => [
        ...actions,
        LayoutActions.closeAllDrawers(),
        LayoutActions.loadingBarHide(),
      ]),
    ),
  );
}
