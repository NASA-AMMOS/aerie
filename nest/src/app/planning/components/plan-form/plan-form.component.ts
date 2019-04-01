/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Adaptation, Plan } from '../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'plan-form',
  styleUrls: ['./plan-form.component.css'],
  templateUrl: './plan-form.component.html',
})
export class PlanFormComponent implements OnChanges {
  @Input()
  adaptations: Adaptation[] = [];

  @Input()
  isNew = false;

  @Input()
  plan: Plan | null;

  @Output()
  create: EventEmitter<Plan> = new EventEmitter<Plan>();

  @Output()
  update: EventEmitter<Plan> = new EventEmitter<Plan>();

  form: FormGroup;

  constructor(fb: FormBuilder) {
    this.form = fb.group({
      adaptationId: new FormControl('', [Validators.required]),
      endTimestamp: new FormControl('', [Validators.required]),
      name: new FormControl('', [Validators.required]),
      startTimestamp: new FormControl('', [Validators.required]),
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.plan && this.plan && !this.isNew) {
      this.form.patchValue(this.plan);
    }
  }

  onSubmit(value: Plan) {
    if (this.form.valid) {
      if (this.isNew) {
        this.create.emit(value);
      } else {
        this.update.emit(value);
      }
    }
  }
}
