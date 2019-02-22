/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { RavenPlanFormDialogModule } from '../shared/components/modules';
import { ActivitiesModule } from './containers/activities/activities.module';
import { PlanningAppModule } from './containers/planning-app/planning-app.module';
import {
  AdaptationEffects,
  LayoutEffects,
  NavEffects,
  PlanEffects,
} from './effects';
import { PlanningRoutingModule } from './planning-routing.module';
import { reducers } from './planning-store';

@NgModule({
  imports: [
    HttpClientModule,
    PlanningRoutingModule,
    StoreModule.forFeature('planning', reducers),
    EffectsModule.forFeature([
      AdaptationEffects,
      LayoutEffects,
      NavEffects,
      PlanEffects,
    ]),
    PlanningAppModule,
    ActivitiesModule,
    RavenPlanFormDialogModule,
  ],
})
export class PlanningModule {}
