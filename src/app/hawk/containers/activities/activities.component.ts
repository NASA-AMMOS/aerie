/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { ActivatedRoute, Data, NavigationEnd, Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { HawkAppState } from '../../hawk-store';
import { getSelectedPlan } from '../../selectors';
import { getActivityTypes } from '../../selectors/adaptation.selector';

import {
  ClearSelectedPlan,
  CreateActivity,
  FetchPlanDetailSuccess,
  FetchPlanListFailure,
  FetchPlanListSuccess,
  UpdateActivity,
} from '../../actions/plan.actions';

import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';

import {
  RavenActivity,
  RavenActivityType,
  RavenPlan,
} from '../../../shared/models';

import {
  FetchActivityTypes,
  FetchAdaptationListFailure,
  FetchAdaptationListSuccess,
} from '../../actions/adaptation.actions';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-activities',
  styleUrls: ['./activities.component.css'],
  templateUrl: './activities.component.html',
})
export class ActivitiesComponent implements OnDestroy {
  activityTypes$: Observable<RavenActivityType[] | null>;
  selectedPlan$: Observable<RavenPlan | null>;

  activityForm: FormGroup;
  selectedActivity: RavenActivity;
  selectedPlan: RavenPlan | null;

  private ngUnsubscribe: Subject<{}> = new Subject();

  /**
   * Return true if the selected activity is new or existing.
   * We determine this by looking at the activityId.
   * If the activityId is empty then we assume the selected activity is new.
   */
  get isNew() {
    return this.selectedActivity && this.selectedActivity.activityId === '';
  }

  /**
   * Helper that just returns a new empty activity.
   */
  get emptyActivity(): RavenActivity {
    return {
      activityId: '',
      activityType: '',
      color: '',
      constraints: [],
      duration: 0,
      end: 0,
      endTimestamp: '',
      intent: '',
      name: '',
      parameters: [],
      start: 0,
      startTimestamp: '',
      subActivityIds: [],
      y: null,
    };
  }

  constructor(
    public fb: FormBuilder,
    private store: Store<HawkAppState>,
    private router: Router,
    private route: ActivatedRoute,
  ) {
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(() => {
        this.resolveActions(this.route.snapshot.data);

        this.activityForm = fb.group({
          activityType: new FormControl(''),
          constraints: fb.array(
            this.selectedActivity.constraints.map(constraint =>
              fb.group({
                default: new FormControl(constraint.default),
                locked: new FormControl(constraint.locked),
                name: new FormControl(constraint.name),
                type: new FormControl(constraint.type),
                value: new FormControl(constraint.value),
                values: new FormControl(constraint.values),
              }),
            ),
          ),
          duration: new FormControl(this.selectedActivity.duration, [
            Validators.required,
          ]),
          intent: new FormControl(this.selectedActivity.intent),
          name: new FormControl(this.selectedActivity.name, [
            Validators.required,
          ]),
          start: new FormControl(this.selectedActivity.start, [
            Validators.required,
          ]),
        });
      });

    this.activityTypes$ = this.store.pipe(select(getActivityTypes));
    this.selectedPlan$ = this.store.pipe(select(getSelectedPlan));

    this.selectedPlan$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((selPlan: RavenPlan | null) => {
        this.selectedPlan = selPlan;
      });
  }

  ngOnDestroy(): void {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Emit actions based on router snapshot data from a resolver.
   * This is so we can easily sync URL state,
   * HTTP responses (i.e. data) from a resolver, and the store.
   */
  resolveActions(data: Data): void {
    const { adaptations, plans, selectedActivity, selectedPlan } = data;

    // Note that selectedActivity is transient state (i.e. it's not kept in the store for this page).
    if (selectedActivity) {
      this.selectedActivity = selectedActivity;
    } else {
      this.selectedActivity = this.emptyActivity;
    }

    if (adaptations) {
      this.store.dispatch(new FetchAdaptationListSuccess(adaptations));
    } else {
      this.store.dispatch(
        new FetchAdaptationListFailure(
          new Error('Failed to fetch adaptations list.'),
        ),
      );
    }

    if (plans) {
      this.store.dispatch(new FetchPlanListSuccess(plans));
    } else {
      this.store.dispatch(
        new FetchPlanListFailure(new Error('Failed to fetch plan list.')),
      );
    }

    if (selectedPlan) {
      this.store.dispatch(new FetchPlanDetailSuccess(selectedPlan));
      this.store.dispatch(new FetchActivityTypes(selectedPlan.adaptationId));
    } else {
      this.store.dispatch(new ClearSelectedPlan());
    }
  }

  onNavBack() {
    if (this.selectedPlan) {
      this.router.navigate([`/plans/${this.selectedPlan.id}`]);
    }
  }

  onSubmitActivityForm(value: RavenActivity) {
    if (this.activityForm.valid && this.selectedPlan) {
      if (!this.isNew) {
        this.store.dispatch(
          new UpdateActivity(this.selectedActivity.activityId, value),
        );
      } else {
        this.store.dispatch(new CreateActivity(this.selectedPlan.id, value));
      }
    }
  }

  /**
   * Determine which select item to select
   * @see https://angular.io/api/forms/SelectControlValueAccessor#caveat-option-selection
   * @param a An object with an id property
   * @param b An object with an id property
   *
   * TODO: THIS IS DUPLICATED INSIDE hawk-app.component. CONSOLIDATE.
   */
  compareSelectValues(a: any, b: any) {
    return a.id === b.id;
  }
}
