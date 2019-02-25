/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'raven-app-header',
  styleUrls: ['./raven-app-header.component.css'],
  templateUrl: './raven-app-header.component.html',
})
export class RavenAppHeaderComponent {
  /**
   * Whether to display the contextual top bar which is typically a different
   * color than the primary, and usually has a back or X icon instead of the
   * standard hamburger icon.
   */
  @Input()
  contextual = false;

  @Input()
  title = '';

  @Output()
  menuClicked: EventEmitter<never> = new EventEmitter();

  get color() {
    return this.contextual ? 'accent' : 'primary';
  }

  get icon() {
    return this.contextual ? 'arrow_back' : 'menu';
  }
}
