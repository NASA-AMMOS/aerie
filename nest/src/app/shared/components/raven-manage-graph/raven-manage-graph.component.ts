import {
  EventEmitter,
  Input,
  Output,
} from '@angular/core';

import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'raven-manage-graph',
  styleUrls: ['./raven-manage-graph.component.css'],
  templateUrl: './raven-manage-graph.component.html',
})
export class RavenManageGraphComponent implements OnInit {
  @Input()
  currentStateChanged: boolean;

  @Input()
  currentStateId: string;

  @Input()
  guides: number[];

  @Input()
  mode: string;

  @Output()
    applyCurrentLayout: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    applyCurrentState: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    panRight: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    panLeft: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    panTo: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    removeAllBands: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    removeAllGuides: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    resetView: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    shareableLink: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    updateCurrentState: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    zoomIn: EventEmitter<any> = new EventEmitter<any>();

  @Output()
    zoomOut: EventEmitter<any> = new EventEmitter<any>();

  constructor() {}

  ngOnInit() {
  }

}
