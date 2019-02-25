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
import { ActivityType } from '../../../../../../schemas/types/ts';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-sub-activity-tree',
  styleUrls: ['./raven-sub-activity-tree.component.css'],
  templateUrl: './raven-sub-activity-tree.component.html',
})
export class RavenSubActivityTreeComponent implements OnChanges {
  // TODO: Pass in real data.
  @Input()
  activityTypes: any[] = [
    {
      description: 'After parent start',
      icon: 'subdirectory_arrow_right',
      id: '1',
      level: 0,
      name: 'Activity Type child 1',
      start: '1',
    },
    {
      description: 'After previous end',
      icon: 'subdirectory_arrow_right',
      id: '2',
      level: 1,
      name: 'Activity Type child 2',
      start: '2',
    },
    {
      description: 'After previous end',
      icon: 'subdirectory_arrow_right',
      id: '3',
      level: 2,
      name: 'Activity Type child 3',
      start: '3',
    },
    {
      description: 'Start Offset + 15min',
      icon: 'keyboard_tab',
      id: '5',
      level: 0,
      name: 'Activity Type child 5',
      start: '0',
    },
  ];

  @Output()
  activityClicked: EventEmitter<string> = new EventEmitter<string>();

  indentPadding = 15;
  sortedActivityTypes: ActivityType[];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.activityTypes) {
      // TODO: Calculate level based on start.
    }
  }
}
