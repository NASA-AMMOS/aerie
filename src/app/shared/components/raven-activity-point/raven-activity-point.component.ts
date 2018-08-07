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
  RavenActivityPoint,
} from './../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-activity-point',
  styleUrls: ['./raven-activity-point.component.css'],
  templateUrl: './raven-activity-point.component.html',
})
export class RavenActivityPointComponent {
  @Input() selectedPoint: RavenActivityPoint;
  @Input() showActivityPointMetadata: boolean;
  @Input() showActivityPointParameters: boolean;

  @Output() toggleShowActivityPointMetadata: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() toggleShowActivityPointParameters: EventEmitter<boolean> = new EventEmitter<boolean>();
}
