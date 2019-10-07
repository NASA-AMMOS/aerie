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
import { dateToTimestring, toDuration } from '../../../shared/util/time';
import { RavenEpoch, RavenResourcePoint } from '../../models';
import { colorHexToRgbArray } from '../../util';
import { getInterpolatedTooltipText, getTooltipText } from '../../util/tooltip';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-resource-band',
  styles: [``],
  template: ``,
})
export class RavenResourceBandComponent
  implements OnChanges, OnDestroy, OnInit {
  @Input()
  autoScale: boolean;

  @Input()
  color: string;

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
  fill: boolean;

  @Input()
  fillColor: string;

  @Input()
  font: string;

  @Input()
  height: number;

  @Input()
  heightPadding: number;

  @Input()
  icon: string;

  @Input()
  id: string;

  @Input()
  interpolation: string;

  @Input()
  isDuration: boolean;

  @Input()
  isTime: boolean;

  @Input()
  label: string;

  @Input()
  labelFont: string;

  @Input()
  labelFontSize: number;

  @Input()
  labelPin: string;

  @Input()
  labelUnit: string;

  @Input()
  logTicks: boolean;

  @Input()
  maxLimit: number;

  @Input()
  minLimit: number;

  @Input()
  name: string;

  @Input()
  points: RavenResourcePoint[];

  @Input()
  scientificNotation: boolean;

  @Input()
  showIcon: boolean;

  @Input()
  showLabelPin: boolean;

  @Input()
  showLabelUnit: boolean;

  @Input()
  timeDelta: number;

  @Input()
  type: string;

  @Output()
  addSubBand: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  removeSubBand: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  updateInterpolation: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  updateIntervals: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  updateSubBand: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  updateTickValues: EventEmitter<any> = new EventEmitter<any>();

  ngOnChanges(changes: SimpleChanges) {
    // Auto Scale Tick Values.
    if (changes.autoScale && !changes.autoScale.firstChange) {
      const ctlResourceBand = (window as any).ResourceBand;
      this.updateSubBand.emit({
        prop: 'autoScale',
        subBandId: this.id,
        value: this.autoScale
          ? ctlResourceBand.VISIBLE_INTERVALS
          : ctlResourceBand.ALL_INTERVALS,
      });
      this.updateTickValues.emit();
    }

    // Color.
    if (changes.color && !changes.color.firstChange) {
      this.updateIntervals.emit({
        subBandId: this.id,
        ...this.getIntervals(this.color),
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

    // Font.
    // TODO.

    // Icon.
    if (changes.icon && !changes.icon.firstChange) {
      this.updateIntervals.emit({
        subBandId: this.id,
        ...this.getIntervals(this.color),
      });
    }

    // Interpolation.
    if (changes.interpolation && !changes.interpolation.firstChange) {
      this.updateInterpolation.emit({
        interpolation: this.interpolation,
        subBandId: this.id,
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

    // Label Unit.
    if (changes.labelUnit && !changes.labelUnit.firstChange) {
      this.updateSubBand.emit({
        prop: 'label',
        subBandId: this.id,
        value: this.getLabel(),
      });
    }

    // Log Ticks.
    if (changes.logTicks && !changes.logTicks.firstChange) {
      // Update intervals since 0 and <1 values are removes for log scale.
      this.updateIntervals.emit({
        subBandId: this.id,
        ...this.getIntervals(this.color),
      });
      this.updateSubBand.emit({
        prop: 'logTicks',
        subBandId: this.id,
        value: this.logTicks,
      });
      this.updateTickValues.emit();
    }

    // Points.
    if (changes.points && !changes.points.firstChange) {
      this.updateIntervals.emit({
        subBandId: this.id,
        ...this.getIntervals(this.color),
      });
    }

    // Scientific Notation.
    if (changes.scientificNotation && !changes.scientificNotation.firstChange) {
      this.updateSubBand.emit({
        prop: 'scientificNotation',
        subBandId: this.id,
        value: this.scientificNotation,
      });
      this.updateTickValues.emit();
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

    // Show Label Unit.
    if (changes.showLabelUnit && !changes.showLabelUnit.firstChange) {
      this.updateSubBand.emit({
        prop: 'label',
        subBandId: this.id,
        value: this.getLabel(),
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
    // Create Resource Band.
    const ctlResourceBand = new (window as any).ResourceBand({
      autoScale: (window as any).ResourceBand.VISIBLE_INTERVALS,
      height: this.height,
      heightPadding: this.heightPadding,
      hideTicks: false,
      icon: this.icon,
      id: this.id,
      interpolation: this.interpolation,
      intervals: [],
      label: this.timeDelta !== 0 ? `[*] ${this.getLabel()}` : this.getLabel(),
      labelColor: colorHexToRgbArray(this.color),
      labelFont: this.labelFont,
      labelFontSize: this.labelFontSize,
      logTicks: this.logTicks,
      name: this.name,
      onFormatTickValue: this.onFormatTickValue.bind(this),
      onGetInterpolatedTooltipText: this.onGetInterpolatedTooltipText.bind(
        this,
      ),
      painter: new (window as any).ResourcePainter({
        // no border within paint units
        borderWidth: 0,
        color: colorHexToRgbArray(this.color),
        fill: this.fill,
        fillColor: colorHexToRgbArray(this.fillColor),
        icon: this.icon,
        showIcon: this.showIcon,
      }),
      scientificNotation: this.scientificNotation,
      tickValues: [],
      timeAxis: this.ctlTimeAxis,
      viewTimeAxis: this.ctlViewTimeAxis,
    });

    // Create Intervals.
    const { intervals, intervalsById } = this.getIntervals(this.color);

    ctlResourceBand.setIntervals(intervals);
    ctlResourceBand.intervalsById = intervalsById;
    ctlResourceBand.type = 'resource';
    ctlResourceBand.isDuration = this.isDuration;
    ctlResourceBand.isTime = this.isTime;

    if (this.maxLimit !== undefined) {
      ctlResourceBand.maxLimit = this.maxLimit;
    }
    if (this.minLimit !== undefined) {
      ctlResourceBand.minLimit = this.minLimit;
    }

    // Send the newly created resource band to the parent composite band so it can be added.
    // All subsequent updates should be made to the parent composite sub-band via events.
    this.addSubBand.emit(ctlResourceBand);
  }

  ngOnDestroy() {
    this.removeSubBand.emit(this.id);
  }

  /**
   * Helper. Creates CTL intervals for a resource band.
   */
  getIntervals(color: string) {
    const intervals = [];
    const intervalsById = {};

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];

      if ((!this.logTicks ||  point.value > 0) && point.pointStatus !== 'deleted' && point.start > 0) {
        const interval = new (window as any).DrawableInterval({
          color: colorHexToRgbArray(color),
          end: point.start,
          endValue: point.value,
          icon: this.icon,
          id: point.id,
          onGetTooltipText: this.onGetTooltipText.bind(this),
          opacity: 0.9,
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
   * Helper. Builds a label from the base label, pins, and units.
   */
  getLabel() {
    let labelPin = '';
    let labelUnit = '';

    if (this.showLabelPin && this.labelPin !== '') {
      labelPin = ` (${this.labelPin})`;
    }

    if (this.showLabelUnit && this.labelUnit !== '') {
      labelUnit = ` (${this.labelUnit})`;
    }

    return `${this.label}${labelPin}${labelUnit}`;
  }

  /**
   * CTL Event. Called when we want to format a tick value.
   */
  onFormatTickValue(tick: any, showMilliseconds: boolean) {
    if (this.isDuration) {
      // Format duration resources ticks.
      return toDuration(tick, showMilliseconds);
    } else if (this.isTime) {
      // Format time resources ticks.
      return dateToTimestring(new Date(tick * 1000), showMilliseconds);
    }

    return tick;
  }

  /**
   * CTL Event. Called when we want to get tooltip text for an interpolated interval.
   */
  onGetInterpolatedTooltipText(e: Event, obj: any) {
    return getInterpolatedTooltipText(
      obj,
      this.earthSecToEpochSec,
      this.epoch,
      this.dayCode,
    );
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
