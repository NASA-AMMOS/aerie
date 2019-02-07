/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ActivitiesComponent, HawkAppComponent } from './containers';

import {
  ActivityResolver,
  AdaptationListResolver,
  PlanListResolver,
  PlanResolver,
} from './resolvers';

export const routes: Routes = [
  {
    component: ActivitiesComponent,
    path: ':planId/activities',
    pathMatch: 'full',
    resolve: {
      adaptations: AdaptationListResolver,
      plans: PlanListResolver,
      selectedPlan: PlanResolver,
    },
  },
  {
    component: ActivitiesComponent,
    path: ':planId/activities/:activityId',
    pathMatch: 'full',
    resolve: {
      adaptations: AdaptationListResolver,
      plans: PlanListResolver,
      selectedActivity: ActivityResolver,
      selectedPlan: PlanResolver,
    },
  },
  {
    component: HawkAppComponent,
    path: ':planId',
    resolve: {
      adaptations: AdaptationListResolver,
      plans: PlanListResolver,
      selectedPlan: PlanResolver,
    },
  },
  {
    component: HawkAppComponent,
    path: '',
    resolve: {
      adaptations: AdaptationListResolver,
      plans: PlanListResolver,
    },
  },
];

@NgModule({
  exports: [RouterModule],
  imports: [RouterModule.forChild(routes)],
})
export class HawkRoutingModule {}
