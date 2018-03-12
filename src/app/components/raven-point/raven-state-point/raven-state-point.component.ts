import { Component, Input } from '@angular/core';

import { RavenStatePoint } from './../../../shared/models';

@Component({
  selector: 'raven-state-point',
  styleUrls: ['./raven-state-point.component.css'],
  templateUrl: './raven-state-point.component.html',
})
export class RavenStatePointComponent {
  @Input() dataPoint: RavenStatePoint;
}
