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
import { ActivityInstance, ActivityType } from '../../../shared/models';
import { datetimeToEpoch, NgTemplateUtils } from '../../../shared/util';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'activity-form',
  styleUrls: ['./activity-form.component.css'],
  templateUrl: './activity-form.component.html',
})
export class ActivityFormComponent implements OnChanges {
  @Input()
  activity: ActivityInstance | null;

  @Input()
  activityTypes: ActivityType[] = [];

  @Input()
  isNew = false;

  @Output()
  clickAdvanced: EventEmitter<string> = new EventEmitter<string>();

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

  constructor(fb: FormBuilder) {
    this.form = fb.group({
      activityType: new FormControl(''),
      duration: new FormControl(0, [Validators.required]),
      intent: new FormControl(''),
      name: new FormControl('', [Validators.required]),
      start: new FormControl(0, [Validators.required]),
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.activity && this.activity && !this.isNew) {
      this.form.patchValue({
        ...this.activity,
        start: new Date(this.activity.start * 1000),
      });
    }
  }

  onSubmit(value: ActivityInstance) {
    // Transforming start here due to activity instance expecting start to be a number but the datetime picker returns a date
    // TODO: revisit this when schema for activity instances is finalized
    if (this.form.valid) {
      if (!this.isNew && this.activity) {
        this.updateActivity.emit({
          ...this.activity,
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
