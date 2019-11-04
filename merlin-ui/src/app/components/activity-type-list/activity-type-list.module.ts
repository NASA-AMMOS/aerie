import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MaterialModule } from '../../material';
import { ActivityTypeListComponent } from './activity-type-list.component';

@NgModule({
  declarations: [ActivityTypeListComponent],
  exports: [ActivityTypeListComponent],
  imports: [CommonModule, MaterialModule],
})
export class ActivityTypeListModule {}
