import { Component, Input } from '@angular/core';

import { RavenDataItem } from './../../shared/models';

@Component({
  selector: 'raven-data-item',
  styleUrls: ['./raven-data-item.component.css'],
  templateUrl: './raven-data-item.component.html',
})

export class RavenDataItemComponent {
  @Input() dataItem: RavenDataItem;
}
