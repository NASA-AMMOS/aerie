import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { RavenActivityPoint } from './../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-activity-point',
  styleUrls: ['./raven-activity-point.component.css'],
  templateUrl: './raven-activity-point.component.html',
})

export class RavenActivityPointComponent {

  @Input() dataPoint: RavenActivityPoint;
  @Input() viewMetadata: boolean;
  @Input() viewParameter: boolean;

  @Output() toggleViewParameter: EventEmitter<any> = new EventEmitter<any>();
  @Output() toggleViewMetadata: EventEmitter<any> = new EventEmitter<any>();
}

