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
  RavenExpandableSource,
  RavenSourceActionEvent,
} from '../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-folder',
  styleUrls: ['./raven-folder.component.css'],
  templateUrl: './raven-folder.component.html',
})
export class RavenFolderComponent {
  @Input()
  source: RavenExpandableSource;

  @Output()
  action: EventEmitter<RavenSourceActionEvent> = new EventEmitter<
    RavenSourceActionEvent
  >();

  @Output()
  collapse: EventEmitter<RavenExpandableSource> = new EventEmitter<
    RavenExpandableSource
  >();

  @Output()
  expand: EventEmitter<RavenExpandableSource> = new EventEmitter<
    RavenExpandableSource
  >();
}
