/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {
  MatButtonModule,
  MatDialogModule,
  MatExpansionModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatProgressBarModule,
  MatSelectModule,
  MatSidenavModule,
  MatTableModule,
  MatToolbarModule,
  MatTooltipModule,
} from '@angular/material';
import { RouterModule } from '@angular/router';
import {
  NestActivityBandModule,
  NestTimeBandModule,
  RavenActivityTypeListModule,
  RavenAppHeaderModule,
  RavenPlanSelectorModule,
} from '../../../shared/components/modules';
import { PlanningAppComponent } from './planning-app.component';

@NgModule({
  declarations: [PlanningAppComponent],
  exports: [PlanningAppComponent],
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSidenavModule,
    MatTableModule,
    MatToolbarModule,
    MatTooltipModule,
    NestActivityBandModule,
    NestTimeBandModule,
    RavenActivityTypeListModule,
    RavenPlanSelectorModule,
    RavenAppHeaderModule,
  ],
})
export class PlanningAppModule {}
