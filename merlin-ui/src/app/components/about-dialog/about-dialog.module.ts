import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MaterialModule } from '../../material';
import { AboutDialogComponent } from './about-dialog.component';

@NgModule({
  declarations: [AboutDialogComponent],
  entryComponents: [AboutDialogComponent],
  exports: [AboutDialogComponent],
  imports: [CommonModule, MaterialModule],
})
export class AboutDialogModule {}
