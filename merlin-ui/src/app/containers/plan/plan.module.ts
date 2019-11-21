import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { AngularSplitModule } from 'angular-split';
import {
  ActivityInstancesTableModule,
  ActivityTypeListModule,
  CreateActivityInstanceFormModule,
  PanelHeaderModule,
  PlaceholderModule,
  ToolbarModule,
  UpdateActivityInstanceFormModule,
} from '../../components';
import { MaterialModule } from '../../material';
import { TimelineModule } from '../timeline';
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
    TimelineModule,
    ToolbarModule,
    UpdateActivityInstanceFormModule,
  ],
})
export class PlanModule {}
