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

import { SourceFilter } from '../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-quick-filter-panel',
  styles: [``],
  templateUrl: './raven-quick-filter-panel.component.html',
})
export class RavenQuickFilterPanelComponent {
  @Input()
  filter: SourceFilter = SourceFilter.truth();

  @Input()
  isActive: boolean = false;

  @Output()
  applyFilter: EventEmitter<SourceFilter> = new EventEmitter<SourceFilter>();

  onSubmit() {
    this.applyFilter.emit(this.filter);
  }

  get namePattern() {
    if (
      this.filter &&
      'name' in this.filter &&
      'matches' in this.filter['name']
    ) {
      return this.filter['name']['matches'];
    } else {
      return '';
    }
  }

  set namePattern(pattern: string) {
    if (pattern === '') {
      this.filter = SourceFilter.truth();
    } else {
      this.filter = { name: { matches: pattern } };
    }
  }
}
