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
import { colorHexToRgbArray, getTooltipText } from '../../util';

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
  isNumeric: boolean;

  @Input()
  label: string;

  @Input()
  labelColor: string;

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
  possibleStates: string[];

  @Input()
  showLabelPin: boolean;

  @Input()
  showStateChangeTimes: boolean;

  @Input()
  stateLabelFontSize: number;

  @Input()
  timeDelta: number;

  @Input()
  type: string;

  // These Inputs are used when isNumeric is true.
  @Input()
  color: string;

  @Input()
  fill: boolean;

  @Input()
  fillColor: string;

  @Input()
  icon: string;

  @Input()
  showIcon: boolean;

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

    // Color.
    if (changes.color && !changes.color.firstChange) {
      this.updateIntervals.emit({
        subBandId: this.id,
        ...this.getLineIntervals(this.color),
      }); // All intervals need to be updated with the new color.
      this.updateSubBand.emit({
        prop: 'color',
        subBandId: this.id,
        subObject: 'painter',
        value: colorHexToRgbArray(this.color),
      });
      this.updateSubBand.emit({
        prop: 'labelColor',
        subBandId: this.id,
        value: colorHexToRgbArray(this.color),
      });
    }

    // Fill.
    if (changes.fill && !changes.fill.firstChange) {
      this.updateSubBand.emit({
        prop: 'fill',
        subBandId: this.id,
        subObject: 'painter',
        value: this.fill,
      });
    }

    // Fill Color.
    if (changes.fillColor && !changes.fillColor.firstChange) {
      this.updateSubBand.emit({
        prop: 'fillColor',
        subBandId: this.id,
        subObject: 'painter',
        value: colorHexToRgbArray(this.fillColor),
      });
    }

    // Icon.
    if (changes.icon && !changes.icon.firstChange) {
      this.updateIntervals.emit({
        subBandId: this.id,
        ...this.getIntervals(this.color),
      });
    }

    // IsNumeric. It is important to check isNumeric prior to height and heightPadding since isNumeric creates a new ctlBand.
    if (changes.isNumeric && !changes.isNumeric.firstChange) {
      // Remove the old state band.
      this.removeSubBand.emit(this.id);
      // Send the newly created state or resource band to the parent composite band so it can be added.
      this.addSubBand.emit(this.createCtlBand());

      if (!this.isNumeric && this.showStateChangeTimes) {
        this.updateSubBand.emit({
          prop: 'showStateChangeTimes',
          subBandId: this.id,
          subObject: 'painter',
          value: this.showStateChangeTimes,
        });
      }
    }

    // Height.
    if (changes.height && !changes.height.firstChange) {
      // Height of state bar drawn needs to exclude heightPadding.
      this.updateSubBand.emit({
        prop: 'height',
        subBandId: this.id,
        value: this.isNumeric ? this.height : this.height - this.heightPadding,
      });
    }

    // Height Padding.
    if (changes.heightPadding && !changes.heightPadding.firstChange) {
      this.updateSubBand.emit({
        prop: 'heightPadding',
        subBandId: this.id,
        value: this.heightPadding,
      });
    }

    // Label.
    if (changes.label && !changes.label.firstChange) {
      this.updateSubBand.emit({
        prop: 'label',
        subBandId: this.id,
        value: this.getLabel(),
      });
    }

    // Label Color.
    if (changes.labelColor && !changes.labelColor.firstChange) {
      this.updateSubBand.emit({
        prop: 'labelColor',
        subBandId: this.id,
        value: colorHexToRgbArray(this.labelColor),
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
      this.updateIntervals.emit({
        subBandId: this.id,
        ...this.getIntervals(this.color),
      });
    }

    // Show Icon.
    if (changes.showIcon && !changes.showIcon.firstChange) {
      this.updateSubBand.emit({
        prop: 'showIcon',
        subBandId: this.id,
        subObject: 'painter',
        value: this.showIcon,
      });
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

    // State Label Font Size.
    if (changes.stateLabelFontSize && !changes.stateLabelFontSize.firstChange) {
      this.updateSubBand.emit({
        prop: 'font',
        subBandId: this.id,
        subObject: 'painter',
        value: `normal ${this.stateLabelFontSize}px Verdana`,
      });
    }

    // timeDelta.
    if (changes.timeDelta && !changes.timeDelta.firstChange) {
      this.updateSubBand.emit({
        prop: 'label',
        subBandId: this.id,
        value:
          this.timeDelta !== 0 ? `[*] ${this.getLabel()}` : this.getLabel(),
      });
    }
  }

  ngOnInit() {
    // Create Band.
    const ctlBand = this.createCtlBand();

    // Send the newly created state or resource band to the parent composite band so it can be added.
    // All subsequent updates should be made to the parent composite sub-band via events.
    this.addSubBand.emit(ctlBand);
  }

  ngOnDestroy() {
    this.removeSubBand.emit(this.id);
  }

  /**
   * Helper. Creates a ResourceBand if isNumeric. Otherwise create a State Band.
   */
  createCtlBand() {
    if (!this.isNumeric) {
      // Create State Band.
      const ctlStateBand = new (window as any).StateBand({
        alignLabel: this.alignLabel,
        autoColor: true,
        baselineLabel: this.baselineLabel,
        borderWidth: this.borderWidth,
        font: `normal ${this.stateLabelFontSize}px Verdana`,
        height: this.height - this.heightPadding,
        heightPadding: this.heightPadding,
        id: this.id,
        intervals: [],
        label:
          this.timeDelta !== 0 ? `[*] ${this.getLabel()}` : this.getLabel(),
        labelColor: colorHexToRgbArray(this.labelColor),
        labelFont: this.labelFont,
        labelFontSize: this.labelFontSize,
        name: this.name,
        timeAxis: this.ctlTimeAxis,
        viewTimeAxis: this.ctlViewTimeAxis,
      });

      if (this.showStateChangeTimes) {
        ctlStateBand.painter.showStateChangeTimes = true;
      }

      // Create Intervals.
      const { intervals, intervalsById } = this.getStateIntervals();

      ctlStateBand.setIntervals(intervals); // This resets interpolation in CTL so we must re-set it on the next line.
      ctlStateBand.intervalsById = intervalsById;
      ctlStateBand.type = 'state';
      return ctlStateBand;
    } else {
      // Create Intervals.
      const { intervals, intervalsById } = this.getLineIntervals(this.color);

      // Create Resource Band.
      const ctlResourceBand = new (window as any).ResourceBand({
        autoScale: (window as any).ResourceBand.VISIBLE_INTERVALS,
        height: this.height,
        heightPadding: 10,
        hideTicks: false,
        icon: this.icon,
        id: this.id,
        interpolation: 'constant',
        intervals,
        label: this.getLabel(),
        labelColor: colorHexToRgbArray(this.color),
        labelFont: this.labelFont,
        labelFontSize: this.labelFontSize,
        name: this.name,
        onFormatTickValue: this.formatTickValue.bind(this),
        onGetInterpolatedTooltipText: null,
        painter: new (window as any).ResourcePainter({
          color: colorHexToRgbArray(this.color),
          fill: this.fill,
          fillColor: colorHexToRgbArray(this.fillColor),
          icon: this.icon,
          showIcon: this.showIcon,
        }),
        timeAxis: this.ctlTimeAxis,
        viewTimeAxis: this.ctlViewTimeAxis,
      });

      ctlResourceBand.intervalsById = intervalsById;
      ctlResourceBand.tickValues = Object.keys(this.getValueToTickMap());
      ctlResourceBand.type = 'state';
      return ctlResourceBand;
    }
  }

  /**
   * Helper. Returns text for value.
   */
  formatTickValue(tickValue: string): string {
    return this.getValueToTickMap()[tickValue];
  }

  /**
   * Helper. Returns state or line intervals.
   */
  getIntervals(color: string) {
    return !this.isNumeric
      ? this.getStateIntervals()
      : this.getLineIntervals(color);
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
   * Helper. Creates CTL intervals for a state band.
   */
  getLineIntervals(color: string) {
    const intervals = [];
    const intervalsById = {};
    const keyValueMap = this.getTickToValueMap();

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];
      if (point.pointStatus !== 'deleted') {
        const interval = new (window as any).DrawableInterval({
          color: colorHexToRgbArray(this.color),
          end: point.start,
          endValue: keyValueMap[point.value],
          icon: this.icon,
          id: point.id,
          onGetTooltipText: this.onGetTooltipText.bind(this),
          opacity: 0.9,
          properties: {
            Value: keyValueMap[point.value],
          },
          start: point.start,
          startValue: keyValueMap[point.value],
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
   * Helper. Creates CTL intervals for a state band.
   */
  getStateIntervals() {
    const intervals = [];
    const intervalsById = {};

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];

      if (point.pointStatus !== 'deleted') {
        const interval = new (window as any).DrawableInterval({
          color: null,
          end: point.end,
          endValue: point.value,
          id: point.id,
          label: point.value,
          labelColor: colorHexToRgbArray('#000000'),
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
    }

    intervals.sort((window as any).DrawableInterval.earlyStartEarlyEnd);

    return {
      intervals,
      intervalsById,
    };
  }

  /**
   * Helper. Returns tick to value map.
   */
  getTickToValueMap() {
    const tickToValueMap = {};
    for (let v = 0, l = this.possibleStates.length; v < l; ++v) {
      const strValue = this.possibleStates[v];
      tickToValueMap[strValue] = v;
    }
    return tickToValueMap;
  }

  /**
   * Helper. Returns value to tick map.
   */
  getValueToTickMap() {
    const valueToTickMap = {};
    for (let v = 0, l = this.possibleStates.length; v < l; ++v) {
      const strValue = this.possibleStates[v];
      valueToTickMap[v] = strValue;
    }
    return valueToTickMap;
  }

  /**
   * CTL Event. Called when we want to get tooltip text.
   */
  onGetTooltipText(e: Event, obj: any) {
    return getTooltipText(
      obj,
      this.earthSecToEpochSec,
      this.epoch,
      this.dayCode,
    );
  }
}
