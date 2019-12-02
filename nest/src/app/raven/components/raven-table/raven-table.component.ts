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
import { FormControl } from '@angular/forms';
import { AgGridAngular } from 'ag-grid-angular';
import {
  AgGridEvent,
  IDatasource,
  SelectionChangedEvent,
} from 'ag-grid-community';
import { GridOptions, ValueSetterParams } from 'ag-grid-community';
import pickBy from 'lodash-es/pickBy';
import startsWith from 'lodash-es/startsWith';
import {
  RavenActivityPoint,
  RavenPoint,
  RavenPointIndex,
  RavenPointUpdate,
  RavenSubBand,
  RavenUpdate,
} from '../../models';
import {
  createNewActivityPoint,
  createNewResourcePoint,
  createNewStatePoint,
  dateToTimestring,
  dhms,
  timestamp,
  toDuration,
  utc,
} from '../../util';

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
  mode: string;

  @Input()
  points: RavenPoint[];

  @Input()
  selectedBandId: string;

  @Input()
  selectedSubBand: RavenSubBand;

  @Input()
  selectedPoint: RavenPoint | null;

  @Output()
  addPointToSubBand: EventEmitter<RavenPointIndex> = new EventEmitter<
    RavenPointIndex
  >();

  @Output()
  removePointsInSubBand: EventEmitter<RavenPoint[]> = new EventEmitter<
    RavenPoint[]
  >();

  @Output()
  save: EventEmitter<null> = new EventEmitter<null>();

  @Output()
  selectPoint: EventEmitter<RavenPoint> = new EventEmitter<RavenPoint>();

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
  infiniteScroll = false;
  infiniteScrollThreshold = 10000;
  rowData: any[] = [];
  rowSelection = 'multiple';

  dataSource: IDatasource;

  private gridApi: any;
  public gridOptions: GridOptions;

  filterControl: FormControl = new FormControl('', [this.validateFilter]);

  constructor() {
    this.gridOptions = {
      rowHeight: 28,
    };
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.activityFilter) {
      this.displayedFilter = this.activityFilter;
    }
    if (changes.activityFilter) {
      const selectedPoint = this.selectedPoint;
      if (this.activityFilter !== undefined && this.activityFilter !== '') {
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
      this.infiniteScroll =
        this.points && this.points.length > this.infiniteScrollThreshold;

      if (this.infiniteScroll) {
        this.gridOptions.infiniteInitialRowCount = 1;
        this.gridOptions.rowModelType = 'infinite';
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

        if (this.gridOptions.api) {
          console.log('dataSource set');
          this.gridOptions.api.setDatasource(this.dataSource);
        }
        this.gridOptions.infiniteInitialRowCount = 1;
        this.gridOptions.rowModelType = 'infinite';
        this.gridOptions.datasource = this.dataSource;
      } else {
        this.gridOptions.rowModelType = undefined;
        this.gridOptions.datasource = undefined;
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
   * Calculates and returns `columnDefs` for use in the grid based on a point.
   */
  createColumnDefs(point: RavenPoint) {
    const bandId = this.selectedBandId;
    const children: any[] = [];
    const subBandId = this.selectedSubBand.id;
    const updatePoint = this.updatePoint;

    if (point) {
      children.push({
        colId: 'index',
        field: 'index',
        headerName: '',
        hide: false,
        resizable: true,
        width: 50,
      });

      Object.keys(
        pickBy(point, prop => prop !== null && prop !== undefined),
      ).forEach(prop => {
        // `pickBy` removes nulls or undefined props.
        // Exclude table columns we do not want to show.
        const excludeProps = [
          'activityId',
          'activityParameters',
          'ancestors',
          'childrenUrl',
          'color',
          'hidden',
          'descendantsUrl',
          'endTimestamp',
          'expandedFromPointId',
          'expansion',
          'id',
          'interpolateEnding',
          'isDuration',
          'isTime',
          'keywordLine',
          'legend',
          'plan',
          'pointStatus',
          'selected',
          'sourceId',
          'span',
          'startTimestamp',
          'status',
          'subBandId',
          'subsystem',
          'type',
          'uniqueId',
        ];
        if (
          typeof point[prop] !== 'object' &&
          !startsWith(prop, '__') &&
          !excludeProps.includes(prop)
        ) {
          children.push({
            colId: prop,
            editable: this.selectedSubBand.editable && this.colEditable(prop),
            field: prop,
            headerName: prop.charAt(0).toUpperCase() + prop.slice(1), // Capitalize header.
            hide: false,
            resizable: true,
            sortable: true,
            valueSetter:
              this.selectedSubBand.editable && this.colEditable(prop)
                ? (params: ValueSetterParams) => {
                    const timeIds = ['start', 'end'];
                    const value = params.newValue;
                    updatePoint.emit({
                      bandId,
                      pointId: params.node.data.id,
                      subBandId,
                      update: {
                        [params.column.getId()]: timeIds.includes(
                          params.column.getId(),
                        )
                          ? utc(value)
                          : value,
                        pointStatus:
                          params.node.data.pointStatus === 'added'
                            ? 'added'
                            : 'updated',
                      },
                    });

                    // Recalculate duration when either start or end changed.
                    if (
                      timeIds.includes(params.column.getId()) &&
                      params.node.data.duration !== undefined
                    ) {
                      updatePoint.emit({
                        bandId,
                        pointId: params.node.data.id,
                        subBandId,
                        update: {
                          duration:
                            params.column.getId() === 'start'
                              ? utc(params.node.data.end) - utc(value)
                              : utc(value) - utc(params.node.data.start),
                        },
                      });
                    }
                    this.highlightRowForSelectedPoint();
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

  /**
   * Helper. Return if column is editable.
   */
  colEditable(prop: string): boolean {
    return ['activityName', 'start', 'end', 'value'].includes(prop);
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
      if (points[i].pointStatus !== 'deleted') {
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
            if (this.gridOptions !== undefined && this.gridOptions.api) {
              this.gridOptions.api.ensureIndexVisible(node.rowIndex);
            }
            node.data.selected = true;
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

  /**
   * Helper. Returns true if no row is selected.
   */
  noneSelected() {
    if (this.agGrid && this.agGrid.api) {
      return this.agGrid.api.getSelectedRows().length === 0;
    } else {
      return true;
    }
  }

  /**
   * Event. Helper that emits a new point to be added.
   */
  onAdd() {
    let newPoint: RavenPoint;
    if (this.selectedSubBand.type === 'activity') {
      newPoint = createNewActivityPoint(
        this.selectedSubBand.sourceIds[0],
        this.selectedSubBand.id,
      );
    } else if (this.selectedSubBand.type === 'state') {
      newPoint = createNewStatePoint(
        this.selectedSubBand.sourceIds[0],
        this.selectedSubBand.id,
      );
    } else {
      newPoint = createNewResourcePoint(
        this.selectedSubBand.sourceIds[0],
        this.selectedSubBand.id,
      );
    }

    this.addPointToSubBand.emit({
      index:
        this.gridApi.getSelectedNodes().length > 0
          ? this.gridApi.getSelectedNodes()[0].rowIndex + 1
          : this.gridApi.getDisplayedRowCount(),
      point: newPoint,
    });

    // Select the newly added point.
    this.selectPoint.emit(newPoint);
  }

  /**
   * Event. Helper that emits points to be removed.
   */
  onRemove() {
    const selectedPoints = this.gridApi.getSelectedRows();
    this.removePointsInSubBand.emit(selectedPoints);
  }

  /**
   * Event. Helper that emits a selected point.
   */
  onSelectionChanged(event: SelectionChangedEvent) {
    if (event.api.getSelectedNodes().length > 0) {
      const point = event.api.getSelectedNodes()[0].data;
      if (!point.selected && event.api.getSelectedNodes()[0].isSelected()) {
        this.selectPoint.emit(point);
      }
    }
  }

  /**
   * Helper that tests if the filter value is a valid RegExp.
   */
  validateFilter(filter: FormControl) {
    try {
      RegExp(filter.value);
      return null;
    } catch (ex) {
      return {
        validateFilter: { valid: false },
      };
    }
  }
}
