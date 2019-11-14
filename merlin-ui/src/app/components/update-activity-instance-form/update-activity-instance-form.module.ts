import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../../material';
import { UpdateActivityInstanceFormComponent } from './update-activity-instance-form.component';

@NgModule({
  declarations: [UpdateActivityInstanceFormComponent],
  exports: [UpdateActivityInstanceFormComponent],
  imports: [CommonModule, MaterialModule, ReactiveFormsModule],
})
export class UpdateActivityInstanceFormModule {}
