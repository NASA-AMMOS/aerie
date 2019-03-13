/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ActivityInstance, ActivityType, Plan } from '../../../shared/models';
import { NgTemplateUtils } from '../../../shared/util';
import { CreateActivity, UpdateActivity } from '../../actions/plan.actions';
import { PlanningAppState } from '../../planning-store';
import { getSelectedActivity, getSelectedPlan } from '../../selectors';
import { getActivityTypes } from '../../selectors';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-activities',
  styleUrls: ['./activities.component.css'],
  templateUrl: './activities.component.html',
})
export class ActivitiesComponent implements OnDestroy {
  // Adaptation state.
  activityTypes$: Observable<ActivityType[] | null>;

  // Plan state.
  selectedActivity$: Observable<ActivityInstance | null>;
  selectedPlan$: Observable<Plan | null>;

  // Local derived state.
  activityForm: FormGroup;
  selectedActivity: ActivityInstance;
  selectedPlan: Plan | null;

  // Helpers.
  ngTemplateUtils: NgTemplateUtils = new NgTemplateUtils();

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
  get emptyActivity(): ActivityInstance {
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
      y: null,
    };
  }

  /**
   * Initializes the activity form with the selected activity.
   */
  initActivityForm() {
    this.activityForm = this.fb.group({
      activityType: new FormControl(''),
      constraints: this.fb.array(
        this.selectedActivity.constraints.map(constraint =>
          this.fb.group({
            name: new FormControl(constraint.name),
            type: new FormControl(constraint.type),
          }),
        ),
      ),
      duration: new FormControl(this.selectedActivity.duration, [
        Validators.required,
      ]),
      intent: new FormControl(this.selectedActivity.intent),
      name: new FormControl(this.selectedActivity.name, [Validators.required]),
      start: new FormControl(this.selectedActivity.start, [
        Validators.required,
      ]),
    });
  }

  constructor(
    public fb: FormBuilder,
    private store: Store<PlanningAppState>,
    private router: Router,
  ) {
    // Adaptation state.
    this.activityTypes$ = this.store.pipe(select(getActivityTypes));

    // Plan state.
    this.selectedActivity$ = this.store.pipe(select(getSelectedActivity));
    this.selectedPlan$ = this.store.pipe(select(getSelectedPlan));

    this.selectedActivity$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((selectedActivity: ActivityInstance | null) => {
        if (selectedActivity) {
          this.selectedActivity = selectedActivity;
        } else {
          this.selectedActivity = this.emptyActivity;
        }
        this.initActivityForm();
      });

    this.selectedPlan$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((selectedPlan: Plan | null) => {
        this.selectedPlan = selectedPlan;
      });
  }

  ngOnDestroy(): void {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  onNavBack() {
    if (this.selectedPlan) {
      this.router.navigate([`/plans/${this.selectedPlan.id}`]);
    }
  }

  onSubmitActivityForm(value: ActivityInstance) {
    if (this.activityForm.valid && this.selectedPlan && this.selectedPlan.id) {
      if (!this.isNew) {
        this.store.dispatch(
          new UpdateActivity(this.selectedActivity.activityId, value),
        );
      } else {
        this.store.dispatch(new CreateActivity(this.selectedPlan.id, value));
      }
    }
  }
}
