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
  Output,
} from '@angular/core';

import {
  RavenCompositeBand,
  RavenStateBand,
  RavenSubBand,
  RavenUpdate,
  StringTMap,
} from '../../../shared/models';
import { getBandLabel } from '../../util';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-settings-bands',
  styleUrls: ['./raven-settings-bands.component.css'],
  templateUrl: './raven-settings-bands.component.html',
})
export class RavenSettingsBandsComponent {
  @Input()
  bandsById: StringTMap<RavenCompositeBand>;

  @Input()
  selectedBandId: string;

  @Input()
  selectedSubBandId: string;

  @Output()
  deleteSubBand: EventEmitter<RavenSubBand> = new EventEmitter<RavenSubBand>();

  @Output()
  updateBand: EventEmitter<RavenUpdate> = new EventEmitter<RavenUpdate>();

  @Output()
  updateBandAndSubBand: EventEmitter<RavenUpdate> = new EventEmitter<
    RavenUpdate
  >();

  @Output()
  updateSubBand: EventEmitter<RavenUpdate> = new EventEmitter<RavenUpdate>();

  @Output()
  updateTimeline: EventEmitter<RavenUpdate> = new EventEmitter<RavenUpdate>();

  /**
   * Returns true if the selected band contains more than one resource sub-band. False otherwise.
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
   * Event. Change callback. Only allow activity label font size between the min/max font size ranges.
   */
  onActivityLabelFontSizeChange(labelFontSize: number) {
    if (labelFontSize > 5 && labelFontSize < 31) {
      this.updateSubBand.emit({
        bandId: this.selectedBandId,
        subBandId: this.selectedSubBandId,
        update: { activityLabelFontSize: labelFontSize },
      });
    }
  }

  /**
   * Change plot type changes the height and heightPadding. Height of CompositeBand needs to include heightPadding for top and bottom tick labels to show in a line plot.
   */
  onChangePlotType(subBand: RavenSubBand, isNumeric: boolean) {
    this.updateBand.emit({
      bandId: this.selectedBandId,
      subBandId: subBand.id,
      update: {
        height: isNumeric ? 100 : 50,
        heightPadding: isNumeric ? 20 : 0,
      },
    });

    this.updateSubBand.emit({
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
   * Change showStateChangeTimes requires change in heightPadding for the times to be shown.
   */
  onChangeShowStateChangeTimes(
    subBand: RavenSubBand,
    showStateChangeTimes: boolean,
  ) {
    this.updateSubBand.emit({
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
  onStateLabelFontSizeChange(labelFontSize: number) {
    if (labelFontSize > 5 && labelFontSize < 31) {
      this.updateSubBand.emit({
        bandId: this.selectedBandId,
        subBandId: this.selectedSubBandId,
        update: { stateLabelFontSize: labelFontSize },
      });
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
