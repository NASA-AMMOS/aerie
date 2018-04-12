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
  RavenStatePoint,
} from './../../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'falcon-state-band',
  styleUrls: ['./falcon-state-band.component.css'],
  templateUrl: './falcon-state-band.component.html',
})
export class FalconStateBandComponent implements OnChanges, OnDestroy, OnInit {
  @Input() alignLabel: number;
  @Input() baselineLabel: number;
  @Input() borderWidth: number;
  @Input() ctlTimeAxis: any;
  @Input() ctlViewTimeAxis: any;
  @Input() height: number;
  @Input() heightPadding: number;
  @Input() id: string;
  @Input() label: string;
  @Input() labelColor: number[];
  @Input() minorLabels: string[];
  @Input() name: string;
  @Input() points: RavenStatePoint[];
  @Input() showTooltip: boolean;
  @Input() type: string;

  @Output() addSubBand: EventEmitter<any> = new EventEmitter<any>();
  @Output() removeSubBand: EventEmitter<string> = new EventEmitter<string>();
  @Output() updateIntervals: EventEmitter<any> = new EventEmitter<any>();
  @Output() updateSubBand: EventEmitter<any> = new EventEmitter<any>();

  ngOnChanges(changes: SimpleChanges) {
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

    // Label Color.
    if (changes.labelColor && !changes.labelColor.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, prop: 'labelColor', value: this.labelColor });
    }

    // Points.
    if (changes.points && !changes.points.firstChange) {
      this.updateIntervals.emit({ type: this.type, subBandId: this.id, ...this.getIntervals() });
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
      label: this.label,
      labelColor: this.labelColor,
      minorLabels: this.minorLabels,
      name: this.name,
      timeAxis: this.ctlTimeAxis,
      viewTimeAxis: this.ctlViewTimeAxis,
    });

    // Create Intervals.
    const { intervals, intervalsById } = this.getIntervals();

    ctlStateBand.setIntervals(intervals); // This resets interpolation in CTL so we must re-set it on the next line.
    ctlStateBand.intervalsById = intervalsById;

    // Send the newly created state band to the parent composite band so it can be added.
    // All subsequent updates should be made to the parent composite sub-band via events.
    this.addSubBand.emit(ctlStateBand);
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
}
