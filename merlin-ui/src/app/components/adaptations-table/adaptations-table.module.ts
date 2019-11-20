import { NgModule } from '@angular/core';
import { MaterialModule } from '../../material';
import { ContextMenuTriggerModule } from '../context-menu-trigger';
import { AdaptationsTableComponent } from './adaptations-table.component';

@NgModule({
  declarations: [AdaptationsTableComponent],
  exports: [AdaptationsTableComponent],
  imports: [ContextMenuTriggerModule, MaterialModule],
})
export class AdaptationsTableModule {}
