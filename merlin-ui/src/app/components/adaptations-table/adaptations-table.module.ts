import { NgModule } from '@angular/core';
import { MaterialModule } from '../../material';
import { AdaptationsTableComponent } from './adaptations-table.component';

@NgModule({
  declarations: [AdaptationsTableComponent],
  exports: [AdaptationsTableComponent],
  imports: [MaterialModule],
})
export class AdaptationsTableModule {}
