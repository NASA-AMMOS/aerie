/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { NavigateByUrl } from '../../../../../libs/ngrx-router';
import { Adaptation, Plan } from '../../../shared/models';
import { LayoutActions, PlanActions } from '../../actions';
import { PlanningAppState } from '../../planning-store';
import {
  getAdaptations,
  getPlansAsList,
  getShowCreatePlanDrawer,
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

  constructor(
    private store: Store<PlanningAppState>,
    public planningService: PlanningService,
  ) {
    this.adaptations$ = this.store.pipe(select(getAdaptations));
    this.plans$ = this.store.pipe(select(getPlansAsList));
    this.showCreatePlanDrawer$ = this.store.pipe(
      select(getShowCreatePlanDrawer),
    );
  }

  onCreatePlan(plan: Plan): void {
    this.store.dispatch(PlanActions.createPlan({ plan }));
  }

  onDeletePlan(planId: string): void {
    this.store.dispatch(PlanActions.deletePlan({ planId }));
  }

  onSelectPlan(planId: string): void {
    this.store.dispatch(new NavigateByUrl(`/plans/${planId}`));
  }

  onToggleCreatePlanDrawer(opened?: boolean): void {
    this.store.dispatch(LayoutActions.toggleCreatePlanDrawer({ opened }));
  }
}
