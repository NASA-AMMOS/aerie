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
  Output,
} from '@angular/core';
import { ActivityInstance } from '../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'activity-table',
  styleUrls: ['./activity-table.component.css'],
  templateUrl: './activity-table.component.html',
})
export class ActivityTableComponent {
  @Input()
  activities: ActivityInstance[];

  @Input()
  activity: ActivityInstance | null;

  @Input()
  displayedColumns: string[] = [
    'name',
    'start',
    'end',
    'duration',
    'activityType',
    'options',
  ];

  @Output()
  deleteActivity: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  editActivity: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  selectActivity: EventEmitter<string> = new EventEmitter<string>();
}
