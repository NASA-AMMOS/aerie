import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { AngularSplitModule } from 'angular-split';
import { ActivityTypeListModule, PanelHeaderModule } from '../../components';
import { MaterialModule } from '../../material';
import { CreateActivityInstanceFormModule } from '../create-activity-instance-form';
import { PlanComponent } from './plan.component';

@NgModule({
  declarations: [PlanComponent],
  exports: [PlanComponent],
  imports: [
    CommonModule,
    MaterialModule,
    AngularSplitModule.forChild(),
    ActivityTypeListModule,
    PanelHeaderModule,
    CreateActivityInstanceFormModule,
  ],
})
export class PlanModule {}
