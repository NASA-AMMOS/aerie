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
  RavenResourcePoint,
} from './../../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-resource-band',
  styleUrls: ['./raven-resource-band.component.css'],
  templateUrl: './raven-resource-band.component.html',
})
export class RavenResourceBandComponent implements OnChanges, OnDestroy, OnInit {
  @Input() autoTickValues: boolean;
  @Input() ctlTimeAxis: any;
  @Input() ctlViewTimeAxis: any;
  @Input() color: number[];
  @Input() fill: boolean;
  @Input() fillColor: number[];
  @Input() font: string;
  @Input() icon: string;
  @Input() labelFont: string;
  @Input() labelFontSize: number;
  @Input() height: number;
  @Input() heightPadding: number;
  @Input() id: string;
  @Input() interpolation: string;
  @Input() label: string;
  @Input() minorLabels: string[];
  @Input() name: string;
  @Input() points: RavenResourcePoint[];
  @Input() rescale: boolean;
  @Input() showIcon: boolean;
  @Input() showTooltip: boolean;
  @Input() type: string;

  @Output() addSubBand: EventEmitter<any> = new EventEmitter<any>();
  @Output() removeSubBand: EventEmitter<string> = new EventEmitter<string>();
  @Output() updateInterpolation: EventEmitter<any> = new EventEmitter<any>();
  @Output() updateIntervals: EventEmitter<any> = new EventEmitter<any>();
  @Output() updateSubBand: EventEmitter<any> = new EventEmitter<any>();

  ngOnChanges(changes: SimpleChanges) {
    // Auto Tick Values.
    if (changes.autoTickValues && !changes.autoTickValues.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, prop: 'autoTickValues', value: this.autoTickValues });
    }

    // Color.
    // TODO.

    // Fill.
    if (changes.fill && !changes.fill.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'fill', value: this.fill });
    }

    // Fill Color.
    if (changes.fillColor && !changes.fillColor.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'fillColor', value: this.fillColor });
    }

    // Font.
    // TODO.

    // Interpolation.
    if (changes.interpolation && !changes.interpolation.firstChange) {
      this.updateInterpolation.emit({ subBandId: this.id, interpolation: this.interpolation });
    }

    // Label.
    if (changes.label && !changes.label.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, prop: 'label', value: this.label });
    }

    // Points.
    if (changes.points && !changes.points.firstChange) {
      this.updateIntervals.emit({ subBandId: this.id, ...this.getIntervals() });
    }

    // Show Icon.
    if (changes.showIcon && !changes.showIcon.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'showIcon', value: this.showIcon });
    }
  }

  ngOnInit() {
    // Create Resource Band.
    const ctlResourceBand = new (window as any).ResourceBand({
      autoScale: (window as any).ResourceBand.VISIBLE_INTERVALS,
      autoTickValues: this.autoTickValues,
      height: this.height,
      heightPadding: this.heightPadding,
      hideTicks: false,
      icon: this.icon,
      id: this.id,
      interpolation: this.interpolation,
      intervals: [],
      label: this.label,
      labelColor: this.color,
      labelFont: this.labelFont,
      labelFontSize: this.labelFontSize,
      minorLabels: this.minorLabels,
      name: this.name,
      painter: new (window as any).ResourcePainter({
        color: this.color,
        fill: this.fill,
        fillColor: this.fillColor,
        icon: this.icon,
        showIcon: this.showIcon,
      }),
      rescale: this.rescale,
      tickValues: [],
      timeAxis: this.ctlTimeAxis,
      viewTimeAxis: this.ctlViewTimeAxis,
    });

    // Create Intervals.
    const { intervals, intervalsById } = this.getIntervals();

    ctlResourceBand.setIntervals(intervals);
    ctlResourceBand.intervalsById = intervalsById;
    ctlResourceBand.type = 'resource';

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
  getIntervals() {
    const intervals = [];
    const intervalsById = {};

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];

      const interval = new (window as any).DrawableInterval({
        color: this.color,
        end: point.start,
        endValue: point.value,
        icon: this.icon,
        id: point.id,
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

    intervals.sort((window as any).DrawableInterval.earlyStartEarlyEnd);

    return {
      intervals,
      intervalsById,
    };
  }
}
