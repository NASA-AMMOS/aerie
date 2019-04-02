/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { timestamp, utc } from '../../../shared/util';
import { RavenUpdate } from '../../models';
import { defaultColors } from '../../util';

@Component({
  selector: 'raven-time-cursor',
  styleUrls: ['./raven-time-cursor.component.css'],
  templateUrl: './raven-time-cursor.component.html',
})
export class RavenTimeCursorComponent implements OnChanges {
  @Input()
  autoPage: boolean;

  @Input()
  clockRate: number;

  @Input()
  currentTimeDelta: number;

  @Input()
  cursorColor: string;

  @Input()
  cursorTime: number | null;

  @Input()
  cursorWidth: number;

  @Input()
  showTimeCursor: boolean;

  @Input()
  setCursorTime: number | null;

  @Output()
  displayTimeCursor: EventEmitter<boolean> = new EventEmitter<boolean>();

  @Output()
  updateTimeCursorSettings: EventEmitter<RavenUpdate> = new EventEmitter<
    RavenUpdate
  >();

  colors = defaultColors;

  cursorWidthControl: FormControl = new FormControl('', [
    Validators.min(1),
    Validators.max(5),
  ]);

  setCursorTimeControl: FormControl = new FormControl('', [
    Validators.pattern(/(\d\d\d\d)-(\d\d\d)T(\d\d):(\d\d):(\d\d)\.?(\d\d\d)?/),
  ]);

  ngOnChanges(changes: SimpleChanges): void {
    // If we are showing the time-cursor and have a user-set cursor time, then set it's input field to it's value..
    if (changes.showTimeCursor && this.showTimeCursor && this.setCursorTime) {
      this.setCursorTimeControl.setValue(timestamp(this.setCursorTime));
    }
  }

  /**
   * Event. Helper that emits a new cursor time when the user presses enter in the `Set Cursor Time` input.
   * Note how we set both `cursorTime` and `setCursorTime`. This is so we can keep track of the user-set time, and
   * the current time-cursor time as it marches on.
   */
  onSetCursorTime(): void {
    if (this.setCursorTimeControl.valid) {
      const newCursorTime = utc(this.setCursorTimeControl.value);

      if (newCursorTime !== 0) {
        this.updateTimeCursorSettings.emit({
          update: {
            cursorTime: newCursorTime,
            setCursorTime: newCursorTime,
          },
        });
      } else {
        // If the `newCursorTime` is 0 (i.e. the input field is empty), then just set `setCursorTime` to null.
        this.updateTimeCursorSettings.emit({
          update: {
            setCursorTime: null,
          },
        });
      }
    }
  }

  /**
   * Event. Called with the cursor width input changes.
   */
  onSetCursorWidth(cursorWidth: number): void {
    if (this.cursorWidthControl.valid) {
      this.updateTimeCursorSettings.emit({ update: { cursorWidth } });
    }
  }
}
