import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { SubSink } from 'subsink';
import { MerlinActions } from '../../actions';
import { AppState } from '../../app-store';
import { getAdaptations, getPlans } from '../../selectors';
import { CAdaptation, CPlan, SPlan } from '../../types';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-plans',
  styleUrls: ['./plans.component.css'],
  templateUrl: './plans.component.html',
})
export class PlansComponent implements AfterViewInit, OnDestroy {
  @ViewChild('inputCreatePlanName')
  inputCreatePlanName: ElementRef;

  adaptations: CAdaptation[] | null = null;
  createPlanForm: FormGroup;
  plans: CPlan[] | null = null;
  selectedAdaptationId = '';

  private subs = new SubSink();

  constructor(
    private fb: FormBuilder,
    private ref: ChangeDetectorRef,
    private router: Router,
    private store: Store<AppState>,
  ) {
    const { state } = this.router.getCurrentNavigation().extras;
    if (state && state.adaptationId) {
      this.selectedAdaptationId = state.adaptationId;
    }

    this.createPlanForm = this.fb.group({
      adaptationId: [this.selectedAdaptationId, Validators.required],
      endTimestamp: ['', Validators.required],
      name: ['', Validators.required],
      startTimestamp: ['', Validators.required],
    });

    this.subs.add(
      this.store.pipe(select(getAdaptations)).subscribe(adaptations => {
        this.adaptations = adaptations;
        this.ref.markForCheck();
      }),
      this.store.pipe(select(getPlans)).subscribe(plans => {
        this.plans = plans;
        this.ref.markForCheck();
      }),
    );
  }

  ngAfterViewInit() {
    if (this.selectedAdaptationId !== '') {
      setTimeout(() => this.inputCreatePlanName.nativeElement.focus(), 0);
    }
  }

  ngOnDestroy() {
    this.subs.unsubscribe();
  }

  onDeletePlan(id: string) {
    this.store.dispatch(MerlinActions.deletePlan({ id }));
  }

  onOpenPlan(id: string) {
    this.router.navigate(['/plans', id]);
  }

  onSubmit() {
    if (this.createPlanForm.valid) {
      const plan: SPlan = { ...this.createPlanForm.value };
      this.store.dispatch(MerlinActions.createPlan({ plan }));
    }
  }
}
