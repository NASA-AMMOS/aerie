/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  AfterViewInit,
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';

import { AgGridNg2 } from 'ag-grid-angular';

import { RavenEpoch, RavenUpdate } from '../../../shared/models';

@Component({
  selector: 'raven-epochs',
  styleUrls: ['./raven-epochs.component.css'],
  templateUrl: './raven-epochs.component.html',
})
export class RavenEpochsComponent implements AfterViewInit, OnChanges {
  @ViewChild('agGrid')
  agGrid: AgGridNg2;

  @Input()
  dayCode: string;

  @Input()
  earthSecToEpochSec: number;

  @Input()
  epochs: RavenEpoch[];

  @Input()
  inUseEpoch: RavenEpoch | null;

  @Output()
  importEpochs: EventEmitter<RavenEpoch[]> = new EventEmitter<RavenEpoch[]>();

  @Output()
  updateEpochs: EventEmitter<RavenUpdate> = new EventEmitter<RavenUpdate>();

  columnDefs: any[] = [];
  rowData: any[] = [];

  ngOnChanges(changes: SimpleChanges) {
    if (changes.epochs) {
      this.columnDefs = this.createColumnDefs();
      this.rowData = this.epochs;
      this.highlightRowForInUseEpoch();
    }

    if (changes.inUseEpoch) {
      this.highlightRowForInUseEpoch();
    }
  }

  ngAfterViewInit() {
    this.sizeColumnsToFit();
  }

  /**
   * Global Event. Called on window resize.
   * Here we just always make sure the columns are the max size they can possibly be.
   */
  @HostListener('window:resize', ['$event'])
  onResize(e: Event): void {
    this.sizeColumnsToFit();
  }

  /**
   * Calculates and returns `columnDefs` for use in the grid based on a point.
   */
  createColumnDefs() {
    const columnDefs: any[] = [
      {
        checkboxSelection: true,
        colId: 'select',
        field: 'select',
        headerName: '',
        hide: false,
        width: 80,
      },
      {
        colId: 'name',
        field: 'name',
        headerName: 'Name',
        hide: false,
      },
      {
        colId: 'value',
        field: 'value',
        headerName: 'Value',
        hide: false,
      },
    ];

    return columnDefs;
  }

  /**
   * Helper that highlights the row in the grid for the currently in-use epoch.
   * Note the setTimeout. This is to ensure Ag Grid is finished rendering before doing any selection.
   */
  highlightRowForInUseEpoch() {
    setTimeout(() => {
      if (this.agGrid && this.agGrid.api) {
        this.agGrid.api.forEachNode(node => {
          if (this.inUseEpoch && node.data.name === this.inUseEpoch.name) {
            this.agGrid.api.ensureIndexVisible(node.rowIndex);
            node.setSelected(true);
          } else {
            node.setSelected(false);
          }
        });
      }
    });
  }

  /**
   * Read an input Epoch file. Emit new epochs if read is successful.
   */
  readFile(file: File): void {
    const reader: FileReader = new FileReader();

    reader.onloadend = e => {
      if (typeof reader.result === 'string') {
        const newEpochs: RavenEpoch[] = JSON.parse(reader.result);
        this.importEpochs.emit(newEpochs);
      }
    };

    reader.readAsText(file);
  }

  /**
   * Event. Called when the row selection changes.
   */
  onSelectionChanged() {
    // Get the first row element since we can only select one row at a time.
    const row = this.agGrid.api.getSelectedRows()[0];

    if (row) {
      this.updateEpochs.emit({ update: { inUseEpoch: row } });
    } else {
      this.updateEpochs.emit({ update: { inUseEpoch: null } });
    }
  }

  /**
   * Ag Grid. Sizes all columns to fit the viewport.
   * Note the setTimeout. We use this to make sure Ag Grid has the most recent data before sizing the columns.
   */
  sizeColumnsToFit() {
    if (this.agGrid && this.agGrid.api) {
      setTimeout(() => this.agGrid.api.sizeColumnsToFit());
    }
  }
}
