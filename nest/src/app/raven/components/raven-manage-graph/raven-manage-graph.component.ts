/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { EventEmitter, Input, Output } from '@angular/core';
import { Component } from '@angular/core';

@Component({
  selector: 'raven-manage-graph',
  styleUrls: ['./raven-manage-graph.component.css'],
  templateUrl: './raven-manage-graph.component.html',
})
export class RavenManageGraphComponent {
  @Input()
  currentStateChanged: boolean;

  @Input()
  currentStateId: string;

  @Input()
  guides: number[];

  @Input()
  mode: string;

  @Output()
  applyCurrentLayout: EventEmitter<any> = new EventEmitter();

  @Output()
  applyCurrentState: EventEmitter<any> = new EventEmitter();

  @Output()
  panToRight: EventEmitter<any> = new EventEmitter();

  @Output()
  panToLeft: EventEmitter<any> = new EventEmitter();

  @Output()
  panTo: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  removeAllBands: EventEmitter<any> = new EventEmitter();

  @Output()
  removeAllGuides: EventEmitter<any> = new EventEmitter();

  @Output()
  resetView: EventEmitter<any> = new EventEmitter();

  @Output()
  shareableLink: EventEmitter<any> = new EventEmitter();

  @Output()
  updateCurrentState: EventEmitter<any> = new EventEmitter();

  @Output()
  zoomIn: EventEmitter<any> = new EventEmitter();

  @Output()
  zoomOut: EventEmitter<any> = new EventEmitter();
}
