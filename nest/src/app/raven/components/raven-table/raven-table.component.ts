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
  ViewChild,
} from '@angular/core';

import { AgGridAngular } from 'ag-grid-angular';
import { AgGridEvent, IDatasource, RowNode } from 'ag-grid-community';
import pickBy from 'lodash-es/pickBy';
import startsWith from 'lodash-es/startsWith';

import {
  dateToTimestring,
  dhms,
  timestamp,
  toDuration,
  utc,
} from '../../../shared/util';
import {
  RavenActivityPoint,
  RavenPoint,
  RavenPointUpdate,
  RavenSubBand,
  RavenUpdate,
} from '../../models';
import { RavenTableDetailComponent } from './raven-table-detail.component';

import { GridOptions } from 'ag-grid-community';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-table',
  styleUrls: ['./raven-table.component.css'],
  templateUrl: './raven-table.component.html',
})
export class RavenTableComponent implements OnChanges {
  @ViewChild('agGrid', { static: false })
  agGrid: AgGridAngular;

  @Input()
  activityFilter: string;

  @Input()
  activityInitiallyHidden: boolean;

  @Input()
  points: RavenPoint[];

  @Input()
  selectedBandId: string;

  @Input()
  selectedSubBand: RavenSubBand;

  @Input()
  selectedPoint: RavenPoint;

  @Output()
  updateFilter: EventEmitter<RavenUpdate> = new EventEmitter<RavenUpdate>();

  @Output()
  updateFilterActivityInSubBand: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  updatePoint: EventEmitter<RavenPointUpdate> = new EventEmitter<
    RavenPointUpdate
  >();

  @Output()
  updateTableColumns: EventEmitter<any> = new EventEmitter<any>();

  displayedFilter: string;

  columnDefs: any[] = [];
  rowData: any[] = [];

  dataSource: IDatasource;

  private gridApi: any;
  public gridOptions: GridOptions;

  constructor() {
    this.gridOptions = {
      infiniteInitialRowCount: 1,
      rowModelType: 'infinite',
    };
    this.dataSource = {
      getRows: (params: any) => {
        const data = this.rowData.slice(params.startRow, params.endRow);
        let lastRow = -1;
        if (this.rowData.length <= params.endRow) {
          lastRow = this.rowData.length;
        }
        params.successCallback(data, lastRow);
      },
    };

    this.gridOptions.datasource = this.dataSource;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.activityFilter) {
      this.displayedFilter = this.activityFilter;
    }
    if (changes.activityFilter) {
      const selectedPoint = this.selectedPoint;
      if (this.activityFilter !== '') {
        this.gridOptions.getRowStyle = function(params: any) {
          if (
            selectedPoint &&
            params.data &&
            params.data.uniqueId === selectedPoint.uniqueId
          ) {
            return { background: 'yellow' };
          } else if (
            params.data &&
            params.data.type === 'activity' &&
            !params.data.hidden
          ) {
            return { background: '#e8eaf6' };
          } else {
            return {};
          }
        };
      } else {
        this.gridOptions.getRowStyle = function(params: any) {
          if (
            selectedPoint &&
            params.data &&
            params.data.uniqueId === selectedPoint.uniqueId
          ) {
            return { background: 'yellow' };
          } else {
            return {};
          }
        };
      }
    }
    // Points (any length).
    if (changes.points) {
      this.rowData = this.createRowData(this.points);
      if (this.gridOptions.api) {
        this.gridOptions.api.setDatasource(this.dataSource);
      }
    }
    // Points (length > 0).
    if (changes.points && this.points.length && this.selectedSubBand) {
      if (this.selectedSubBand.tableColumns.length) {
        // Load columns if they exist on the band.
        this.columnDefs = this.selectedSubBand.tableColumns;
      } else {
        // Otherwise generate new columns dynamically.
        this.columnDefs = this.createColumnDefs(this.points[0]);
      }

      this.setColumnHeader(); // Set the header since it may have changed if we are coming from saved columns.
      this.highlightRowForSelectedPoint();

      if (this.gridApi) {
        this.gridApi.refreshInfiniteCache();
      }
    }

    // Selected Sub Band.
    if (changes.selectedSubBand && this.selectedSubBand) {
      this.setColumnHeader();
    }

    // Selected Point.
    if (changes.selectedPoint) {
      this.highlightRowForSelectedPoint();
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
    return rowIsDetailRow ? 300 : 28;
  }

