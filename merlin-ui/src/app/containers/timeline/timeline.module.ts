import { NgModule } from '@angular/core';
import { TimeAxisModule } from '../../components';
import { TimelineComponent } from './timeline.component';

@NgModule({
  declarations: [TimelineComponent],
  exports: [TimelineComponent],
  imports: [TimeAxisModule],
})
export class TimelineModule {}
