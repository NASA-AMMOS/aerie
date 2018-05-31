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

import {
  RavenFilterSource,
} from './../../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-filter',
  styleUrls: ['./raven-filter.component.css'],
  templateUrl: './raven-filter.component.html',
})

export class RavenFilterComponent {
  @Input() id: string;
  @Input() source: RavenFilterSource;

  @Output() addFilter: EventEmitter<RavenFilterSource> = new EventEmitter<RavenFilterSource>();
  @Output() removeFilter: EventEmitter<RavenFilterSource> = new EventEmitter<RavenFilterSource>();
  @Output() select: EventEmitter<RavenFilterSource> = new EventEmitter<RavenFilterSource>();
}
