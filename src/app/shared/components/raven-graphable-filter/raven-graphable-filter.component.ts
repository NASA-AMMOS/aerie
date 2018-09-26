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

import { RavenGraphableFilterSource } from '../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-graphable-filter',
  styleUrls: ['./raven-graphable-filter.component.css'],
  templateUrl: './raven-graphable-filter.component.html',
})
export class RavenGraphableFilterComponent {
  @Input()
  id: string;

  @Input()
  source: RavenGraphableFilterSource;

  @Output()
  addGraphableFilter: EventEmitter<
    RavenGraphableFilterSource
  > = new EventEmitter<RavenGraphableFilterSource>();

  @Output()
  removeGraphableFilter: EventEmitter<
    RavenGraphableFilterSource
  > = new EventEmitter<RavenGraphableFilterSource>();
}
