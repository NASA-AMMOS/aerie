/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { ActivityType } from '../../../shared/models';

@Component({
  selector: 'activity-type-list',
  styleUrls: ['./activity-type-list.component.css'],
  templateUrl: './activity-type-list.component.html',
})
export class ActivityTypeListComponent implements OnChanges {
  @Input()
  activityTypes: ActivityType[] = [];

  filteredActivityTypes: ActivityType[] = [];
  searchText = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.activityTypes) {
      this.filterActivityTypes(this.searchText);
    }
  }

  filterActivityTypes(text: string) {
    this.filteredActivityTypes = this.activityTypes.filter(activityType =>
      activityType.activityClass.toLowerCase().includes(text.toLowerCase()),
    );
    this.searchText = text;
  }
}
