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
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';

import {
  AgGridNg2,
} from 'ag-grid-angular';

import {
  HBCommandDictionary,
} from '../../models/hb-command-dictionary';

/**
 * Display a list of commands that can be loaded into the system
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hb-command-loader',
  styleUrls: ['./hb-command-loader.component.css'],
  templateUrl: './hb-command-loader.component.html',
})
export class HBCommandLoaderComponent implements AfterViewInit, OnChanges {

  @ViewChild('agGrid') agGrid: AgGridNg2;

  @Input() dictionaries: HBCommandDictionary[] = [];
  @Input() selectedId: string | null = null;

  @Output() selectedDictionaryChanged: EventEmitter<HBCommandDictionary> =
    new EventEmitter<HBCommandDictionary>();

  columnDefs: any[] = [
    {
      colId: 'id',
      field: 'id',
      hide: true,
    },
    {
      checkboxSelection: true,
      colId: 'selected',
      field: 'selected',
      headerName: '',
      hide: false,
      width: 60,
    },
    {
      colId: 'name',
      field: 'name',
      headerName: 'Name',
      hide: false,
    },
    {
      colId: 'version',
      field: 'version',
      headerName: 'Version',
      hide: false,
    },
  ];

  rowData: any[] = [];

  /**
   * When the list of dictionaries changes, remap the rowData for the grid
   */
  ngOnChanges(changes: SimpleChanges) {
    if (changes.dictionaries) {
      this.rowData = this.dictionaries.map(d => {
        return {
          id: d.id,
          name: d.name,
          version: d.version,
        };
      });
      this.highlightSelectedRow();
    }

    if (changes.selected) {
      this.highlightSelectedRow();
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
   * Helper that highlights the row in the grid for the selected item.
   * Note the setTimeout. This is to ensure Ag Grid is finished rendering
   * before doing any selection.
   */
  highlightSelectedRow() {
    setTimeout(() => {
      if (this.agGrid && this.agGrid.api) {
        this.agGrid.api.forEachNode(node => {
          if (this.selectedId && node.data.id === this.selectedId) {
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
   * Event. Called when the row selection changes.
   */
  onSelectionChanged() {
    // Get the first row element since we can only select one row at a time.
    const row = this.agGrid.api.getSelectedRows()[0];
    this.selectedDictionaryChanged.emit({ ...row, selected: true });
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
