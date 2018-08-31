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

import { HBCommand } from '../../models/hb-command';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hb-command-list',
  styleUrls: ['./hb-command-list.component.css'],
  templateUrl: './hb-command-list.component.html',
})
export class HBCommandListComponent {
  @Input()
  commands: HBCommand[] | null;

  @Output()
  selectedCommandChanged: EventEmitter<string> = new EventEmitter<string>();

  /**
   * Event. Called when a command is selected.
   */
  onSelection(command: HBCommand) {
    console.log('HBCommandListComponent::onSelection', command);
    this.selectedCommandChanged.emit(command.name);
  }
}
