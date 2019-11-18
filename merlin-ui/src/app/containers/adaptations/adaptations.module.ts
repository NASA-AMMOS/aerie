import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { AngularSplitModule } from 'angular-split';
import {
  AdaptationsTableModule,
  PanelHeaderModule,
  PlaceholderModule,
  ToolbarModule,
} from '../../components';
import { MaterialModule } from '../../material';
import { AdaptationsComponent } from './adaptations.component';

@NgModule({
  declarations: [AdaptationsComponent],
  exports: [AdaptationsComponent],
  imports: [
    CommonModule,
    MaterialModule,
    ReactiveFormsModule,
    AngularSplitModule.forChild(),
    AdaptationsTableModule,
    PanelHeaderModule,
    PlaceholderModule,
    ToolbarModule,
  ],
})
export class AdaptationsModule {}
