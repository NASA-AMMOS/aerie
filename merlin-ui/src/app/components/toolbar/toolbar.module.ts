import { NgModule } from '@angular/core';
import { MaterialModule } from '../../material';
import { ToolbarComponent } from './toolbar.component';
@NgModule({
  declarations: [ToolbarComponent],
  exports: [ToolbarComponent],
  imports: [MaterialModule],
})
export class ToolbarModule {}
