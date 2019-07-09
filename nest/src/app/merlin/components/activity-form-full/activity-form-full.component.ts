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
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { ActivityInstance, ActivityType } from '../../../shared/models';
import { datetimeToEpoch, NgTemplateUtils } from '../../../shared/util';
import { MerlinService } from '../../services/merlin.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'activity-form-full',
  styleUrls: ['./activity-form-full.component.css'],
  templateUrl: './activity-form-full.component.html',
})
export class ActivityFormFullComponent implements OnInit, OnChanges {
  @Input()
  activityTypes: ActivityType[] | null;

  @Input()
  isNew: boolean;

  @Input()
  selectedActivity: ActivityInstance | null;

  @Output()
  cancel: EventEmitter<null> = new EventEmitter<null>();

  @Output()
  createActivity: EventEmitter<ActivityInstance> = new EventEmitter<
    ActivityInstance
  >();

  @Output()
  updateActivity: EventEmitter<ActivityInstance> = new EventEmitter<
    ActivityInstance
  >();

  form: FormGroup;
  ngTemplateUtils: NgTemplateUtils = new NgTemplateUtils();

  constructor(private fb: FormBuilder, public merlinService: MerlinService) {}

  ngOnInit() {
    this.form = this.fb.group({
      activityType: new FormControl(''),
      backgroundColor: new FormControl('#FFFFFF'),
      duration: new FormControl(0, [Validators.required]),
      intent: new FormControl(''),
      name: new FormControl('', [Validators.required]),
      start: new FormControl('', [Validators.required]),
      textColor: new FormControl('#000000'),
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (
      changes.selectedActivity &&
      this.selectedActivity !== null &&
      !this.isNew &&
      this.form
    ) {
      this.addParametersFormControl();
      this.form.patchValue({
        ...this.selectedActivity,
        start: new Date(this.selectedActivity.start * 1000),
      });
    }
  }

  /**
   * Add control for parameters dynamically since we can have an arbitrary number of them.
   */
  addParametersFormControl() {
    if (this.selectedActivity && this.selectedActivity.parameters) {
      this.form.addControl(
        'parameters',
        this.fb.array(
          this.selectedActivity.parameters.map(parameter =>
            this.fb.group({
              defaultValue: new FormControl(parameter.defaultValue),
              name: new FormControl(parameter.name),
              range: this.fb.array(
                parameter.range
                  ? parameter.range.map(range => new FormControl(range))
                  : [],
              ),
              type: new FormControl(parameter.type),
              value: new FormControl(parameter.value),
            }),
          ),
        ),
      );
    }
  }

  onSubmit(value: ActivityInstance) {
    if (this.form.valid) {
      if (!this.isNew) {
        this.updateActivity.emit({
          ...value,
          start: datetimeToEpoch(new Date(value.start)),
        });
      } else {
        this.createActivity.emit({
          ...value,
          start: datetimeToEpoch(new Date(value.start)),
        });
      }
    }
  }
}
