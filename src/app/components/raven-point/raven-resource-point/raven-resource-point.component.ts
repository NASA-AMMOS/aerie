import { Component, Input } from '@angular/core';

import { RavenResourcePoint } from './../../../shared/models';

@Component({
  selector: 'raven-resource-point',
  styleUrls: ['./raven-resource-point.component.css'],
  templateUrl: './raven-resource-point.component.html',
})

export class RavenResourcePointComponent {
  @Input() dataPoint: RavenResourcePoint;
}
