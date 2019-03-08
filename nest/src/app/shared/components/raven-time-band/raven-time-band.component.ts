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
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';

import { RavenEpoch, RavenTimeRange } from '../../models';
import { colorHexToRgbArray } from '../../util/color';
import { formatTimeTickTFormat, getLocalTimezoneName } from '../../util/time';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-time-band',
  styles: [
    `
      :host {
        display: block;
      }
    `,
  ],
  template: ``,
})
export class RavenTimeBandComponent
  implements AfterViewInit, OnChanges, OnInit {
  @Input()
  cursorColor: string;

  @Input()
  cursorTime: number | null;

  @Input()
  cursorWidth: number;

  @Input()
  dayCode: string;

  @Input()
  earthSecToEpochSec: number;

  @Input()
  epoch: RavenEpoch | null;

  @Input()
  guides: number[];

  @Input()
  labelWidth: number;

  @Input()
  maxTimeRange: RavenTimeRange;

  @Input()
  showTooltip: boolean;

  @Input()
  sideMenuDivSize: number;

  @Input()
  viewTimeRange: RavenTimeRange;

  @Output()
  updateViewTimeRange: EventEmitter<RavenTimeRange> = new EventEmitter<
    RavenTimeRange
  >();

  ctlTimeAxis = new (window as any).TimeAxis({ end: 0, start: 0 });
  ctlTimeBand: any;
  ctlTooltip = new (window as any).Tooltip({});
  ctlViewTimeAxis = new (window as any).TimeAxis({ end: 0, start: 0 });

  constructor(public elementRef: ElementRef) {}

  ngAfterViewInit() {
    this.resize();
  }

  ngOnChanges(changes: SimpleChanges) {
    let shouldRedraw = false;
    let shouldResize = false;

    // Day Code.
    if (changes.dayCode && !changes.dayCode.firstChange) {
      shouldRedraw = true;
    }

    // Earth Sec To Epoch Sec.
    if (changes.earthSecToEpochSec && !changes.earthSecToEpochSec.firstChange) {
      shouldRedraw = true;
    }

    // Epoch.
    if (changes.epoch && !changes.epoch.firstChange) {
      this.ctlTimeBand.minorLabels = this.getMinorLabel();
      shouldRedraw = true;
    }

    if (changes.guides && !changes.guides.firstChange) {
      this.ctlTimeAxis.guideTimes = this.guides;
      shouldRedraw = true;
    }

    // Label Width.
    if (changes.labelWidth && !changes.labelWidth.firstChange) {
      shouldResize = true;
    }

    // Max Time Range.
    if (changes.maxTimeRange && !changes.maxTimeRange.firstChange) {
      const currentMaxTimeRange = changes.maxTimeRange.currentValue;
      const previousMaxTimeRange = changes.maxTimeRange.previousValue;

      // Make sure we don't redraw or update times unless the times actually changed.
      if (
        previousMaxTimeRange.start !== currentMaxTimeRange.start ||
        previousMaxTimeRange.end !== currentMaxTimeRange.end
      ) {
        this.ctlTimeAxis.updateTimes(
          currentMaxTimeRange.start,
          currentMaxTimeRange.end,
        );
        shouldRedraw = true;
      }
    }

    // Time Cursor Color.
    if (changes.cursorColor && !changes.cursorColor.firstChange) {
      this.ctlTimeBand.timeCursorColor = colorHexToRgbArray(this.cursorColor);
      shouldRedraw = true;
    }

    // Time Cursor Time.
    if (changes.cursorTime && !changes.cursorTime.firstChange) {
      this.ctlViewTimeAxis.now = changes.cursorTime.currentValue;
      shouldRedraw = true;
    }

    // Time Cursor Width.
    if (changes.cursorWidth && !changes.cursorWidth.firstChange) {
      this.ctlTimeBand.timeCursorWidth = this.cursorWidth;
      shouldRedraw = true;
    }

    // View Time Range.
    if (changes.viewTimeRange && !changes.viewTimeRange.firstChange) {
      const currentViewTimeRange = changes.viewTimeRange.currentValue;
      const previousViewTimeRange = changes.viewTimeRange.previousValue;

      // Make sure we don't redraw or update times unless the times actually changed.
      if (
        previousViewTimeRange.start !== currentViewTimeRange.start ||
        previousViewTimeRange.end !== currentViewTimeRange.end
      ) {
        this.ctlViewTimeAxis.updateTimes(
          currentViewTimeRange.start,
          currentViewTimeRange.end,
        );
        shouldRedraw = true;
      }
    }

    // Only resize OR redraw once to maintain performance.
    if (shouldResize) {
      this.resize();
    } else if (shouldRedraw) {
      this.redraw();
    }
  }

  ngOnInit() {
    this.ctlTimeBand = new (window as any).TimeBand({
      font: 'normal 9px Verdana',
      height: 37,
      // height: 75,
      label: 'SCET',
      minorLabels: this.getMinorLabel(),
      onFormatNow: this.onFormatNow.bind(this),
      onFormatTimeTick: this.onFormatTimeTick.bind(this),
      onHideTooltip: this.onHideTooltip.bind(this),
      onShowTooltip: this.onShowTooltip.bind(this),
      onUpdateView: this.onUpdateView.bind(this),
      scrollDelta: 21600,
      timeAxis: this.ctlTimeAxis,
      viewTimeAxis: this.ctlViewTimeAxis,
      zoomDelta: 21600,
    });
    this.ctlTimeBand.timeCursorWidth = this.cursorWidth;
    this.ctlTimeBand.timeCursorColor = colorHexToRgbArray(this.cursorColor);

    this.ctlTimeAxis.guideTimes = this.guides;

    this.ctlTimeAxis.updateTimes(
      this.maxTimeRange.start,
      this.maxTimeRange.end,
    );
    this.ctlViewTimeAxis.updateTimes(
      this.viewTimeRange.start,
      this.viewTimeRange.end,
    );

    this.elementRef.nativeElement.appendChild(this.ctlTimeBand.div);
    this.elementRef.nativeElement.appendChild(this.ctlTooltip.div);
  }

  /**
   * Global Event. Called on window resize.
   */
  @HostListener('window:resize', ['$event'])
  onResize(e: Event): void {
    this.resize();
  }

  /**
   * CTL Event. Called when tooltip is hidden.
   */
  onHideTooltip() {
    this.ctlTooltip.hide();
  }

  /**
   * CTL Event. Called to get a CTL `formatNow` time tick (for use in the time cursor).
   */
  onFormatNow(obj: any) {
    const formattedTimes = formatTimeTickTFormat(
      obj,
      this.epoch,
      this.earthSecToEpochSec,
      this.dayCode,
    );
    formattedTimes[0].y = 30;

    return [formattedTimes[0]];
  }

  /**
   * CTL Event. Called to get custom time band ticks.
   */
  onFormatTimeTick(obj: any) {
    return formatTimeTickTFormat(
      obj,
      this.epoch,
      this.earthSecToEpochSec,
      this.dayCode,
    );
  }

  /**
   * CTL Event. Called when tooltip is shown.
   */
  onShowTooltip(e: MouseEvent, text: string) {
    if (this.showTooltip) {
      this.ctlTooltip.show(text, e.clientX, e.clientY);
    }
  }

  /**
   * CTL Event. Called when the view is updated.
   */
  onUpdateView(start: number, end: number) {
    if (start !== 0 && end !== 0 && start < end) {
      this.updateViewTimeRange.emit({ end, start });
    }
  }

  /**
   * Helper. Recalculates x-coordinates of the band based on the label width.
   */
  updateTimeAxisXCoordinates() {
    const offsetWidth = this.elementRef.nativeElement.offsetWidth;
    console.log('time band offsetWidth:' + offsetWidth);
    this.ctlTimeAxis.updateXCoordinates(
      this.labelWidth,
      offsetWidth + this.sideMenuDivSize,
    );
    this.ctlViewTimeAxis.updateXCoordinates(
      this.labelWidth,
      offsetWidth + this.sideMenuDivSize,
    );
  }

  /**
   * Helper. Call when a time-band should be redrawn.
   */
  redraw() {
    this.ctlTimeBand.revalidate();
    this.ctlTimeBand.repaint();
  }

  /**
   * Helper. Call when a time-band should be resized.
   * Note that this triggers a redraw.
   */
  resize() {
    this.updateTimeAxisXCoordinates();
    this.redraw();
  }

  /**
   * Helper that returns an epoch label based on an epoch if it exists.
   */
  getEpochLabel(epoch: RavenEpoch | null): string[] {
    if (epoch !== null) {
      return [epoch.name];
    }
    return [];
  }

  /**
   * Helper that returns an epoch name or local time name.
   */
  getMinorLabel() {
    return this.getEpochLabel(this.epoch).length > 0
      ? this.getEpochLabel(this.epoch)
      : [getLocalTimezoneName()];
  }
}
