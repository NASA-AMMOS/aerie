import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { AngularSplitModule } from 'angular-split';
import {
  ActivityInstancesTableModule,
  ActivityTypeListModule,
  CreateActivityInstanceFormModule,
  PanelHeaderModule,
  PlaceholderModule,
  UpdateActivityInstanceFormModule,
} from '../../components';
import { MaterialModule } from '../../material';
import { PlanComponent } from './plan.component';

@NgModule({
  declarations: [PlanComponent],
  exports: [PlanComponent],
  imports: [
    CommonModule,
    MaterialModule,
    AngularSplitModule.forChild(),
    ActivityInstancesTableModule,
    ActivityTypeListModule,
    CreateActivityInstanceFormModule,
    PanelHeaderModule,
    PlaceholderModule,
    UpdateActivityInstanceFormModule,
  ],
})
export class PlanModule {}
