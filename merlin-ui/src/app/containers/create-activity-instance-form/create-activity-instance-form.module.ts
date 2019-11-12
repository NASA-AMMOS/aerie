import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../../material';
import { CreateActivityInstanceFormComponent } from './create-activity-instance-form.component';

@NgModule({
  declarations: [CreateActivityInstanceFormComponent],
  exports: [CreateActivityInstanceFormComponent],
  imports: [CommonModule, MaterialModule, ReactiveFormsModule],
})
export class CreateActivityInstanceFormModule {}
