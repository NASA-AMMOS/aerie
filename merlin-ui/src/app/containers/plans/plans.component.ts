import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
} from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { SubSink } from 'subsink';
import { MerlinActions } from '../../actions';
import { AppState } from '../../app-store';
import { getAdaptations, getLoading, getPlans } from '../../selectors';
import { CAdaptation, CPlan, SPlan } from '../../types';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-plans',
  styleUrls: ['./plans.component.css'],
  templateUrl: './plans.component.html',
})
export class PlansComponent implements OnDestroy {
  adaptations: CAdaptation[] = [];
  createPlanForm: FormGroup;
  displayedColumns: string[] = [
    'menu',
    'name',
    'adaptationId',
    'startTimestamp',
    'endTimestamp',
  ];
  loading = false;
  plans: CPlan[] = [];

  private subs = new SubSink();

  constructor(
    private fb: FormBuilder,
    private ref: ChangeDetectorRef,
    private router: Router,
    private store: Store<AppState>,
  ) {
    this.store.dispatch(MerlinActions.setLoading({ loading: true }));
    this.createPlanForm = this.fb.group({
      adaptationId: new FormControl('', [Validators.required]),
      endTimestamp: new FormControl('', [Validators.required]),
      name: new FormControl('', [Validators.required]),
      startTimestamp: new FormControl('', [Validators.required]),
    });

    this.subs.add(
      this.store.pipe(select(getAdaptations)).subscribe(adaptations => {
        this.adaptations = adaptations;
        this.ref.markForCheck();
      }),
      this.store.pipe(select(getLoading)).subscribe(loading => {
        this.loading = loading;
        this.ref.markForCheck();
      }),
      this.store.pipe(select(getPlans)).subscribe(plans => {
        this.plans = plans;
        this.ref.markForCheck();
      }),
    );
  }

  ngOnDestroy() {
    this.subs.unsubscribe();
  }

  onAbout() {
    this.store.dispatch(MerlinActions.openAboutDialog());
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
