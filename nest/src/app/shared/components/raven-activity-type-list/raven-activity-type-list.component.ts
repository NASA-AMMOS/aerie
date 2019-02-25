/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ActivityType } from '../../../../../../schemas/types/ts';

@Component({
  selector: 'raven-activity-type-list',
  styleUrls: ['./raven-activity-type-list.component.css'],
  templateUrl: './raven-activity-type-list.component.html',
})
export class RavenActivityTypeListComponent {
  @Input()
  activityTypes: ActivityType[];

  @Input()
  expanded = true;

  @Output()
  createActivityTypeClicked = new EventEmitter();

  @Output()
  deleteActivityTypeClicked = new EventEmitter<string>();

  @Output()
  updateActivityTypeClicked = new EventEmitter<string>();

  @Output()
  selectActivityTypeClicked = new EventEmitter<string>();

  /**
   * Getter for creating new activity types.
   * @todo Integrate permissions
   */
  get canCreate() {
    return false;
  }

  /**
   * Getter for editing activity types
   * @todo Integrate permissions
   */
  get canEdit() {
    return false;
  }

  /**
   * Getter for deleting activity types
   * @todo Integrate permissions
   */
  get canDelete() {
    return false;
  }

  /**
   * Event. Called when the create button is clicked
   */
  onClickCreate() {
    this.createActivityTypeClicked.emit();
  }

  /**
   * Event. Called when the "X" button is clicked
   * @param id id of the record to delete
   */
  onClickDelete(e: Event, id: string) {
    e.stopPropagation();
    this.deleteActivityTypeClicked.emit(id);
  }

  /**
   * Event. Called when the item is clicked
   * @param id id of the record to update
   */
  onClickUpdate(e: Event, id: string) {
    e.stopPropagation();
    this.updateActivityTypeClicked.emit(id);
  }

  /**
   * Event. Called when the item is selected
   * @param id id of the record to select
   */
  onClickSelect(id: string) {
    this.selectActivityTypeClicked.emit(id);
  }
}
