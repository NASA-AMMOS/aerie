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
  FilterState,
  RavenPin,
  RavenSource,
  RavenSourceActionEvent,
  StringTMap,
} from '../../../shared/models';

import { getSortedChildIds } from '../../util/source';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-tree',
  styleUrls: ['./raven-tree.component.css'],
  templateUrl: './raven-tree.component.html',
})
export class RavenTreeComponent implements OnChanges {
  @Input()
  pins: RavenPin[];

  @Input()
  source: RavenSource;

  @Input()
  tree: StringTMap<RavenSource>;

  @Input()
  filter: FilterState;

  @Output()
  action: EventEmitter<RavenSourceActionEvent> = new EventEmitter<
    RavenSourceActionEvent
  >();

  @Output()
  addCustomGraph: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  @Output()
  addFilter: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  @Output()
  addGraphableFilter: EventEmitter<RavenSource> = new EventEmitter<
    RavenSource
  >();

  @Output()
  close: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  @Output()
  collapse: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  @Output()
  expand: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  @Output()
  open: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  @Output()
  openMetadata: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  @Output()
  removeFilter: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  @Output()
  removeGraphableFilter: EventEmitter<RavenSource> = new EventEmitter<
    RavenSource
  >();

  @Output()
  select: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  @Output()
  selectCustomFilter: EventEmitter<RavenSource> = new EventEmitter<
    RavenSource
  >();

  sortedChildIds: string[];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.source) {
      this.sortedChildIds = getSortedChildIds(this.tree, this.source.childIds);
    }
  }

  get childFilter(): FilterState {
    if (FilterState.isMatch(this.filter, this.source.id)) {
      // All descendants of a filtered source should be visible.
      return FilterState.empty();
    } else {
      return this.filter;
    }
  }

  get isVisible(): boolean {
    // This node is visible if it's the ancestor of a match.
    // This allows us to actually navigate down to a match.
    return FilterState.isAncestorOfMatch(this.filter, this.source.id);
  }
}
