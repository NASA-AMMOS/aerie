import { Injectable } from '@angular/core';
import { Actions, createEffect } from '@ngrx/effects';
import { concat, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { mapToParam, ofRoute } from '../../../libs/ngrx-router';
import { MerlinActions, ToastActions } from '../actions';
import { ApiService } from '../services';

@Injectable()
export class NavEffects {
  constructor(private actions: Actions, private apiService: ApiService) {}

  navAdaptations = createEffect(() =>
    this.actions.pipe(
      ofRoute('adaptations'),
      switchMap(_ =>
        concat(
          of(MerlinActions.setLoading({ loading: true })),
          this.apiService.getAdaptations().pipe(
            map(adaptations => MerlinActions.setAdaptations({ adaptations })),
            catchError((error: Error) => {
              console.error(error);
              return [
                ToastActions.showToast({
                  toastType: 'error',
                  message: 'Fetch adaptations failed. Error logged in console.',
                }),
              ];
            }),
          ),
          of(MerlinActions.setLoading({ loading: false })),
        ),
      ),
    ),
  );

  navPlans = createEffect(() =>
    this.actions.pipe(
      ofRoute('plans'),
      switchMap(_ =>
        concat(
          of(MerlinActions.setLoading({ loading: true })),
          this.apiService.getAdaptations().pipe(
            map(adaptations => MerlinActions.setAdaptations({ adaptations })),
            catchError((error: Error) => {
              console.error(error);
              return [
                ToastActions.showToast({
                  toastType: 'error',
                  message: 'Fetch adaptations failed. Error logged in console.',
                }),
              ];
            }),
          ),
          this.apiService.getPlans().pipe(
            map(plans => MerlinActions.setPlans({ plans })),
            catchError((error: Error) => {
              console.error(error);
              return [
                ToastActions.showToast({
                  toastType: 'error',
                  message: 'Fetch plans failed. Error logged in console.',
                }),
              ];
            }),
          ),
          of(MerlinActions.setLoading({ loading: false })),
        ),
      ),
    ),
  );

  navPlansWithId = createEffect(() =>
    this.actions.pipe(
      ofRoute('plans/:id'),
      mapToParam<string>('id'),
      switchMap(planId =>
        concat(
          of(MerlinActions.setLoading({ loading: true })),
          this.apiService.getPlanAndActivityTypes(planId).pipe(
            map(({ activityTypes, plan }) =>
              MerlinActions.setSelectedPlanAndActivityTypes({
                activityTypes,
                selectedPlan: plan,
              }),
            ),
            catchError((error: Error) => {
              console.error(error);
              return [
                ToastActions.showToast({
                  toastType: 'error',
                  message:
                    'Fetch plan and activity types failed. Error logged in console.',
                }),
              ];
            }),
          ),
          this.apiService.getActivityInstances(planId).pipe(
            map(activityInstances =>
              MerlinActions.setActivityInstances({ planId, activityInstances }),
            ),
            catchError((error: Error) => {
              console.error(error);
              return [
                ToastActions.showToast({
                  toastType: 'error',
                  message:
                    'Fetch activity instances failed. Error logged in console.',
                }),
              ];
            }),
          ),
          of(MerlinActions.setLoading({ loading: false })),
        ),
      ),
    ),
  );
}
