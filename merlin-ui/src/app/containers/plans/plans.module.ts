import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AngularSplitModule } from 'angular-split';
import { PanelHeaderModule } from '../../components';
import { MaterialModule } from '../../material';
import { PlansComponent } from './plans.component';

@NgModule({
  declarations: [PlansComponent],
  exports: [PlansComponent],
  imports: [
    CommonModule,
    MaterialModule,
    ReactiveFormsModule,
    RouterModule,
    AngularSplitModule.forChild(),
    PanelHeaderModule,
  ],
})
export class PlansModule {}
