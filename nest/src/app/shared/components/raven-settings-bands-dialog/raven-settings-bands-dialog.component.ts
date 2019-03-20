/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, Inject, OnDestroy } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { AppState } from '../../../app-store';

import {
  RavenActivityBand,
  RavenCompositeBand,
  RavenDividerBand,
  RavenResourceBand,
  RavenStateBand,
  RavenSubBand,
  RavenUpdate,
  StringTMap,
} from '../../../shared/models';

import * as sourceExplorerActions from '../../../raven/actions/source-explorer.actions';
import * as timelineActions from '../../../raven/actions/timeline.actions';
import { defaultColors, getBandLabel } from '../../util';
import { RavenConfirmDialogComponent } from '../raven-confirm-dialog/raven-confirm-dialog.component';

@Component({
  selector: 'raven-settings-bands-dialog',
  styleUrls: ['./raven-settings-bands-dialog.component.css'],
  templateUrl: './raven-settings-bands-dialog.component.html',
})
export class RavenSettingsBandsDialogComponent implements OnDestroy {
  private ngUnsubscribe: Subject<{}> = new Subject();

  activityStyle: number;
  bandsById: StringTMap<RavenCompositeBand>;
  isNumericStateBand = false;
  selectedBandId: string;
  selectedBackgroundColor = '';
  selectedFillColor = '';
  selectedSubBandId: string;
  selectedLineColor = '';
  subBands: RavenSubBand[];

  colors = defaultColors;

  heightControl: FormControl = new FormControl('', [
    Validators.required,
    Validators.min(5),
  ]);

  constructor(
    public dialogRef: MatDialogRef<RavenSettingsBandsDialogComponent>,
    private dialog: MatDialog,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private store: Store<AppState>,
  ) {
    if (this.data.bandsById) {
      this.bandsById = this.data.bandsById;
    }
    if (this.data.selectedBandId) {
      this.selectedBandId = this.data.selectedBandId;
      this.subBands =
        this.bandsById[this.selectedBandId] &&
        this.bandsById[this.selectedBandId].subBands;
    }
    if (this.data.selectedSubBandId) {
      this.selectedSubBandId = this.data.selectedSubBandId;
      const selectedSubBand = this.bandsById[
        this.selectedBandId
      ].subBands.filter(subBand => subBand.id === this.selectedSubBandId);
      if (selectedSubBand[0].type === 'divider') {
        this.selectedBackgroundColor = (selectedSubBand[0] as RavenDividerBand).color;
      }
      if (
        selectedSubBand[0].type === 'resource' ||
        selectedSubBand[0].type === 'state'
      ) {
        this.selectedLineColor = (selectedSubBand[0] as RavenResourceBand).color;
        this.selectedFillColor = (selectedSubBand[0] as RavenResourceBand).fillColor;
      }
      if (selectedSubBand[0].type === 'state') {
        this.isNumericStateBand = (selectedSubBand[0] as RavenStateBand).isNumeric;
      }
      if (selectedSubBand[0].type === 'activity') {
        this.activityStyle = (selectedSubBand[0] as RavenActivityBand).activityStyle;
      }
    }
  }

