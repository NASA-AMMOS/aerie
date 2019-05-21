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
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { MpsCommand, StringTMap } from '../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'seq-command-list',
  styleUrls: ['./seq-command-list.component.css'],
  templateUrl: './seq-command-list.component.html',
})
export class SeqCommandListComponent implements OnChanges {
  @Input()
  commands: MpsCommand[] | null;

  @Input()
  commandsByName: StringTMap<MpsCommand> = {};

  @Input()
  commandFilterQuery: string;

  @Output()
  selectCommand: EventEmitter<string> = new EventEmitter<string>();

  sortedCommands: MpsCommand[] = [];
  sortedAndFilteredCommands: MpsCommand[] = [];

  ngOnChanges(changes: SimpleChanges) {
    if (this.commands && changes.commands) {
      this.sortedCommands = this.commands.sort((a, b) => {
        if (a.name < b.name) return -1;
        if (a.name > b.name) return 1;
        return 0;
      });
      this.sortedAndFilteredCommands = this.sortedCommands;
    }

    if (changes.commandFilterQuery) {
      if (this.commandFilterQuery === '') {
        this.sortedAndFilteredCommands = this.sortedCommands;
      } else {
        this.filteredCommands();
      }
    }
  }

  onSelectCommand(event: MouseEvent, command: MpsCommand) {
    // Prevents the click event from propagating to the mat-expansion-panel so it doesn't open
    // when the user clicks on the add command button
    event.stopPropagation();
    this.selectCommand.emit(command.name);
  }

  /**
   * Getter for the sorted and filtered commands
   */
  // TODO: Should debounce the filter if performance issues arise
  filteredCommands() {
    if (this.commandFilterQuery !== '') {
      this.sortedAndFilteredCommands = this.sortedCommands.filter(command => {
        return command.name.startsWith(this.commandFilterQuery);
      });
    } else {
      this.sortedAndFilteredCommands = this.sortedCommands;
    }
  }
}
