/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { NestModule } from '../../models/nest-module';

/**
 * Width of the drawer when it is collapsed (when the drawer is open)
 */
export const COLLAPSED_WIDTH = 66;

@Component({
  selector: 'nest-app-nav',
  styles: [
    `
      mat-nav-list {
        width: ${COLLAPSED_WIDTH}px;
      }

      mat-nav-list.iconsOnly {
        width: ${COLLAPSED_WIDTH}px;
      }

      .nav-item-active {
        background: rgba(0, 0, 0, 0.15) !important;
      }
    `,
  ],
  templateUrl: './nest-app-nav.component.html',
})
export class NestAppNavComponent {
  @Input()
  modules: NestModule[];

  @Input()
  iconsOnly = false;

  @Output()
  aboutClicked: EventEmitter<MouseEvent> = new EventEmitter<MouseEvent>();
}
