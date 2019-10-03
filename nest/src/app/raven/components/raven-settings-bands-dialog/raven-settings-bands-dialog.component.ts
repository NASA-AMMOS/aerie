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
import { select, Store } from '@ngrx/store';
import keyBy from 'lodash-es/keyBy';
import { Observable, Subscription } from 'rxjs';
import { AppState } from '../../../app-store';
import * as timelineSelectors from '../../../raven/selectors/timeline.selectors';
import { NestConfirmDialogComponent } from '../../../shared/components/nest-confirm-dialog/nest-confirm-dialog.component';
import { StringTMap } from '../../../shared/models';
import { fromDuration } from '../../../shared/util/time';
import { SourceExplorerActions, TimelineActions } from '../../actions';
import {
  RavenCompositeBand,
  RavenStateBand,
  RavenSubBand,
  RavenUpdate,
} from '../../models';
import {
  defaultColors,
  getBandLabel,
  getNumericStateBandsWithUniquePossibleStates,
} from '../../util';

@Component({
  selector: 'raven-settings-bands-dialog',
  styleUrls: ['./raven-settings-bands-dialog.component.css'],
  templateUrl: './raven-settings-bands-dialog.component.html',
})
export class RavenSettingsBandsDialogComponent implements OnDestroy {
  bands$: Observable<RavenCompositeBand[]>;
  selectedSubBand$: Observable<RavenSubBand | null>;

  bandsById: StringTMap<RavenCompositeBand>;
  selectedBandId: string;
  selectedSubBand: RavenSubBand | null;
  subBands: RavenSubBand[];

  colors = defaultColors;
  maxHeight = window.innerHeight - 200;

  heightControl: FormControl = new FormControl('', [
    Validators.required,
    Validators.min(5),
  ]);

  timeDeltaControl: FormControl = new FormControl('', [
    Validators.pattern(/(\d\d\d)T(\d\d):(\d\d):(\d\d)\.?(\d\d\d)?$/),
  ]);

  private subscriptions = new Subscription();

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

    this.bands$ = this.store.pipe(select(timelineSelectors.getBands));
    this.selectedSubBand$ = this.store.pipe(
      select(timelineSelectors.getSelectedSubBand),
    );

    this.subscriptions.add(
      this.bands$.subscribe(bands => {
        this.bandsById = keyBy(bands, 'id');
        if (this.bandsById[this.selectedBandId] !== undefined) {
          this.subBands = this.bandsById[this.selectedBandId].subBands;
        } else {
          this.dialogRef.close();
        }
      }),
    );

    this.subscriptions.add(
      this.selectedSubBand$.subscribe(
        selectedSubBand => (this.selectedSubBand = selectedSubBand),
      ),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  onChangeActivityStyle(
    bandId: string,
    subBandId: string,
    activityStyle: number,
  ) {
    this.updateSubBand({ bandId, subBandId, update: { activityStyle } });
  }

  /**
   * Event. Called when a subBand selection event is fired from the raven-settings-band-dialog component.
   */
  onChangeSelectedSubBand(subBandId: string): void {
    this.store.dispatch(
      TimelineActions.updateTimeline({
        update: { selectedSubBandId: subBandId },
      }),
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
    const confirmDialogRef = this.dialog.open(NestConfirmDialogComponent, {
      data: {
        cancelText: 'No',
        confirmText: 'Yes',
        message: `Are you sure you want to delete subBand ${subBand.label}?`,
      },
      width: '300px',
    });
    this.subscriptions.add(
      confirmDialogRef.afterClosed().subscribe(result => {
        if (result.confirm) {
          this.store.dispatch(
            TimelineActions.removeSubBand({ subBandId: subBand.id }),
          );
          this.store.dispatch(
            SourceExplorerActions.subBandIdRemove({
              sourceIds: subBand.sourceIds,
              subBandId: subBand.id,
            }),
          );
        }
      }),
    );
  }

  /**
   * Event. Helper that emits a new time delta when the user updates the `Time Delta` input.
   */
  onSetTimeDelta(): void {
    if (this.timeDeltaControl.valid && this.timeDeltaControl.value) {
      const timeDelta = fromDuration(this.timeDeltaControl.value);
      if (this.selectedSubBand) {
        this.store.dispatch(
          TimelineActions.updateSubBandTimeDelta({
            bandId: this.selectedBandId,
            subBandId: this.selectedSubBand.id,
            timeDelta,
          }),
        );
      }
    }
  }

  /**
   * Event. Called when an `update-band` event is fired from the raven-settings component.
   */
  updateBand(e: RavenUpdate): void {
    if (e.bandId) {
      this.store.dispatch(
        TimelineActions.updateBand({ bandId: e.bandId, update: e.update }),
      );
    }
  }

  /**
   * Event. Called when an `update-band-and-sub-band` event is fired from the raven-settings component.
   */
  updateBandAndSubBand(e: RavenUpdate): void {
    if (e.bandId && e.subBandId) {
      this.store.dispatch(
        TimelineActions.updateBand({ bandId: e.bandId, update: e.update }),
      );
      this.store.dispatch(
        TimelineActions.updateSubBand({
          bandId: e.bandId,
          subBandId: e.subBandId,
          update: e.update,
        }),
      );
    }
  }

  /**
   * Event. Called when an `update-sub-band` event is fired from the raven-settings component.
   */
  updateSubBand(e: RavenUpdate): void {
    if (e.bandId && e.subBandId) {
      this.store.dispatch(
        TimelineActions.updateSubBand({
          bandId: e.bandId,
          subBandId: e.subBandId,
          update: e.update,
        }),
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
        subBandId: this.selectedSubBand ? this.selectedSubBand.id : '',
        update: { activityLabelFontSize: labelFontSize },
      });
    }
  }

  /**
   * Event. Change plot type changes the height and heightPadding. Height of CompositeBand needs to include heightPadding for top and bottom tick labels to show in a line plot.
   */
  changePlotType(subBand: RavenSubBand, isNumeric: boolean) {
    this.updateSubBand({
      bandId: this.selectedBandId,
      subBandId: subBand.id,
      update: {
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
        heightPadding: showStateChangeTimes ? 10 : 0,
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
        subBandId: this.selectedSubBand ? this.selectedSubBand.id : '',
        update: { stateLabelFontSize: labelFontSize },
      });
    }
  }

  /**
   * Helper. Returns true if the selected band contains more than one resource sub-band or multiple state bands with identical possible states.
   * False otherwise.
   */
  containsMultipleResourceBandsOrIdenticalPossibleValueStateBands(): boolean {
    const subBands = this.bandsById[this.selectedBandId].subBands;
    const resourceCount = subBands.reduce(
      (count, subBand) => (subBand.type === 'resource' ? count + 1 : count),
      0,
    );
    return (
      resourceCount > 1 ||
      getNumericStateBandsWithUniquePossibleStates(this.subBands).length > 0
    );
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
  trackByFn(_: number, item: RavenSubBand) {
    return item.id;
  }
}
