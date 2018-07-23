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
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';

import {
  RavenActivityPoint,
  RavenEpoch,
} from './../../../models';

import {
  getTooltipText,
} from './../../../util';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-activity-band',
  styleUrls: ['./raven-activity-band.component.css'],
  templateUrl: './raven-activity-band.component.html',
})
export class RavenActivityBandComponent implements OnChanges, OnDestroy, OnInit {
  @Input() activityHeight: number;
  @Input() activityStyle: number;
  @Input() alignLabel: number;
  @Input() baselineLabel: number;
  @Input() borderWidth: number;
  @Input() ctlTimeAxis: any;
  @Input() ctlViewTimeAxis: any;
  @Input() dayCode: string;
  @Input() earthSecToEpochSec: number;
  @Input() epoch: RavenEpoch | null;
  @Input() height: number;
  @Input() heightPadding: number;
  @Input() icon: string;
  @Input() id: string;
  @Input() label: string;
  @Input() labelColor: number[];
  @Input() labelFont: string;
  @Input() labelFontSize: number;
  @Input() labelPin: string;
  @Input() layout: number;
  @Input() minorLabels: string[];
  @Input() name: string;
  @Input() points: RavenActivityPoint[];
  @Input() showActivityTimes: boolean;
  @Input() showLabel: boolean;
  @Input() showLabelPin: boolean;
  @Input() type: string;

  @Output() addSubBand: EventEmitter<any> = new EventEmitter<any>();
  @Output() removeSubBand: EventEmitter<string> = new EventEmitter<string>();
  @Output() updateIntervals: EventEmitter<any> = new EventEmitter<any>();
  @Output() updateSubBand: EventEmitter<any> = new EventEmitter<any>();

