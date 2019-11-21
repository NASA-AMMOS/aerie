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
                    message: 'Activity instance created',
                    toastType: 'success',
                  }),
                  MerlinActions.createActivityInstanceSuccess({
                    activityInstance,
                    activityInstanceId: id,
                    planId,
                  }),
                ];
              }),
              catchError((error: Error) => {
                console.error(error);
                return [
                  ToastActions.showToast({
                    message: 'Create activity instance failed',
                    toastType: 'error',
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
                  message: 'Adaptation created',
                  toastType: 'success',
                }),
                MerlinActions.createAdaptationSuccess({ id, adaptation }),
              ];
            }),
            catchError((error: Error) => {
              console.error(error);
              return [
                ToastActions.showToast({
                  message: 'Create adaptation failed',
                  toastType: 'error',
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
                  message: 'Plan created',
                  toastType: 'success',
                }),
                MerlinActions.createPlanSuccess({ id, plan }),
              ];
            }),
            catchError((error: Error) => {
              console.error(error);
              return [
                ToastActions.showToast({
                  message: 'Create plan failed',
                  toastType: 'error',
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
        activityInstanceId,
        planId,
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
                      message: 'Activity instance deleted',
                      toastType: 'success',
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
                      message: 'Delete activity instance failed',
                      toastType: 'error',
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
                    message: 'Adaptation deleted',
                    toastType: 'success',
                  }),
                  MerlinActions.deleteAdaptationSuccess({ id }),
                ];
              }),
              catchError((error: Error) => {
                console.error(error);
                return [
                  ToastActions.showToast({
                    message: 'Delete adaptation failed',
                    toastType: 'error',
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
                    message: 'Plan deleted',
                    toastType: 'success',
                  }),
                  MerlinActions.deletePlanSuccess({ id }),
                ];
              }),
              catchError((error: Error) => {
                console.error(error);
                return [
                  ToastActions.showToast({
                    message: 'Delete plan failed',
                    toastType: 'error',
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

  resize = createEffect(
    () =>
      this.actions.pipe(
        ofType(MerlinActions.resize),
        switchMap(() => {
          setTimeout(() => dispatchEvent(new Event('resize')));
          return [];
        }),
      ),
    { dispatch: false },
  );

  updateActivityInstance = createEffect(() => {
    return this.actions.pipe(
      ofType(MerlinActions.updateActivityInstance),
      switchMap(({ planId, activityInstanceId, activityInstance }) =>
        concat(
          of(MerlinActions.setLoading({ loading: true })),
          this.apiService
            .updateActivityInstance(
              planId,
              activityInstanceId,
              activityInstance,
            )
            .pipe(
              switchMap(() => {
                return [
                  ToastActions.showToast({
                    message: 'Activity instance updated',
                    toastType: 'success',
                  }),
                  MerlinActions.updateActivityInstanceSuccess({
                    activityInstance,
                    activityInstanceId,
                  }),
                ];
              }),
              catchError((error: Error) => {
                console.error(error);
                return [
                  ToastActions.showToast({
                    message: 'Update activity instance failed',
                    toastType: 'error',
                  }),
                ];
              }),
            ),
          of(MerlinActions.setLoading({ loading: false })),
        ),
      ),
    );
  });
}
