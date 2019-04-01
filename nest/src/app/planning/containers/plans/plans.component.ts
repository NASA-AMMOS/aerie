/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { Adaptation, Plan } from '../../../shared/models';
import {
  ToggleCreatePlanDrawer,
  ToggleEditPlanDrawer,
} from '../../actions/layout.actions';
import {
  CreatePlan,
  DeletePlan,
  SelectPlan,
  UpdatePlan,
} from '../../actions/plan.actions';
import { PlanningAppState } from '../../planning-store';
import {
  getAdaptations,
  getPlans,
  getSelectedPlan,
  getShowCreatePlanDrawer,
  getShowEditPlanDrawer,
} from '../../selectors';
import { PlanningService } from '../../services/planning.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'plans-app',
  styleUrls: ['./plans.component.css'],
  templateUrl: './plans.component.html',
})
export class PlansComponent {
  adaptations$: Observable<Adaptation[]>;
  plans$: Observable<Plan[]>;
  showCreatePlanDrawer$: Observable<boolean>;
  showEditPlanDrawer$: Observable<boolean>;
  selectedPlan$: Observable<Plan | null>;

  constructor(
    private store: Store<PlanningAppState>,
    private router: Router,
    public planningService: PlanningService,
  ) {
    this.adaptations$ = this.store.pipe(select(getAdaptations));
    this.plans$ = this.store.pipe(select(getPlans));
    this.showCreatePlanDrawer$ = this.store.pipe(
      select(getShowCreatePlanDrawer),
    );
    this.showEditPlanDrawer$ = this.store.pipe(select(getShowEditPlanDrawer));
    this.selectedPlan$ = this.store.pipe(select(getSelectedPlan));
  }

  onCreatePlan(plan: Plan): void {
    this.store.dispatch(new CreatePlan(plan));
  }

  onDeletePlan(planId: string): void {
    this.store.dispatch(new DeletePlan(planId));
  }

  onOpenPlan(planId: string): void {
    this.router.navigateByUrl(`/plans/${planId}`);
  }

  onSelectPlan(planId: string): void {
    this.store.dispatch(new SelectPlan(planId));
    this.store.dispatch(new ToggleEditPlanDrawer(true));
  }

  onToggleCreatePlanDrawer(opened?: boolean): void {
    this.store.dispatch(new ToggleCreatePlanDrawer(opened));
  }

  onToggleEditPlanDrawer(opened?: boolean): void {
    this.store.dispatch(new ToggleEditPlanDrawer(opened));
  }

  onUpdatePlan(plan: Plan): void {
    this.store.dispatch(new UpdatePlan(plan.id || '', plan));
  }
}
