import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MaterialModule } from '../../material';
import { ActivityInstancesTableComponent } from './activity-instances-table.component';

@NgModule({
  declarations: [ActivityInstancesTableComponent],
  exports: [ActivityInstancesTableComponent],
  imports: [CommonModule, MaterialModule],
})
export class ActivityInstancesTableModule {}
