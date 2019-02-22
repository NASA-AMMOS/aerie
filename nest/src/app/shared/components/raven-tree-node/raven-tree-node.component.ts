/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-tree-node',
  styleUrls: ['./raven-tree-node.component.css'],
  templateUrl: './raven-tree-node.component.html',
})
export class RavenTreeNodeComponent {
  @Input()
  activated = false;

  @Input()
  markerActivatedIcon: string = ' ';

  @Input()
  markerDeactivatedIcon: string = ' ';

  @Input()
  markerTooltip?: string;

  @Input()
  icon = '';

  @Input()
  label = '';

  @Input()
  menu: any[] = [];

  @Input()
  multiselectable = false;

  @Input()
  status: 'normal' | 'pending' | 'failure' = 'normal';

  @Output()
  activate: EventEmitter<void> = new EventEmitter<void>();

  @Output()
  deactivate: EventEmitter<void> = new EventEmitter<void>();

  @Output()
  action: EventEmitter<string> = new EventEmitter<string>();

  hovered = false;
  menuOpen = false;

  get highlighted() {
    return this.hovered || this.menuOpen;
  }

  onClick() {
    if (this.activated && !this.multiselectable) {
      this.deactivate.emit();
    } else {
      this.activate.emit();
    }
  }

  onMenuOpenState(opened: boolean) {
    this.menuOpen = opened;
  }

  onMouseOverState(hovered: boolean) {
    this.hovered = hovered;
  }
}
