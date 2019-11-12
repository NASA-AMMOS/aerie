/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, Input } from '@angular/core';

@Component({
  selector: 'raven-app-header',
  styles: [
    `
      mat-toolbar {
        height: 36px;
      }

      .mat-toolbar-right {
        display: inline-flex;
        flex: 1 1 auto;
        justify-content: flex-end;
      }
    `,
  ],
  template: `
    <mat-toolbar color="primary">
      <span>{{ title }}</span>

      <div class="mat-toolbar-right">
        <ng-content></ng-content>
      </div>
    </mat-toolbar>
  `,
})
export class RavenAppHeaderComponent {
  @Input()
  title = '';
}
