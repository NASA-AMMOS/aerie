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
import { RavenSource, RavenUpdate, StringTMap } from '../../../shared/models';

@Component({
  selector: 'raven-output',
  styleUrls: ['./raven-output.component.css'],
  templateUrl: './raven-output.component.html',
})
export class RavenOutputComponent implements AfterViewInit, OnChanges {
  @ViewChild('agGrid')
  agGrid: AgGridNg2;

  @Input()
  allInOneFile: boolean;

  @Input()
  allInOneFilename: string;

  @Input()
  decimateOutputData: boolean;

  @Input()
  filtersByTarget: StringTMap<StringTMap<string[]>>;

  @Input()
  outputFormat: string;

  @Input()
  outputSourceIdsByLabel: StringTMap<string[]>;

  @Input()
  subBandSourceIdsByLabel: StringTMap<string[]>;

  @Input()
  treeBySourceId: StringTMap<RavenSource>;

  @Output()
  createOutput: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  updateOutputSettings: EventEmitter<RavenUpdate> = new EventEmitter<
    RavenUpdate
  >();

  columnDefs: any[] = [];
  rowData: any[] = [];
  rowSelection: string;

  ngAfterViewInit() {
    this.sizeColumnsToFit();
    this.applyCurrentOutputSelections();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.allInOneFile) {
      this.allInOneFile = changes.allInOneFile.currentValue;
    }

    if (changes.subBandSourceIdsByLabel) {
      this.subBandSourceIdsByLabel =
        changes.subBandSourceIdsByLabel.currentValue;
      this.rowData = this.createRowData(this.subBandSourceIdsByLabel);
      this.columnDefs = this.createColumnDefs();
      this.rowSelection = 'multiple';
      this.sizeColumnsToFit();
      this.applyCurrentOutputSelections();
    }
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
   * Set checkbox for currently selected sources.
   */
  applyCurrentOutputSelections() {
    setTimeout(() => {
      if (this.agGrid && this.agGrid.api) {
        this.agGrid.api.forEachNode(node => {
          if (
            Object.keys(this.outputSourceIdsByLabel).includes(node.data.label)
          ) {
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
   * Calculates and returns `columnDefs` for use in the grid.
   */
  createColumnDefs() {
    const columnDefs: any[] = [
      {
        checkboxSelection: true,
        colId: 'select',
        field: 'select',
        headerName: '',
        hide: false,
        width: 15,
      },
      {
        colId: 'label',
        field: 'label',
        headerName: 'Label',
        hide: false,
        width: 80,
      },
      {
        colId: 'name',
        field: 'name',
        headerName: 'Source Name',
        hide: false,
        width: 80,
      },
      {
        colId: 'id',
        field: 'id',
        headerName: 'Path',
        hide: false,
      },
      {
        colId: 'filter',
        field: 'filter',
        headerName: 'Filter',
        hide: false,
        width: 40,
      },
    ];

    return columnDefs;
  }

  /**
   * Build row data for the output table.
   * Returns rows based on  the name, filter, path from sourceId, and label.
   */
  createRowData(subBandSourceIdsByLabel: StringTMap<string[]>) {
    const rowData: any[] = [];

    Object.keys(subBandSourceIdsByLabel).forEach(label => {
      const sourceIds = subBandSourceIdsByLabel[label];
      let firstGraphableLabel = true;
      sourceIds.forEach(sourceId => {
        if (
          this.treeBySourceId[sourceId].type !== 'graphableFilter' ||
          firstGraphableLabel
        ) {
          const pathNameArgs = sourceId.match(
            new RegExp('(.*)/([^\\?]*)(\\?.*)?'),
          );

          if (pathNameArgs) {
            const [, path, name, args] = pathNameArgs;
            let filter = '';

            if (args) {
              const labelFilter = args.match(
                new RegExp('\\?label=(.*)&filter=(.*)'),
              );

              if (labelFilter) {
                label = labelFilter[1];
                filter = labelFilter[2];
              }
            }

            rowData.push({
              filter,
              id: `${path}/${name}`,
              label,
              name,
            });
          }
          firstGraphableLabel = false;
        }
      });
    });

    return rowData;
  }

  /**
   * Validator. Invalid if all-in-one file and filename is empty.
   * Note: Use this function instead of FormGroup validator because when file format is change to 'JSON', allInOneFile is auto set to false, the change
   * is not reflected in the form validator.
   */
  isValid() {
    return this.allInOneFile && this.allInOneFilename.length === 0
      ? false
      : true;
  }

  /**
   * Helper. Returns true if more than one row is selected.
   */
  moreThanOneSelected() {
    if (this.agGrid && this.agGrid.api) {
      return this.agGrid.api.getSelectedRows().length > 1 ? true : false;
    } else {
      return false;
    }
  }

  /**
   * Event. Called when the `Create Output` button is clicked.
   */
  onCreateOutput() {
    this.createOutput.emit();
  }

  /**
   * Event. Called when a selection is made.
   */
  onSelectionChanged() {
    const rows = this.agGrid.api.getSelectedRows();
    this.updateOutputSettings.emit({
      update: {
        outputSourceIdsByLabel: rows.reduce((obj, row) => {
          obj[row.label]
            ? obj[row.label].push(row.id)
            : (obj[row.label] = [row.id]);
          return obj;
        }, {}),
      },
    });
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
