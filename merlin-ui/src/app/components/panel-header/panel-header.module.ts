import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { PanelHeaderComponent } from './panel-header.component';

@NgModule({
  declarations: [PanelHeaderComponent],
  exports: [PanelHeaderComponent],
  imports: [CommonModule],
})
export class PanelHeaderModule {}
