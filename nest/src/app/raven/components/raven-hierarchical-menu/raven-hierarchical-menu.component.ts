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
  ElementRef,
  EventEmitter,
  Input,
  Output,
  ViewChild,
} from '@angular/core';
import { RavenSourceAction } from '../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-hierarchical-menu',
  styleUrls: ['./raven-hierarchical-menu.component.css'],
  templateUrl: './raven-hierarchical-menu.component.html',
})
export class RavenHierarchicalMenuComponent {
  @Input()
  options: RavenSourceAction[];

  @Output()
  action: EventEmitter<string> = new EventEmitter<string>();

  @ViewChild('menuRef')
  public menuRef: ElementRef;

  isExpandOption(option: RavenSourceAction): boolean {
    return Array.isArray(option.event);
  }

  isEmitOption(option: RavenSourceAction): boolean {
    return !this.isExpandOption(option);
  }
}
