import { NgModule } from '@angular/core';
import { MaterialModule } from '../../material';
import { ContextMenuTriggerModule } from '../context-menu-trigger';
import { PlansTableComponent } from './plans-table.component';

@NgModule({
  declarations: [PlansTableComponent],
  exports: [PlansTableComponent],
  imports: [ContextMenuTriggerModule, MaterialModule],
})
export class PlansTableModule {}