  /**
   * Ag Grid. Helper that gets child records from a row.
   * Note the Ag Grid quirk of `children` being an array-of-arrays.
   */
  getNodeChildDetails(record: any) {
    if (record.activityParameters || record.metadata) {
      const children = [];

      // Add activity parameters table only if we have activity parameters.
      if (record.activityParameters && record.activityParameters.length) {
        children.push([
          { type: 'Activity Parameters', rows: record.activityParameters },
        ]);
      }

      // Add metadata table only if we have metadata.
      if (record.metadata && record.metadata.length) {
        children.push([{ type: 'Metadata', rows: record.metadata }]);
      }

      return {
        children,
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
  createColumnDefs(point: RavenPoint) {
    const bandId = this.selectedBandId;
    const children: any[] = [];
    const subBandId = this.selectedSubBand.id;
    const updatePoint = this.updatePoint;

    if (point) {
      // First push the sub-grid menu column for opening/closing the detail panel if it exists.
      children.push({
        cellRenderer: 'agGroupCellRenderer',
        cellRendererParams: {
          suppressCount: true,
          suppressDoubleClickExpand: true,
        },
        colId: 'index',
        field: 'index',
        headerName: '',
        hide: false,
        width: 70,
      });

      Object.keys(
        pickBy(point, prop => prop !== null && prop !== undefined),
      ).forEach(prop => {
        // `pickBy` removes nulls or undefined props.
        // Exclude table columns we do not want to show.
        if (
          typeof point[prop] !== 'object' &&
          !startsWith(prop, '__') &&
          prop !== 'activityId' &&
          prop !== 'activityParameters' &&
          prop !== 'ancestors' &&
          prop !== 'childrenUrl' &&
          prop !== 'color' &&
          prop !== 'hidden' &&
          prop !== 'descendantsUrl' &&
          prop !== 'editable' &&
          prop !== 'endTimestamp' &&
          prop !== 'expandedFromPointId' &&
          prop !== 'expansion' &&
          prop !== 'id' &&
          prop !== 'interpolateEnding' &&
          prop !== 'isDuration' &&
          prop !== 'isTime' &&
          prop !== 'keywordLine' &&
          prop !== 'legend' &&
          prop !== 'plan' &&
          prop !== 'selected' &&
          prop !== 'sourceId' &&
          prop !== 'span' &&
          prop !== 'startTimestamp' &&
          prop !== 'status' &&
          prop !== 'subBandId' &&
          prop !== 'subsystem' &&
          prop !== 'type' &&
          prop !== 'uniqueId'
        ) {
          children.push({
            colId: prop,
            editable: point.editable && this.colEditable(prop),
            field: prop,
            headerName: prop.charAt(0).toUpperCase() + prop.slice(1), // Capitalize header.
            hide: false,
            valueSetter:
              point.editable && this.colEditable(prop)
                ? function(params: any) {
                    const times = ['start', 'end'];
                    console.log(
                      'params.node.data: ' + JSON.stringify(params.node.data),
                    );
                    console.log(
                      'params.column.id: ' +
                        JSON.stringify(params.column.getId()),
                    );
                    console.log(
                      'params.node.rowIndex: ' + params.node.rowIndex,
                    );
                    console.log(
                      'params.newValue: ' + JSON.stringify(params.newValue),
                    );
                    const value = params.newValue;
                    updatePoint.emit({
                      bandId,
                      pointId: params.node.data.id,
                      subBandId,
                      update: {
                        [params.column.getId()]: times.includes(
                          params.column.getId(),
                        )
                          ? utc(value)
                          : value,
                      },
                    });
                    return true;
                  }
                : null,
          });
        }
      });
    }

    return [
      {
        children: this.groupTimeColumns(children),
        groupId: 'header',
        headerName: this.selectedSubBand.label,
      },
    ];
  }

  colEditable(prop: string): boolean {
    const editables = ['activityName', 'start', 'end', 'value'];
    return editables.includes(prop);
  }

  /**
   * Returns `rowData` for use in the grid.
   * Makes sure points have a properly formatted timestamp and an index.
   *
   * `pointsByActivityId` keeps tracks of points with unique activity IDs we have already seen
   * to make sure we don't ever keep two activities with the same IDs in a point array.
   *
   * Notice how we are looping through the points only once here for max performance.
   */
  createRowData(points: RavenPoint[] = []) {
    const newPoints = [];
    const pointsByActivityId = {};
    let index = 0;

    for (let i = 0, l = points.length; i < l; ++i) {
      const point = {
        ...this.timestampPoint(points[i]),
        index,
      } as RavenActivityPoint; // This is so typings work out.

      if (point.type !== 'activity') {
        newPoints.push(point);
        ++index;
      } else if (
        point.type === 'activity' &&
        !pointsByActivityId[point.uniqueId]
      ) {
        pointsByActivityId[point.uniqueId] = true; // Track that we have now seen this unique activity id so we don't add it again.
        newPoints.push(point);
        ++index;
      }
    }

    return newPoints;
  }

  /**
   * Helper that emits an `updateTableColumns` event.
   * Note the setTimeout. This is to ensure Ag Grid has the most recent columns before emitting them.
   */
  emitUpdateTableColumns(tableColumns?: any[]) {
    setTimeout(() =>
      this.updateTableColumns.emit({
        bandId: this.selectedBandId,
        subBandId: this.selectedSubBand.id,
        update: {
          tableColumns: tableColumns || this.getColumnState(),
        },
      }),
    );
  }

  /**
   * Helper that returns column state based on the current column defs and api state.
   * Ag Grid does not return the api state or sort with the column def so we have to do this manually :(.
   */
  getColumnState() {
    if (this.agGrid) {
      return [
        {
          ...this.agGrid.columnApi.getColumnGroupState()[0],
          children: this.agGrid.columnApi.getColumnState().map(column => ({
            ...this.agGrid.columnApi.getColumn(column.colId).getColDef(),
            ...this.agGrid.api
              .getSortModel()
              .find(sort => sort.colId === column.colId),
            ...column,
          })),
        },
      ];
    }
    return [];
  }

  /**
   * Helper that groups columns based on time (i.e. start, end, and duration).
   */
  groupTimeColumns(columns: any[]) {
    const columnDefs = [...columns];

    if (columnDefs.length) {
      let startIndex = -1;
      let endIndex = -1;
      let durationIndex = -1;

      columnDefs.forEach((column: any, i: number) => {
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
        const startColumn = columnDefs[startIndex];
        const endColumn = columnDefs[endIndex];

        // Remove both columns.
        // This correctly accounts for the case if there is a column between end and start.
        columnDefs.splice(startIndex, 1);
        columnDefs.splice(endIndex, 1);

        // Add back start and end columns in the correct order.
        columnDefs.push(startColumn);
        columnDefs.push(endColumn);
      }

      // Next (only after start and end have been rearranged), move duration to the end column.
      // This gives the order: start | end | duration.
      if (durationIndex > -1) {
        const durationColumn = columnDefs[durationIndex];
        columnDefs.splice(durationIndex, 1);
        columnDefs.push(durationColumn);
      }
    }

    return columnDefs;
  }

  /**
   * Helper that highlights the row in the grid for the currently selected point.
   * Note the setTimeout. This is to ensure Ag Grid is finished rendering before doing any selection.
   */
  highlightRowForSelectedPoint() {
    setTimeout(() => {
      if (this.agGrid && this.agGrid.api) {
        this.agGrid.api.forEachNode(node => {
          if (
            this.selectedPoint &&
            node.data &&
            node.data.uniqueId === this.selectedPoint.uniqueId
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

  onGridReady(params: AgGridEvent) {
    this.gridApi = params.api;
  }

  /**
   * Button Click Callback. Called when the `Reset Columns` button is clicked.
   * This resets the columns to their original state.
   */
  onResetColumns() {
    this.columnDefs = this.createColumnDefs(this.points[0]);
    this.emitUpdateTableColumns([]);
  }

  /**
   * Helper that sets the current column header.
   * Note the setTimeout. This is to ensure Ag Grid is finished rendering before setting the new header.
   */
  setColumnHeader() {
    setTimeout(() => {
      if (this.agGrid && this.agGrid.columnApi) {
        const header = this.agGrid.columnApi.getColumnGroup('header');

        if (header) {
          header.getColGroupDef().headerName = this.selectedSubBand.label;
          this.agGrid.api.refreshHeader();
        }
      }
    });
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

    if (point.duration) {
      point = {
        ...point,
        duration: dhms(point.duration),
      };
    }

    if (point.value !== undefined && point.isDuration) {
      // If we have a point form a duration resource band, we need to make sure to format the value accordingly.
      point = {
        ...point,
        value: toDuration(point.value, true),
      };
    }

    if (point.value !== undefined && point.isTime) {
      // If we have a point form a time resource band, we need to make sure to format the value accordingly.
      point = {
        ...point,
        value: dateToTimestring(new Date(point.value * 1000), true),
      };
    }

    return point;
  }
}
