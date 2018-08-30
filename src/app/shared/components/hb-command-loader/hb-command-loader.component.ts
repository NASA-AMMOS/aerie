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

import { HBCommandDictionary } from '../../models/hb-command-dictionary';

/**
 * Display a list of commands that can be loaded into the system
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hb-command-loader',
  styleUrls: ['./hb-command-loader.component.css'],
  templateUrl: './hb-command-loader.component.html',
})
export class HBCommandLoaderComponent {

  @Input()
  dictionaries: HBCommandDictionary[] = [];

  @Input()
  selectedId: string | null = null;

  @Output()
  selectedDictionaryChanged: EventEmitter<string> = new EventEmitter<string>();

  /**
   * Event. Called when the row selection changes.
   */
  onSelectionChanged(id: string) {
    this.selectedDictionaryChanged.emit(id);
  }
}
