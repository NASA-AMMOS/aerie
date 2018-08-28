/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { AfterViewInit, ChangeDetectionStrategy, Component } from '@angular/core';
import { GridOptions } from 'ag-grid';
import { ICellRendererAngularComp } from 'ag-grid-angular';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-table-detail',
  styleUrls: ['./raven-table-detail.component.css'],
  templateUrl: './raven-table-detail.component.html',
})
export class RavenTableDetailComponent implements AfterViewInit, ICellRendererAngularComp {
  public gridOptions: GridOptions;
  public parentRecord: any;

  constructor() {
    this.gridOptions = <GridOptions>{};
  }

  agInit(params: any): void {
    this.parentRecord = params.data[0];
  }

  ngAfterViewInit() {
    if (this.gridOptions && this.gridOptions.api && this.parentRecord) {
      this.gridOptions.api.setColumnDefs(this.createColumnDefs(this.parentRecord.type));
      this.gridOptions.api.setRowData(this.parentRecord.rows);
      this.gridOptions.api.sizeColumnsToFit();
    }
  }

  /**
   * Calculates and returns `columnDefs` for use in the grid based on a point.
   */
  createColumnDefs(headerName: string): any[] {
    return [
      {
        children: [
          { headerName: 'Name', field: 'Name' },
          { headerName: 'Value', field: 'Value' },
        ],
        headerName,
      },
    ];
  }

  /**
   * Ag Grid. Needed for `ICellRendererAngularComp` implementation.
   */
  refresh(): boolean {
    return false;
  }
}
