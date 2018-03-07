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
  RavenSource,
  StringTMap,
} from './../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-tree',
  styleUrls: ['./raven-tree.component.css'],
  templateUrl: './raven-tree.component.html',
})
export class RavenTreeComponent {
  @Input() id: string;
  @Input() tree: StringTMap<RavenSource>;

  @Output() action: EventEmitter<any> = new EventEmitter<any>(); // TODO: Remove any.
  @Output() close: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();
  @Output() collapse: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();
  @Output() expand: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();
  @Output() open: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();
  @Output() select: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();
}
