import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MaterialModule } from '../../material';
import { ConfirmDialogComponent } from './confirm-dialog.component';

@NgModule({
  declarations: [ConfirmDialogComponent],
  entryComponents: [ConfirmDialogComponent],
  exports: [ConfirmDialogComponent],
  imports: [CommonModule, MaterialModule],
})
export class ConfirmDialogModule {}
