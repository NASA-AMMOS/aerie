import { NgModule } from '@angular/core';
import { MaterialModule } from '../../material';
import { PlansTableComponent } from './plans-table.component';

@NgModule({
  declarations: [PlansTableComponent],
  exports: [PlansTableComponent],
  imports: [MaterialModule],
})
export class PlansTableModule {}
