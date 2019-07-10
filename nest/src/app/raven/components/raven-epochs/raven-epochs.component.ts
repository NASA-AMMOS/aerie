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
import { FormControl, Validators } from '@angular/forms';
import { AgGridAngular } from 'ag-grid-angular';
import { RavenEpoch, RavenEpochUpdate, RavenUpdate } from '../../models';

import { RavenCheckboxRendererComponent } from '../raven-checkbox-renderer/raven-checkbox-renderer.component';

import { GridOptions, ValueSetterParams } from 'ag-grid-community';
import { utc } from '../../../shared/util';

@Component({
  selector: 'raven-epochs',
  styleUrls: ['./raven-epochs.component.css'],
  templateUrl: './raven-epochs.component.html',
})
export class RavenEpochsComponent implements AfterViewInit, OnChanges {
  @ViewChild('agGrid', { static: true })
  agGrid: AgGridAngular;

  @Input()
  dayCode: string;

  @Input()
  earthSecToEpochSec: number;

  @Input()
  epochs: RavenEpoch[];

  @Input()
  epochsModified: boolean;

  @Input()
  inUseEpoch: RavenEpoch | null;

  @Input()
  projectEpochsUrl: string;

  @Output()
  addEpoch: EventEmitter<RavenEpoch> = new EventEmitter<RavenEpoch>();

  @Output()
  removeEpochs: EventEmitter<RavenEpoch[]> = new EventEmitter<RavenEpoch[]>();

  @Output()
  saveNewEpochFile: EventEmitter<null> = new EventEmitter<null>();

  @Output()
  updateEpochSetting: EventEmitter<RavenUpdate> = new EventEmitter<
    RavenUpdate
  >();

  @Output()
  updateEpochData: EventEmitter<RavenEpochUpdate> = new EventEmitter<
    RavenEpochUpdate
  >();

  @Output()
  updateProjectEpochs: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  epochError: EventEmitter<string> = new EventEmitter<string>();

  gridApi: any;
  public gridOptions: GridOptions;
  columnDefs: any[] = [];
  rowSelection: string;

  epochSecControl: FormControl = new FormControl('', [
    Validators.required,
    Validators.min(0),
  ]);

  ngOnChanges(changes: SimpleChanges) {
    if (changes.epochs) {
      this.columnDefs = this.createColumnDefs();
    }
  }

  ngAfterViewInit() {
    this.sizeColumnsToFit();
    this.rowSelection = 'multiple';
  }

  constructor() {
    this.gridOptions = {
      context: {
        componentParent: this,
      },
    } as GridOptions;
  }

  /**
   * Global Event. Called on window resize.
   * Here we just always make sure the columns are the max size they can possibly be.
   */
  @HostListener('window:resize', ['$event'])
  onResize(e: Event): void {
    this.sizeColumnsToFit();
  }

  public toggleEpoch(index: number) {
    const epoch = this.epochs[index];
    if (epoch.name === '') {
      this.epochError.emit('Cannot select empty epoch name');
    } else if (utc(epoch.value) === 0) {
      this.epochError.emit('Cannot select epoch with invalid value');
    } else {
      this.updateEpochData.emit({
        ...epoch,
        rowIndex: index,
        selected:
          epoch.name === '' || utc(epoch.value) === 0 ? false : !epoch.selected,
      });
    }
  }

  /**
   * Calculates and returns `columnDefs` for use in the grid based on a point.
   */
  createColumnDefs() {
    const update = this.updateEpochData;
    const epochs = this.epochs;
    const epochError = this.epochError;
    const columnDefs: any[] = [
      {
        cellRendererFramework: RavenCheckboxRendererComponent,
        colId: 'select',
        field: 'select',
        headerName: 'Epoch Used',
        hide: false,
        width: 120,
      },
      {
        colId: 'name',
        editable: true,
        field: 'name',
        headerName: 'Name',
        hide: false,
        valueSetter: (params: ValueSetterParams) => {
          const dup = epochs.filter(epoch => epoch.name === params.newValue);
          if (dup.length > 0) {
            epochError.emit('Epoch name already exists');
            return false;
          }
          update.emit({
            ...params.node.data,
            name: params.newValue,
            rowIndex: params.node.childIndex,
          });
          return true;
        },
      },
      {
        colId: 'value',
        editable: true,
        field: 'value',
        headerName: 'Value',
        hide: false,
        valueSetter: (params: ValueSetterParams) => {
          update.emit({
            ...params.node.data,
            rowIndex: params.node.childIndex,
            value: params.newValue,
          });
          return true;
        },
      },
    ];

    return columnDefs;
  }

  /**
   * Helper. Invoke updateEpochSetting if condition is true.
   */
  conditionalUpdateEpochSetting(condition: boolean, updateDict: RavenUpdate) {
    if (condition) {
      this.updateEpochSetting.emit(updateDict);
    }
  }

  onAddRow() {
    const newEpoch = { name: 'newEpoch', selected: false, value: 'value' };
    this.addEpoch.emit(newEpoch);
  }

  onRemoveSelected() {
    const selectedData = this.gridApi.getSelectedRows();
    this.removeEpochs.emit(selectedData);
  }

  onGridReady(params: any) {
    this.gridApi = params.api;
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
