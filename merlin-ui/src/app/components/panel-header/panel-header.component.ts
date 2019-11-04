import { Component } from '@angular/core';

@Component({
  selector: 'app-panel-header',
  styles: [
    `
      :host {
        align-items: center;
        background-color: #b96102;
        color: white;
        display: inline-flex;
        height: 25px;
        overflow: hidden;
        justify-content: flex-start;
        padding: 0.25rem;
        white-space: nowrap;
        width: 100%;
      }
    `,
  ],
  template: '<ng-content></ng-content>',
})
export class PanelHeaderComponent {}
