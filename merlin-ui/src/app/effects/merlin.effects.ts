import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { concat, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { version } from '../../environments/version';
import { MerlinActions, ToastActions } from '../actions';
import { AboutDialogComponent, ConfirmDialogComponent } from '../components';
import { ApiService } from '../services';

@Injectable()
export class MerlinEffects {
  constructor(
    private actions: Actions,
    private apiService: ApiService,
    private dialog: MatDialog,
  ) {}

  createActivityInstance = createEffect(() => {
    return this.actions.pipe(
      ofType(MerlinActions.createActivityInstance),
      switchMap(({ planId, activityInstance }) =>
        concat(
          of(MerlinActions.setLoading({ loading: true })),
          this.apiService
            .createActivityInstances(planId, [activityInstance])
            .pipe(
              switchMap(([id]) => {
                return [
                  ToastActions.showToast({
                    toastType: 'success',
                    message: 'Activity instance created successfully',
                  }),
                  MerlinActions.createActivityInstanceSuccess({
                    planId,
                    activityInstanceId: id,
                    activityInstance,
                  }),
                ];
              }),
              catchError((error: Error) => {
                console.error(error);
                return [
                  ToastActions.showToast({
                    toastType: 'error',
                    message: 'Create activity instance failed',
                  }),
                ];
              }),
            ),
          of(MerlinActions.setLoading({ loading: false })),
        ),
      ),
    );
  });

  createAdaptation = createEffect(() => {
    return this.actions.pipe(
      ofType(MerlinActions.createAdaptation),
      switchMap(({ adaptation }) =>
        concat(
          of(MerlinActions.setLoading({ loading: true })),
          this.apiService.createAdaptation(adaptation).pipe(
            switchMap(({ id }) => {
              return [
                ToastActions.showToast({
                  toastType: 'success',
                  message: 'Adaptation created successfully',
                }),
                MerlinActions.createAdaptationSuccess({ id, adaptation }),
              ];
            }),
            catchError((error: Error) => {
              console.error(error);
              return [
                ToastActions.showToast({
                  toastType: 'error',
                  message: 'Create adaptation failed',
                }),
              ];
            }),
          ),
          of(MerlinActions.setLoading({ loading: false })),
        ),
      ),
    );
  });

  createPlan = createEffect(() => {
    return this.actions.pipe(
      ofType(MerlinActions.createPlan),
      switchMap(({ plan }) =>
        concat(
          of(MerlinActions.setLoading({ loading: true })),
          this.apiService.createPlan(plan).pipe(
            switchMap(({ id }) => {
              return [
                ToastActions.showToast({
                  toastType: 'success',
                  message: 'Plan created successfully',
                }),
                MerlinActions.createPlanSuccess({ id, plan }),
              ];
            }),
            catchError((error: Error) => {
              console.error(error);
              return [
                ToastActions.showToast({
                  toastType: 'error',
                  message: 'Create plan failed',
                }),
              ];
            }),
          ),
          of(MerlinActions.setLoading({ loading: false })),
        ),
      ),
    );
  });

  deleteActivityInstance = createEffect(() => {
    return this.actions.pipe(
      ofType(MerlinActions.deleteActivityInstance),
      switchMap(({ planId, activityInstanceId }) => {
        const deleteActivityInstanceDialog = this.dialog.open(
          ConfirmDialogComponent,
          {
            data: {
              cancelText: 'No',
              confirmText: 'Yes',
              message: `Are you sure you want to permanently delete this activity instance?`,
            },
            width: '400px',
          },
        );
        return forkJoin([
          of(planId),
          of(activityInstanceId),
          deleteActivityInstanceDialog.afterClosed(),
        ]);
      }),
      map(([planId, activityInstanceId, result]) => ({
        planId,
        activityInstanceId,
        result,
      })),
      switchMap(({ planId, activityInstanceId, result }) => {
        if (result && result.confirm) {
          return concat(
            of(MerlinActions.setLoading({ loading: true })),
            this.apiService
              .deleteActivityInstance(planId, activityInstanceId)
              .pipe(
                switchMap(() => {
                  return [
                    ToastActions.showToast({
                      toastType: 'success',
                      message: 'Activity instance deleted successfully',
                    }),
                    MerlinActions.deleteActivityInstanceSuccess({
                      activityInstanceId,
                    }),
                  ];
                }),
                catchError((error: Error) => {
                  console.error(error);
                  return [
                    ToastActions.showToast({
                      toastType: 'error',
                      message: 'Delete activity instance failed',
                    }),
                  ];
                }),
              ),
            of(MerlinActions.setLoading({ loading: false })),
          );
        }
        return [];
      }),
    );
  });

  deleteAdaptation = createEffect(() => {
    return this.actions.pipe(
      ofType(MerlinActions.deleteAdaptation),
      switchMap(({ id }) => {
        const deleteAdaptationDialog = this.dialog.open(
          ConfirmDialogComponent,
          {
            data: {
              cancelText: 'No',
              confirmText: 'Yes',
              message: `
                Are you sure you want to permanently delete this adaptation?
                All plans associated with this adaptation will stop working.
              `,
            },
            width: '400px',
          },
        );
        return forkJoin([of(id), deleteAdaptationDialog.afterClosed()]);
      }),
      map(([id, result]) => ({ id, result })),
      switchMap(({ id, result }) => {
        if (result && result.confirm) {
          return concat(
            of(MerlinActions.setLoading({ loading: true })),
            this.apiService.deleteAdaptation(id).pipe(
              switchMap(() => {
                return [
                  ToastActions.showToast({
                    toastType: 'success',
                    message: 'Adaptation deleted successfully',
                  }),
                  MerlinActions.deleteAdaptationSuccess({ id }),
                ];
              }),
              catchError((error: Error) => {
                console.error(error);
                return [
                  ToastActions.showToast({
                    toastType: 'error',
                    message: 'Delete adaptation failed',
                  }),
                ];
              }),
            ),
            of(MerlinActions.setLoading({ loading: false })),
          );
        }
        return [];
      }),
    );
  });

  deletePlan = createEffect(() => {
    return this.actions.pipe(
      ofType(MerlinActions.deletePlan),
      switchMap(({ id }) => {
        const deletePlanDialog = this.dialog.open(ConfirmDialogComponent, {
          data: {
            cancelText: 'No',
            confirmText: 'Yes',
            message: `Are you sure you want to permanently delete this plan?`,
          },
          width: '400px',
        });
        return forkJoin([of(id), deletePlanDialog.afterClosed()]);
      }),
      map(([id, result]) => ({ id, result })),
      switchMap(({ id, result }) => {
        if (result && result.confirm) {
          return concat(
            of(MerlinActions.setLoading({ loading: true })),
            this.apiService.deletePlan(id).pipe(
              switchMap(() => {
                return [
                  ToastActions.showToast({
                    toastType: 'success',
                    message: 'Plan deleted successfully',
                  }),
                  MerlinActions.deletePlanSuccess({ id }),
                ];
              }),
              catchError((error: Error) => {
                console.error(error);
                return [
                  ToastActions.showToast({
                    toastType: 'error',
                    message: 'Delete plan failed',
                  }),
                ];
              }),
            ),
            of(MerlinActions.setLoading({ loading: false })),
          );
        }
        return [];
      }),
    );
  });

  openAboutDialog = createEffect(
    () => {
      return this.actions.pipe(
        ofType(MerlinActions.openAboutDialog),
        switchMap(() => {
          const { packageJsonName, packageJsonVersion, tag } = version;
          this.dialog.open(AboutDialogComponent, {
            data: {
              version: `${packageJsonName} ${packageJsonVersion} - ${tag}`,
            },
            width: `500px`,
          });

          return [];
        }),
      );
    },
    { dispatch: false },
  );
}