  ngOnDestroy(): void {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  onChangeActivityStyle(
    bandId: string,
    subBandId: string,
    activityStyle: number,
  ) {
    this.activityStyle = activityStyle;
    this.updateSubBand({ bandId, subBandId, update: { activityStyle } });
  }

  /**
   * Event. Called when a divider color change event is fired from the raven-settings-band-dialog component.
   */
  onChangeBackgroundColor(bandId: string, subBandId: string, color: string) {
    this.selectedBackgroundColor = color;
    this.updateBand({ bandId, update: { backgroundColor: color } });
    this.updateSubBand({
      bandId,
      subBandId,
      update: { backgroundColor: color },
    });
  }

  /**
   * Event. Called when a line color change event is fired from the raven-settings-band-dialog component.
   */
  onChangeLineColor(bandId: string, subBandId: string, color: string) {
    this.selectedLineColor = color;
    this.updateSubBand({ bandId, subBandId, update: { color: color } });
  }

  /**
   * Event. Called when a fill color change event is fired from the raven-settings-band-dialog component.
   */
  onChangeFillColor(bandId: string, subBandId: string, color: string) {
    this.selectedFillColor = color;
    this.updateSubBand({ bandId, subBandId, update: { fillColor: color } });
  }

  /**
   * Event. Called when a subBand selection event is fired from the raven-settings-band-dialog component.
   */
  onChangeSelectedSubBand(subBandId: string): void {
    this.selectedSubBandId = subBandId;
    this.store.dispatch(
      new timelineActions.UpdateTimeline({ selectedSubBandId: subBandId }),
    );
  }

  /**
   * Closes the dialog.
   */
  onClose() {
    this.dialogRef.close();
  }

  /**
   * Event. Called when a subBand delete event is fired from the raven-settings-band-dialog component.
   */
  onDeleteSubBand(subBand: RavenSubBand) {
    const confirmDialogRef = this.dialog.open(RavenConfirmDialogComponent, {
      data: {
        cancelText: 'No',
        confirmText: 'Yes',
        message: `Are you sure you want to delete subBand ${subBand.label}?`,
      },
      width: '300px',
    });
    confirmDialogRef.afterClosed().subscribe(result => {
      if (result.confirm) {
        this.store.dispatch(new timelineActions.RemoveSubBand(subBand.id));
        this.store.dispatch(
          new sourceExplorerActions.SubBandIdRemove(
            subBand.sourceIds,
            subBand.id,
          ),
        );
        this.subBands = this.subBands.filter(
          sBand => sBand.id !== this.selectedSubBandId,
        );
        if (this.subBands.length > 0) {
          this.selectedSubBandId = this.subBands[0].id;
        } else {
          this.dialogRef.close();
        }
      }
    });
  }

  /**
   * Event. Called when an `update-band` event is fired from the raven-settings component.
   */
  updateBand(e: RavenUpdate): void {
    if (e.bandId) {
      this.store.dispatch(new timelineActions.UpdateBand(e.bandId, e.update));
    }
  }

  /**
   * Event. Called when an `update-band-and-sub-band` event is fired from the raven-settings component.
   */
  updateBandAndSubBand(e: RavenUpdate): void {
    if (e.bandId && e.subBandId) {
      this.store.dispatch(new timelineActions.UpdateBand(e.bandId, e.update));
      this.store.dispatch(
        new timelineActions.UpdateSubBand(e.bandId, e.subBandId, e.update),
      );
    }
  }

  /**
   * Event. Called when an `update-sub-band` event is fired from the raven-settings component.
   */
  updateSubBand(e: RavenUpdate): void {
    if (e.bandId && e.subBandId) {
      this.store.dispatch(
        new timelineActions.UpdateSubBand(e.bandId, e.subBandId, e.update),
      );
    }
  }

  /**
   * Event. Change callback. Only allow activity label font size between the min/max font size ranges.
   */
  activityLabelFontSizeChange(labelFontSize: number) {
    if (labelFontSize > 5 && labelFontSize < 31) {
      this.updateSubBand({
        bandId: this.selectedBandId,
        subBandId: this.selectedSubBandId,
        update: { activityLabelFontSize: labelFontSize },
      });
    }
  }

  /**
   * Event. Change plot type changes the height and heightPadding. Height of CompositeBand needs to include heightPadding for top and bottom tick labels to show in a line plot.
   */
  changePlotType(subBand: RavenSubBand, isNumeric: boolean) {
    this.isNumericStateBand = isNumeric;
    this.updateBand({
      bandId: this.selectedBandId,
      subBandId: subBand.id,
      update: {
        height: isNumeric ? 100 : 50,
        heightPadding: isNumeric ? 20 : 0,
      },
    });

    this.updateSubBand({
      bandId: this.selectedBandId,
      subBandId: subBand.id,
      update: {
        height: isNumeric ? 100 : 50,
        heightPadding:
          isNumeric || (subBand as RavenStateBand).showStateChangeTimes
            ? 10
            : 0,
        isNumeric,
      },
    });
  }

  /**
   * Event. Change showStateChangeTimes requires change in heightPadding for the times to be shown.
   */
  changeShowStateChangeTimes(
    subBand: RavenSubBand,
    showStateChangeTimes: boolean,
  ) {
    this.updateSubBand({
      bandId: this.selectedBandId,
      subBandId: subBand.id,
      update: {
        heightPadding: showStateChangeTimes ? 12 : 0,
        showStateChangeTimes,
      },
    });
  }

  /**
   * Event. Change callback. Only allow state label font size between the min/max font size ranges.
   */
  stateLabelFontSizeChange(labelFontSize: number) {
    if (labelFontSize > 5 && labelFontSize < 31) {
      this.updateSubBand({
        bandId: this.selectedBandId,
        subBandId: this.selectedSubBandId,
        update: { stateLabelFontSize: labelFontSize },
      });
    }
  }

  /**
   * Helper. Returns true if the selected band contains more than one resource sub-band. False otherwise.
   */
  containsMultipleResourceBands(): boolean {
    const subBands = this.bandsById[this.selectedBandId].subBands;
    const resourceCount = subBands.reduce(
      (count, subBand) => (subBand.type === 'resource' ? count + 1 : count),
      0,
    );
    return resourceCount > 1;
  }

  /**
   * Helper. Invoke updateBandAndSubBand if condition is true.
   */
  conditionalUpdateBandAndSubBand(condition: boolean, updateDict: RavenUpdate) {
    if (condition) {
      this.updateBandAndSubBand(updateDict);
    }
  }

  /**
   * Get subBand label including pin and units.
   */
  getSubBandLabel(subBand: RavenSubBand) {
    return getBandLabel(subBand);
  }

  /**
   * trackBy for subBands.
   */
  trackByFn(index: number, item: RavenSubBand) {
    return item.id;
  }
}
