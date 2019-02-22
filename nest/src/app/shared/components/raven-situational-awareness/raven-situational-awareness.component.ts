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
import { AgGridNg2 } from 'ag-grid-angular';
import { fromDuration, utc } from '../../../shared/util/time';

import {
  RavenSituationalAwarenessPefEntry,
  RavenUpdate,
} from '../../../shared/models';

@Component({
  selector: 'raven-situational-awareness',
  styleUrls: ['./raven-situational-awareness.component.css'],
  templateUrl: './raven-situational-awareness.component.html',
})
export class RavenSituationalAwarenessComponent
  implements AfterViewInit, OnChanges {
  @ViewChild('agGrid')
  agGrid: AgGridNg2;

  @Input()
  nowMinus: number | null;

  @Input()
  nowPlus: number | null;

  @Input()
  pageDuration: number | null;

  @Input()
  pefEntries: RavenSituationalAwarenessPefEntry[] | null;

  @Input()
  situationalAware: boolean;

  @Input()
  startTime: number | null;

  @Input()
  useNow: boolean;

  @Output()
  changeSituationalAwareness: EventEmitter<boolean> = new EventEmitter<
    boolean
  >();

  @Output()
  updateSituationalAwarenessSettings: EventEmitter<
    RavenUpdate
  > = new EventEmitter<RavenUpdate>();

  columnDefs: any[] = [];
  rowData: any[] = [];

  nowMinusControl: FormControl = new FormControl('', [
    Validators.pattern(/(\d\d\d)T(\d\d):(\d\d):(\d\d)\.?(\d\d\d)?$/),
  ]);

  nowPlusControl: FormControl = new FormControl('', [
    Validators.pattern(/(\d\d\d)T(\d\d):(\d\d):(\d\d)\.?(\d\d\d)?$/),
  ]);

  pageDurationControl: FormControl = new FormControl('', [
    Validators.pattern(/(\d\d\d)T(\d\d):(\d\d):(\d\d)\.?(\d\d\d)?$/),
  ]);

  startTimeControl: FormControl = new FormControl('', [
    Validators.pattern(/(\d\d\d\d)-(\d\d\d)T(\d\d):(\d\d):(\d\d)\.?(\d\d\d)?$/),
  ]);

  ngOnChanges(changes: SimpleChanges) {
    if (changes.pefEntries) {
      this.columnDefs = this.createColumnDefs();
      this.rowData = this.pefEntries as any[];
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
        colId: 'sequenceId',
        field: 'sequenceId',
        headerName: 'Sequence Id',
        hide: false,
      },
      {
        colId: 'pefFile',
        field: 'pefFile',
        headerName: 'Pef File',
        hide: false,
      },
      {
        colId: 'startTime',
        field: 'startTime',
        headerName: 'Start Time',
        hide: false,
      },
      {
        colId: 'endTime',
        field: 'endTime',
        headerName: 'End Time',
        hide: false,
      },
    ];

    return columnDefs;
  }

  /**
   * Event. Helper that emits a new now minus when the user presses enter in the `Minus Delta` input.
   */
  onSetMinusDelta(): void {
    if (this.nowMinusControl.valid && this.nowMinusControl.value) {
      const newNowMinus = fromDuration(this.nowMinusControl.value);
      this.updateSetting('nowMinus', newNowMinus);
    }
  }

  /**
   * Event. Helper that emits a new now plus when the user presses enter in the `Plus Delta` input.
   */
  onSetPlusDelta(): void {
    if (this.nowPlusControl.valid && this.nowPlusControl.value) {
      const newNowPlus = fromDuration(this.nowPlusControl.value);
      this.updateSetting('nowPlus', newNowPlus);
    }
  }

  /**
   * Event. Helper that emits a new page duration when the user presses enter in the `Page Duration` input.
   */
  onSetPageDuration(): void {
    if (this.pageDurationControl.valid && this.pageDurationControl.value) {
      const newPageDuration = fromDuration(this.pageDurationControl.value);
      this.updateSetting('pageDuration', newPageDuration);
    }
  }

  /**
   * Event. Helper that emits a new start time when the user presses enter in the `Start Time` input.
   */
  onSetStartTime(): void {
    if (this.startTimeControl.valid && this.startTimeControl.value) {
      const newStartTime = utc(this.startTimeControl.value);
      this.updateSetting('startTime', newStartTime);
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

  /**
   * Helper. Emit updateSituationalAwarenessSettings to update the value of a prop.
   */
  updateSetting(propName: string, value: number | null) {
    this.updateSituationalAwarenessSettings.emit({
      update: {
        [propName]: value !== 0 ? value : null,
      },
    });
  }
}
