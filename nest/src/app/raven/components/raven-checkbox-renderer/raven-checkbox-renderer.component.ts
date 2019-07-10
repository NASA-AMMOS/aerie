import { Component } from '@angular/core';

import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';

@Component({
  selector: 'raven-checkbox-renderer',
  styleUrls: ['./raven-checkbox-renderer.component.css'],
  templateUrl: './raven-checkbox-renderer.component.html',
})
export class RavenCheckboxRendererComponent
  implements ICellRendererAngularComp {
  public params: ICellRendererParams;

  agInit(params: ICellRendererParams): void {
    this.params = params;
  }

  public onClick() {
    // invoke parent method
    this.params.context.componentParent.toggleEpoch(this.params.node.rowIndex);
  }

  refresh(): boolean {
    return false;
  }
}
