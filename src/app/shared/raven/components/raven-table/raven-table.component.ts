/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  pickBy,
  startsWith,
} from 'lodash';

import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';

import {
  RowNode,
} from 'ag-grid';

import {
  AgGridNg2,
} from 'ag-grid-angular';

import {
  RavenActivityPoint,
  RavenPoint,
} from './../../../models';

import {
  timestamp,
} from './../../../util';

import {
  RavenTableDetailComponent,
} from './../raven-table-detail/raven-table-detail.component';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-table',
  styleUrls: ['./raven-table.component.css'],
  templateUrl: './raven-table.component.html',
})
export class RavenTableComponent implements OnChanges {
  @ViewChild('agGrid') agGrid: AgGridNg2;

  @Input() points: RavenPoint[];
  @Input() selectedBandId: string;
  @Input() selectedPoint: RavenPoint;

  columnDefs: any[] = [];
  rowData: any[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.selectedPoint) {
      this.highlightRowForSelectedPoint();
    }

    if (changes.points && this.points.length) {
      this.columnDefs = this.createColumnDefs(this.points[0]);

      const { rowData, hideFirstColumn } = this.createRowData(this.points);
      this.rowData = rowData;
      this.columnDefs[0].hide = hideFirstColumn; // Hide first column if we don't need to see it (e.g. no activity params or metadata).

      this.groupTimeColumns();
    }
  }

  /**
   * Ag Grid. Returns component to be used in the grids detail row.
   */
  getFullWidthCellRenderer() {
    return RavenTableDetailComponent;
  }

  /**
   * Ag Grid. Returns the row height for a given row in the grid.
   * Takes into account the detail row height.
   */
  getRowHeight(params: any) {
    const rowIsDetailRow = params.node.level === 1;
    return rowIsDetailRow ? 400 : 48;
  }

  /**
   * Ag Grid. Helper that gets child records from a row.
   */
  getNodeChildDetails(record: any) {
    if (record.activityParameters || record.metadata) {
      return {
        children: [
          [record],
        ],
        group: true,
        key: record.id, // The key is used by the default group cellRenderer.
      };
    } else {
      return null;
    }
  }

  /**
   * Ag Grid. Returns if we are a master or detail row.
   */
  isFullWidthCell(rowNode: RowNode) {
    return rowNode.level === 1;
  }

  /**
   * Calculates and returns `columnDefs` for use in the grid based on a point.
   */
  createColumnDefs(point?: RavenPoint): any[] {
    const columnDefs: any[] = [];

    if (point) {
      // First push the sub-grid menu column for opening/closing the detail panel if it exists.
      columnDefs.push({
        cellRenderer: 'agGroupCellRenderer',
        cellRendererParams: {
          suppressCount: true,
          suppressDoubleClickExpand: true,
        },
        colId: 'detail',
        editable: false,
        field: 'detail',
        headerName: '',
        hide: false,
        maxWidth: 25,
        minWidth: 25,
        showRowGroup: true,
        suppressFilter: true,
        suppressMenu: true,
        suppressMovable: true,
        suppressResize: true,
        suppressSizeToFit: true,
        suppressSorting: true,
        suppressToolPanel: true,
        width: 25,
      });

      Object.keys(pickBy(point)).forEach(prop => { // `pickBy` removes undefined or null props.
        // Exclude table columns we do not want to show.
        if (
          typeof point[prop] !== 'object' &&
          !startsWith(prop, '__') &&
          prop !== 'activityId' &&
          prop !== 'activityParameters' &&
          prop !== 'ancestors' &&
          prop !== 'childrenUrl' &&
          prop !== 'descendantsUrl' &&
          prop !== 'endTimestamp' &&
          prop !== 'id' &&
          prop !== 'interpolateEnding' &&
          prop !== 'keywordLine' &&
          prop !== 'legend' &&
          prop !== 'plan' &&
          prop !== 'sourceId' &&
          prop !== 'span' &&
          prop !== 'startTimestamp' &&
          prop !== 'status' &&
          prop !== 'subBandId' &&
          prop !== 'subsystem' &&
          prop !== 'type' &&
          prop !== 'uniqueId'
        ) {
          columnDefs.push({
            colId: prop,
            field: prop,
            headerName: prop.charAt(0).toUpperCase() + prop.slice(1), // Capitalize header.
            hide: false,
          });
        }
      });
    }

    return columnDefs;
  }

  /**
   * Returns `rowData` for use in the grid.
   */
  createRowData(points?: RavenPoint[]) {
    const rowData: any[] = [];

    let hideFirstColumn = true;

    if (points) {
      for (let i = 0, l = points.length; i < l; ++i) {
        const point = this.timestampPoint(points[i]);

        if (
          (point as RavenActivityPoint).activityParameters && (point as RavenActivityPoint).activityParameters.length ||
          (point as RavenActivityPoint).metadata && (point as RavenActivityPoint).metadata.length
        ) {
          hideFirstColumn = false;

          rowData.push({
            ...point,
            detail: '', // Add a `detail` data point so the detail column shows an expand/collapse arrow if needed.
          });
        } else {
          rowData.push(point);
        }
      }
    }

    return {
      hideFirstColumn,
      rowData,
    };
  }

  /**
   * Helper that groups columns based on time (i.e. start, end, and duration).
   */
  groupTimeColumns() {
    if (this.columnDefs.length) {
      let startIndex = -1;
      let endIndex = -1;
      let durationIndex = -1;

      this.columnDefs.forEach((column, i) => {
        if (column.field === 'start') {
          startIndex = i;
        } else if (column.field === 'end') {
          endIndex = i;
        } else if (column.field === 'duration') {
          durationIndex = i;
        }
      });

      // Rearrange start and end first and place them at the end of the columns.
      if (startIndex > -1 && endIndex > -1) {
        // Keep a reference to the start and end column.
        const startColumn = this.columnDefs[startIndex];
        const endColumn = this.columnDefs[endIndex];

        // Remove both columns.
        // This correctly accounts for the case if there is a column between end and start.
        this.columnDefs.splice(startIndex, 1);
        this.columnDefs.splice(endIndex, 1);

        // Add back start and end columns in the correct order.
        this.columnDefs.push(startColumn);
        this.columnDefs.push(endColumn);
      }

      // Next (only after start and end have been rearranged), move duration to the end column.
      // This gives the order: start | end | duration.
      if (durationIndex > -1) {
        const durationColumn = this.columnDefs[durationIndex];
        this.columnDefs.splice(durationIndex, 1);
        this.columnDefs.push(durationColumn);
      }
    }
  }

  /**
   * Helper that highlights the row in the grid for the currently selected point.
   * Note the setTimeout. This is to ensure Ag Grid is finished rendering before doing any selection.
   */
  highlightRowForSelectedPoint() {
    setTimeout(() => {
      if (this.agGrid && this.agGrid.api) {
        this.agGrid.api.forEachNode(node => {
          if (this.selectedPoint && node.data.uniqueId === this.selectedPoint.uniqueId) {
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
   * Button Click Callback. Called when the `Reset Columns` button is clicked.
   * This resets the columns to their original state.
   */
  onResetColumns() {
    this.agGrid.columnApi.resetColumnState();
  }

  /**
   * Helper that timestamps a points start/end value.
   */
  timestampPoint(point: any) {
    if (point.start) {
      point = {
        ...point,
        start: timestamp(point.start),
      };
    }

    if (point.end) {
      point = {
        ...point,
        end: timestamp(point.end),
      };
    }

    return point;
  }
}
