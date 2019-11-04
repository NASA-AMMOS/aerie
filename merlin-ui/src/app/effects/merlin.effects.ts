import { Injectable } from '@angular/core';
import { MatDialog, MatSnackBar } from '@angular/material';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { concat, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { MerlinActions } from '../actions';
import { ConfirmDialogComponent } from '../components';
import { ApiService } from '../services';

@Injectable()
export class MerlinEffects {
  constructor(
    private actions: Actions,
    private apiService: ApiService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
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
              map(([id]) => {
                this.snackBar.open(
                  'Activity instance created successfully!',
                  'Ok',
                  {
                    duration: 3000,
                  },
                );
                return MerlinActions.createActivityInstanceSuccess({
                  planId,
                  activityInstanceId: id,
                  activityInstance,
                });
              }),
              catchError((error: Error) => {
                console.error(error);
                this.snackBar.open(
                  'Oops! Activity instance could not be created. Error logged in console.',
                  'Ok',
                  {
                    duration: 3000,
                  },
                );
                return [];
              }),
            ),
          of(MerlinActions.setLoading({ loading: false })),
        ),
      ),
    );
  });

  createAdaptation = createEffect(() =>
    this.actions.pipe(
      ofType(MerlinActions.createAdaptation),
      switchMap(({ adaptation }) =>
        concat(
          of(MerlinActions.setLoading({ loading: true })),
          this.apiService.createAdaptation(adaptation).pipe(
            map(({ id }) => {
              this.snackBar.open('Adaptation created successfully!', 'Ok', {
                duration: 3000,
              });
              return MerlinActions.createAdaptationSuccess({ id, adaptation });
            }),
            catchError((error: Error) => {
              console.error(error);
              this.snackBar.open(
                'Oops! Adaptation could not be created. Error logged in console.',
                'Ok',
                {
                  duration: 3000,
                },
              );
              return [];
            }),
          ),
          of(MerlinActions.setLoading({ loading: false })),
        ),
      ),
    ),
  );

  createPlan = createEffect(() =>
    this.actions.pipe(
      ofType(MerlinActions.createPlan),
      switchMap(({ plan }) =>
        concat(
          of(MerlinActions.setLoading({ loading: true })),
          this.apiService.createPlan(plan).pipe(
            map(({ id }) => {
              this.snackBar.open('Plan created successfully!', 'Ok', {
                duration: 3000,
              });
              return MerlinActions.createPlanSuccess({ id, plan });
            }),
            catchError((error: Error) => {
              console.error(error);
              this.snackBar.open(
                'Oops! Plan could not be created. Error logged in console.',
                'Ok',
                {
                  duration: 3000,
                },
              );
              return [];
            }),
          ),
          of(MerlinActions.setLoading({ loading: false })),
        ),
      ),
    ),
  );

  deleteActivityInstance = createEffect(() =>
    this.actions.pipe(
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
                map(() => {
                  this.snackBar.open(
                    'Activity instance deleted successfully!',
                    'Ok',
                    {
                      duration: 3000,
                    },
                  );
                  return MerlinActions.deleteActivityInstanceSuccess({
                    activityInstanceId,
                  });
                }),
                catchError((error: Error) => {
                  console.error(error);
                  this.snackBar.open(
                    'Oops! Activity instance could not be deleted. Error logged in console.',
                    'Ok',
                    {
                      duration: 3000,
                    },
                  );
                  return [];
                }),
              ),
            of(MerlinActions.setLoading({ loading: false })),
          );
        }
        return [];
      }),
    ),
  );

  deleteAdaptation = createEffect(() =>
    this.actions.pipe(
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
              map(() => {
                this.snackBar.open('Adaptation deleted successfully!', 'Ok', {
                  duration: 3000,
                });
                return MerlinActions.deleteAdaptationSuccess({ id });
              }),
              catchError((error: Error) => {
                console.error(error);
                this.snackBar.open(
                  'Oops! Adaptation could not be deleted. Error logged in console.',
                  'Ok',
                  {
                    duration: 3000,
                  },
                );
                return [];
              }),
            ),
            of(MerlinActions.setLoading({ loading: false })),
          );
        }
        return [];
      }),
    ),
  );

  deletePlan = createEffect(() =>
    this.actions.pipe(
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
              map(() => {
                this.snackBar.open('Plan deleted successfully!', 'Ok', {
                  duration: 3000,
                });
                return MerlinActions.deletePlanSuccess({ id });
              }),
              catchError((error: Error) => {
                console.error(error);
                this.snackBar.open(
                  'Oops! Plan could not be deleted. Error logged in console.',
                  'Ok',
                  {
                    duration: 3000,
                  },
                );
                return [];
              }),
            ),
            of(MerlinActions.setLoading({ loading: false })),
          );
        }
        return [];
      }),
    ),
  );
}
