import { NgModule } from '@angular/core';
import { MaterialModule } from '../../material';
import { ContextMenuTriggerModule } from '../context-menu-trigger';
import { ActivityInstancesTableComponent } from './activity-instances-table.component';

@NgModule({
  declarations: [ActivityInstancesTableComponent],
  exports: [ActivityInstancesTableComponent],
  imports: [ContextMenuTriggerModule, MaterialModule],
})
export class ActivityInstancesTableModule {}
