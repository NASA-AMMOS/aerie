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
  Output,
  ViewChild,
} from '@angular/core';
import { MatExpansionPanel } from '@angular/material';

import { RavenPlan } from '../../models/raven-plan';

@Component({
  selector: 'raven-plan-list',
  styleUrls: ['./raven-plan-list.component.css'],
  templateUrl: './raven-plan-list.component.html',
})
export class RavenPlanListComponent {
  @Input()
  plans: RavenPlan[];

  @Input()
  expanded = true;

  @Input()
  title = 'Plans';

  @Input()
  selectedPlan: RavenPlan;

  @Output()
  createPlanClicked = new EventEmitter();

  @Output()
  deletePlanClicked = new EventEmitter<string>();

  @Output()
  updatePlanClicked = new EventEmitter<string>();

  @Output()
  selectPlanClicked = new EventEmitter<string>();

  @Output()
  closed = new EventEmitter<void>();

  @Output()
  opened = new EventEmitter<void>();

  @ViewChild('panel')
  panel: MatExpansionPanel;

  /**
   * Event. Called when the create button is clicked
   */
  onClickCreate() {
    this.createPlanClicked.emit();
  }

  /**
   * Event. Called when the "X" button is clicked
   * @param id id of the record to delete
   */
  onClickDelete(e: Event, id: string) {
    e.stopPropagation();
    this.deletePlanClicked.emit(id);
  }

  /**
   * Event. Called when the item is clicked
   * @param id id of the record to update
   */
  onClickUpdate(e: Event, id: string) {
    e.stopPropagation();
    this.updatePlanClicked.emit(id);
  }

  /**
   * Event. Called when the item is selected
   * @param id id of the record to select
   */
  onClickSelect(id: string) {
    this.panel.close();
    this.selectPlanClicked.emit(id);
  }
}
