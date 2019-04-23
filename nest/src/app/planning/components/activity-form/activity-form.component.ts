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
import { ColorEvent } from 'ngx-color';
import { ActivityInstance, ActivityType } from '../../../shared/models';
import { datetimeToEpoch, NgTemplateUtils } from '../../../shared/util';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'activity-form',
  styleUrls: ['./activity-form.component.css'],
  templateUrl: './activity-form.component.html',
})
export class ActivityFormComponent implements OnInit, OnChanges {
  @Input()
  activity: ActivityInstance | null;

  @Input()
  activityTypes: ActivityType[] = [];

  @Input()
  displayActions: boolean | null = true;

  @Input()
  isNew = false;

  @Input()
  parentForm: FormGroup | null;

  @Input()
  selectedActivityType: ActivityType | null = null;

  @Output()
  cancel: EventEmitter<null> = new EventEmitter<null>();

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
  manualTimeInput = false;

  constructor(fb: FormBuilder) {
    this.form = fb.group({
      activityType: new FormControl(''),
      backgroundColor: new FormControl('#FFFFFF'),
      duration: new FormControl(0, [Validators.required]),
      intent: new FormControl(''),
      name: new FormControl('', [Validators.required]),
      start: new FormControl('', [Validators.required]),
      textColor: new FormControl('#000000'),
    });
  }

  ngOnInit() {
    // Disable the activityType form if we are editing an activity instance
    if (!this.isNew) {
      this.form.controls.activityType.disable();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    // Editing selectedActivity
    if (changes.activity && this.activity && !this.isNew) {
      this.form.patchValue({
        ...this.activity,
        start: new Date(this.activity.start * 1000),
      });
    }

    // Create new activity from selecting activityType from activityType list
    if (changes.selectedActivityType && this.selectedActivityType) {
      this.form.patchValue({
        activityType: this.selectedActivityType.activityClass,
      });
    }

    // Handles when using activity-form independently or nested in full-activity form
    if (
      changes.parentForm &&
      this.parentForm &&
      this.form !== this.parentForm
    ) {
      this.form = this.parentForm;
    }
  }

  /**
   * Returns the correct datetime format
   */
  transformTime(newStart: Date | number): number {
    if (this.manualTimeInput) {
      return newStart as number;
    } else {
      return datetimeToEpoch(new Date(newStart));
    }
  }

  /**
   * Transforming start here due to activity instance expecting start to be a number but the datetime picker returns a date
   * @todo revisit this when schema for activity instances is finalized
   */
  onSubmit(value: ActivityInstance) {
    if (this.form.valid) {
      const start = this.transformTime(value.start);

      if (!this.isNew && this.activity) {
        this.updateActivity.emit({
          ...this.activity,
          ...value,
          start,
        });
      } else {
        this.createActivity.emit({
          ...value,
          start,
        });
      }
    }
  }

  updateColor($event: ColorEvent, type: string) {
    this.form.patchValue({ [type]: $event.color.hex });
  }

  onManualTimeInputChange() {
    this.manualTimeInput = !this.manualTimeInput;

    let start;
    if (!this.manualTimeInput) {
      // show datetime
      start = new Date(this.form.controls.start.value * 1000);
    } else if (this.activity) {
      // show epoch
      start = this.activity.start;
    }
    this.form.patchValue({
      start,
    });
  }
}