  ngOnChanges(changes: SimpleChanges) {
    // Activity Style.
    if (changes.activityStyle && !changes.activityStyle.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'style', value: this.activityStyle });
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'showIcon', value: this.activityStyle === 3 ? true : false });
      this.updateIntervals.emit({ subBandId: this.id, ...this.getIntervals() });
    }

    // Icon.
    if (changes.icon && !changes.icon.firstChange) {
      this.updateIntervals.emit({ subBandId: this.id, ...this.getIntervals() });
    }

    // Align Label.
    if (changes.alignLabel && !changes.alignLabel.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'alignLabel', value: this.alignLabel });
    }

    // Baseline Label.
    if (changes.baselineLabel && !changes.baselineLabel.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'baselineLabel', value: this.baselineLabel });
    }

    // Border Width.
    if (changes.borderWidth && !changes.borderWidth.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'borderWidth', value: this.borderWidth });
    }

    // Label.
    if (changes.label && !changes.label.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, prop: 'label', value: this.label });
    }

    // Label Font Size.
    if (changes.labelFontSize && !changes.labelFontSize.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'decorator', prop: 'labelFontSize', value: this.labelFontSize });
    }

    // Layout.
    // Note: layout === 0 is autoFit. We autoFit is set then we need to set the layout to compact by default.
    if (changes.layout && !changes.layout.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'autoFit', value: this.layout === 0 ? true : null });
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'layout', value: this.layout === 0 ? 1 : this.layout });

      // Need to reset activityHeight when going from Waterfall to other layouts.
      if (changes.layout.previousValue === 2) {
        this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'activityHeight', value: 20 });
      }
    }

    // Label Color.
    if (changes.labelColor && !changes.labelColor.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, prop: 'labelColor', value: this.labelColor });
    }

    // Label Pin.
    if (changes.labelPin && !changes.labelPin.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, prop: 'label', value: this.getLabel() });
    }

    // Minor labels.
    if (changes.minorLabels && !changes.minorLabels.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, prop: 'minorLabels', value: this.minorLabels });
    }

    // Points.
    if (changes.points && !changes.points.firstChange) {
      this.updateIntervals.emit({ subBandId: this.id, ...this.getIntervals() });
    }

    // Show Activity times.
    if (changes.showActivityTimes && !changes.showActivityTimes.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'showActivityTimes', value: this.showActivityTimes });
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'rowPadding', value: this.showActivityTimes ? 15 : 2 });
    }

    // Show Label.
    if (changes.showLabel && !changes.showLabel.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'showLabel', value: this.showLabel });
    }

    // Show Label Pin.
    if (changes.showLabelPin && !changes.showLabelPin.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, prop: 'label', value: this.getLabel() });
    }
  }

  ngOnInit() {
    // Create Activity Band.
    const ctlActivityBand = new (window as any).ActivityBand({
      activityHeight: this.activityHeight,
      alignLabel: this.alignLabel,
      autoFit: this.layout === 0 ? 1 : null,
      baselineLabel: this.baselineLabel,
      borderWidth: this.borderWidth,
      height: this.height,
      heightPadding: this.heightPadding,
      id: this.id,
      intervals: [],
      label: this.getLabel(),
      labelColor: this.labelColor,
      labelFont: this.labelFont,
      labelFontSize: this.labelFontSize,
      layout: this.layout === 0 ? 1 : this.layout,
      minorLabels: this.minorLabels,
      name: this.name,
      showActivityTimes: this.showActivityTimes,
      showIcon: this.activityStyle === 3 ? true : false,
      showLabel: this.showLabel,
      style: this.activityStyle,
      timeAxis: this.ctlTimeAxis,
      trimLabel: false,
      viewTimeAxis: this.ctlViewTimeAxis,
    });

    // Create Intervals.
    const { intervals, intervalsById } = this.getIntervals();

    ctlActivityBand.setIntervals(intervals); // This resets interpolation in CTL so we must re-set it on the next line.
    ctlActivityBand.intervalsById = intervalsById;
    ctlActivityBand.type = 'activity';

    // Send the newly created activity band to the parent composite band so it can be added.
    // All subsequent updates should be made to the parent composite sub-band via events.
    this.addSubBand.emit(ctlActivityBand);

    // Show Activity times.
    if (this.showActivityTimes) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'showActivityTimes', value: this.showActivityTimes });
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'rowPadding', value: this.showActivityTimes ? 15 : 2 });
    }
  }

  ngOnDestroy() {
    this.removeSubBand.emit(this.id);
  }

  /**
   * Helper. Creates CTL intervals for a activity band.
   * Notice how we are looping through the points only once here for max performance.
   */
  getIntervals() {
    const intervals = [];
    const intervalsById = {};

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];

      // If we have not seen the unique activity id before then add it to be drawn.
      if (!intervalsById[point.uniqueId]) {
        const interval = new (window as any).DrawableInterval({
          color: point.color,
          end: point.end,
          icon: this.activityStyle === 3 ? this.icon : null,
          id: point.id,
          label: point.activityName,
          onGetTooltipText: this.onGetTooltipText.bind(this),
          opacity: 0.5,
          properties: point.message ? { message: point.message } : {},
          start: point.start,
        });

        // Set the sub-band ID and unique ID separately since they are not a DrawableInterval prop.
        interval.subBandId = this.id;
        interval.uniqueId = point.uniqueId;

        intervals.push(interval);
        intervalsById[interval.uniqueId] = interval;
      }
    }

    intervals.sort((window as any).DrawableInterval.earlyStartEarlyEnd);

    return {
      intervals,
      intervalsById,
    };
  }

  /**
   * Helper. Builds a label from the base label and pins.
   */
  getLabel() {
    let labelPin = '';

    if (this.showLabelPin && this.labelPin !== '') {
      labelPin = ` (${this.labelPin})`;
    }

    return `${this.label}${labelPin}`;
  }

  /**
   * CTL Event. Called when we want to get tooltip text.
   */
  onGetTooltipText(e: Event, obj: any) {
    return getTooltipText(obj, this.earthSecToEpochSec, this.epoch, this.dayCode);
  }
}
