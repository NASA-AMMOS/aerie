import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AngularSplitModule } from 'angular-split';
import { PanelHeaderModule } from '../../components';
import { MaterialModule } from '../../material';
import { AdaptationsComponent } from './adaptations.component';

@NgModule({
  declarations: [AdaptationsComponent],
  exports: [AdaptationsComponent],
  imports: [
    CommonModule,
    MaterialModule,
    ReactiveFormsModule,
    RouterModule,
    AngularSplitModule.forChild(),
    PanelHeaderModule,
  ],
})
export class AdaptationsModule {}
