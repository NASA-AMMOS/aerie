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

import { RavenPlan } from '../../models/raven-plan';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-plan-selector',
  styleUrls: ['./raven-plan-selector.component.css'],
  templateUrl: './raven-plan-selector.component.html',
})
export class RavenPlanSelectorComponent {
  @Input()
  plans: RavenPlan[];

  @Input()
  selectedPlan: RavenPlan;

  @Output()
  selectPlanClicked = new EventEmitter<string>();

  /**
   * Formatted start date
   * Assumes startTimestamp of format YYY-MM-DDTMM:SS:mm
   */
  get startDate(): string {
    if (this.selectedPlan) {
      const d = new Date(this.selectedPlan.startTimestamp);
      return `${d.getMonth() + 1}.${d.getDate()}.${d.getFullYear()}`;
    }
    return 'from';
  }

  /**
   * Formatted end date
   * Assumes endTimestamp of format YYY-MM-DDTMM:SS:mm
   */
  get endDate(): string {
    if (this.selectedPlan) {
      const d = new Date(this.selectedPlan.endTimestamp);
      return `${d.getMonth() + 1}.${d.getDate()}.${d.getFullYear()}`;
    }
    return 'to';
  }

  /**
   * ID of the selected plan or empty
   */
  get selectedId(): string {
    if (this.selectedPlan) {
      return this.selectedPlan.id;
    }
    return '';
  }

  /**
   * Event. Called when the item is selected
   * @param id id of the record to select
   */
  onSelectionChanged(id: string) {
    this.selectPlanClicked.emit(id);
  }
}
