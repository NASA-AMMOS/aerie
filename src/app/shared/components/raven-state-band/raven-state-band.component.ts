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

import { RavenEpoch, RavenStatePoint } from '../../models';
import { getTooltipText } from '../../util';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-state-band',
  styles: [``],
  template: ``,
})
export class RavenStateBandComponent implements OnChanges, OnDestroy, OnInit {
  @Input()
  alignLabel: number;

  @Input()
  baselineLabel: number;

  @Input()
  borderWidth: number;

  @Input()
  ctlTimeAxis: any;

  @Input()
  ctlViewTimeAxis: any;

  @Input()
  dayCode: string;

  @Input()
  earthSecToEpochSec: number;

  @Input()
  epoch: RavenEpoch | null;

  @Input()
  height: number;

  @Input()
  heightPadding: number;

  @Input()
  id: string;

  @Input()
  label: string;

  @Input()
  labelColor: number[];

  @Input()
  labelFont: string;

  @Input()
  labelFontSize: number;

  @Input()
  labelPin: string;

  @Input()
  name: string;

  @Input()
  points: RavenStatePoint[];

  @Input()
  showLabelPin: boolean;

  @Input()
  showStateChangeTimes: boolean;

  @Input()
  type: string;

  @Output()
  addSubBand: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  removeSubBand: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  updateIntervals: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  updateSubBand: EventEmitter<any> = new EventEmitter<any>();

  ngOnChanges(changes: SimpleChanges) {
    // Align Label.
    if (changes.alignLabel && !changes.alignLabel.firstChange) {
      this.updateSubBand.emit({
        prop: 'alignLabel',
        subBandId: this.id,
        subObject: 'painter',
        value: this.alignLabel,
      });
    }

    // Baseline Label.
    if (changes.baselineLabel && !changes.baselineLabel.firstChange) {
      this.updateSubBand.emit({
        prop: 'baselineLabel',
        subBandId: this.id,
        subObject: 'painter',
        value: this.baselineLabel,
      });
    }

    // Border Width.
    if (changes.borderWidth && !changes.borderWidth.firstChange) {
      this.updateSubBand.emit({
        prop: 'borderWidth',
        subBandId: this.id,
        subObject: 'painter',
        value: this.borderWidth,
      });
    }

    // Label.
    if (changes.label && !changes.label.firstChange) {
      this.updateSubBand.emit({
        prop: 'label',
        subBandId: this.id,
        value: this.label,
      });
    }

    // Label Color.
    if (changes.labelColor && !changes.labelColor.firstChange) {
      this.updateSubBand.emit({
        prop: 'labelColor',
        subBandId: this.id,
        value: this.labelColor,
      });
    }

    // Label Font Size.
    if (changes.labelFontSize && !changes.labelFontSize.firstChange) {
      this.updateSubBand.emit({
        prop: 'labelFontSize',
        subBandId: this.id,
        subObject: 'decorator',
        value: this.labelFontSize,
      });
    }

    // Label Pin.
    if (changes.labelPin && !changes.labelPin.firstChange) {
      this.updateSubBand.emit({
        prop: 'label',
        subBandId: this.id,
        value: this.getLabel(),
      });
    }

    // Points.
    if (changes.points && !changes.points.firstChange) {
      this.updateIntervals.emit({ subBandId: this.id, ...this.getIntervals() });
    }

    // Show Label Pin.
    if (changes.showLabelPin && !changes.showLabelPin.firstChange) {
      this.updateSubBand.emit({
        prop: 'label',
        subBandId: this.id,
        value: this.getLabel(),
      });
    }

    // Show State Change Times.
    if (
      changes.showStateChangeTimes &&
      !changes.showStateChangeTimes.firstChange
    ) {
      this.updateSubBand.emit({
        prop: 'showStateChangeTimes',
        subBandId: this.id,
        subObject: 'painter',
        value: this.showStateChangeTimes,
      });
      this.updateSubBand.emit({
        prop: 'heightPadding',
        subBandId: this.id,
        value: this.showStateChangeTimes ? 12 : 0,
      });
      this.updateSubBand.emit({
        prop: 'height',
        subBandId: this.id,
        value: this.showStateChangeTimes ? this.height - 12 : this.height,
      });
    }
  }

  ngOnInit() {
    // Create State Band.
    const ctlStateBand = new (window as any).StateBand({
      alignLabel: this.alignLabel,
      autoColor: true,
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
      name: this.name,
      timeAxis: this.ctlTimeAxis,
      viewTimeAxis: this.ctlViewTimeAxis,
    });

    // Create Intervals.
    const { intervals, intervalsById } = this.getIntervals();

    ctlStateBand.setIntervals(intervals); // This resets interpolation in CTL so we must re-set it on the next line.
    ctlStateBand.intervalsById = intervalsById;
    ctlStateBand.type = 'state';

    // Send the newly created state band to the parent composite band so it can be added.
    // All subsequent updates should be made to the parent composite sub-band via events.
    this.addSubBand.emit(ctlStateBand);

    // Show State Change Times.
    // Note: We need this update here after the band is created because CTL is quirky.
    // Adding these properties in the `new StateBand` above does not properly initialize these properties for drawing.
    // TODO: Look into how we can remove these emits.
    if (this.showStateChangeTimes) {
      this.updateSubBand.emit({
        prop: 'showStateChangeTimes',
        subBandId: this.id,
        subObject: 'painter',
        value: true,
      });
      this.updateSubBand.emit({
        prop: 'heightPadding',
        subBandId: this.id,
        value: 12,
      });
      this.updateSubBand.emit({
        prop: 'height',
        subBandId: this.id,
        value: this.height - 12,
      });
    }
  }

  ngOnDestroy() {
    this.removeSubBand.emit(this.id);
  }

  /**
   * Helper. Creates CTL intervals for a state band.
   */
  getIntervals() {
    const intervals = [];
    const intervalsById = {};

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];

      const interval = new (window as any).DrawableInterval({
        end: point.end,
        endValue: point.value,
        id: point.id,
        label: point.value,
        onGetTooltipText: this.onGetTooltipText.bind(this),
        opacity: 0.5,
        properties: {
          Value: point.value,
        },
        start: point.start,
        startValue: point.value,
      });

      // Set the sub-band ID and unique ID separately since they are not a DrawableInterval prop.
      interval.subBandId = this.id;
      interval.uniqueId = point.uniqueId;

      intervals.push(interval);
      intervalsById[interval.uniqueId] = interval;
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
    return getTooltipText(
      obj,
      this.earthSecToEpochSec,
      this.epoch,
      this.dayCode
    );
  }
}
