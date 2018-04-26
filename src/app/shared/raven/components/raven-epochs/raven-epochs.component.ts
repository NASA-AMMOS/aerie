/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';

import {
  Sort,
} from '@angular/material';

import {
  RavenEpoch,
  RavenUpdate,
} from '../../../../shared/models';

@Component({
  selector: 'raven-epochs',
  styleUrls: ['./raven-epochs.component.css'],
  templateUrl: './raven-epochs.component.html',
})
export class RavenEpochsComponent implements OnInit {
  @Input() dayCode: string;
  @Input() earthSecToEpochSec: number;
  @Input() epochs: RavenEpoch[];
  @Input() inUseEpoch: RavenEpoch | null;

  @Output() importEpochs: EventEmitter<RavenEpoch[]> = new EventEmitter<RavenEpoch[]>();
  @Output() updateEpochs: EventEmitter<RavenUpdate> = new EventEmitter<RavenUpdate>();

  displayedColumns = ['select', 'name', 'value'];
  sortedAndFilteredEpochs: RavenEpoch[];

  ngOnInit() {
    this.sortedAndFilteredEpochs = [...this.epochs];
  }

  /**
   * Filter the table of Epochs.
   */
  applyFilter(filter: string) {
    const filterValue = filter.trim();
    const copy = [...this.epochs];

    // Match either name or value.
    this.sortedAndFilteredEpochs = copy.filter(epoch =>
      (epoch.name.indexOf(filterValue) > -1) || (epoch.value.indexOf(filterValue) > -1),
    );
  }

  /**
   * Helper. Compare function for use in sorting epochs.
   *
   * TODO: Move this to a util lib eventually.
   */
  compare(a: string, b: string, isAsc: boolean) {
    return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
  }

  /**
   * Read an input Epoch file. Emit new epochs if read is successful.
   */
  readFile(file: File): void {
    const reader: FileReader = new FileReader();

    reader.onloadend = (e) => {
      const newEpochs: RavenEpoch[] = JSON.parse(reader.result);
      this.importEpochs.emit(newEpochs);
    };

    reader.readAsText(file);
  }

  /**
   * Sort the table of epochs.
   */
  sortData(sort: Sort) {
    const epochs = [...this.epochs];

    if (!sort.active || sort.direction === '') {
      this.sortedAndFilteredEpochs = epochs;
      return;
    }

    this.sortedAndFilteredEpochs = epochs.sort((a, b) => {
      const isAsc = sort.direction === 'asc';

      switch (sort.active) {
        case 'name':
          return this.compare(a.name, b.name, isAsc);
        case 'value':
          return this.compare(a.value, b.value, isAsc);
        default:
          return 0;
      }
    });
  }
}
