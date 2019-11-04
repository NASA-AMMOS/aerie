import { Injectable } from '@angular/core';
import { Actions, createEffect } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { forkJoin } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { mapToParam, ofRoute } from '../../../libs/ngrx-router';
import { MerlinActions } from '../actions';
import { ApiService } from '../services';

@Injectable()
export class NavEffects {
  constructor(private actions: Actions, private apiService: ApiService) {}

  navAdaptations = createEffect(() =>
    this.actions.pipe(
      ofRoute('adaptations'),
      switchMap(_ => forkJoin([this.apiService.setAdaptations()])),
      switchMap((actions: Action[]) => [
        ...actions,
        MerlinActions.setLoading({ loading: false }),
      ]),
    ),
  );

  navPlans = createEffect(() =>
    this.actions.pipe(
      ofRoute('plans'),
      switchMap(_ =>
        forkJoin([
          this.apiService.setAdaptations(),
          this.apiService.setPlans(),
        ]),
      ),
      switchMap((actions: Action[]) => [
        ...actions,
        MerlinActions.setLoading({ loading: false }),
      ]),
    ),
  );

  navPlansWithId = createEffect(() =>
    this.actions.pipe(
      ofRoute('plans/:id'),
      mapToParam<string>('id'),
      switchMap(planId =>
        forkJoin([
          this.apiService.setPlanAndActivityTypes(planId),
          this.apiService.setActivityInstances(planId),
        ]),
      ),
      switchMap((actions: Action[]) => [
        ...actions,
        MerlinActions.setLoading({ loading: false }),
      ]),
    ),
  );
}
