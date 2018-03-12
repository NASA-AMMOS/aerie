import { Component, Input } from '@angular/core';

import { RavenActivityPoint } from './../../../shared/models';

@Component({
  selector: 'raven-activity-point',
  styleUrls: ['./raven-activity-point.component.css'],
  templateUrl: './raven-activity-point.component.html',
})

export class RavenActivityPointComponent {

  @Input() dataPoint: RavenActivityPoint;

}
