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

import {
  RavenTimeRange,
} from './../../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-time-band',
  styleUrls: ['./raven-time-band.component.css'],
  templateUrl: './raven-time-band.component.html',
})
export class RavenTimeBandComponent implements AfterViewInit, OnChanges, OnInit {
  @Input() labelWidth: number;
  @Input() maxTimeRange: RavenTimeRange;
  @Input() showTooltip: boolean;
  @Input() viewTimeRange: RavenTimeRange;

  @Output() updateViewTimeRange: EventEmitter<RavenTimeRange> = new EventEmitter<RavenTimeRange>();

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

    // Label Width.
    if (changes.labelWidth && !changes.labelWidth.firstChange) {
      shouldResize = true;
    }

    // Max Time Range.
    if (changes.maxTimeRange && !changes.maxTimeRange.firstChange) {
      const currentMaxTimeRange = changes.maxTimeRange.currentValue;
      const previousMaxTimeRange = changes.maxTimeRange.previousValue;

      // Make sure we don't redraw or update times unless the times actually changed.
      if (previousMaxTimeRange.start !== currentMaxTimeRange.start ||
          previousMaxTimeRange.end !== currentMaxTimeRange.end) {
        this.ctlTimeAxis.updateTimes(currentMaxTimeRange.start, currentMaxTimeRange.end);
        shouldRedraw = true;
      }
    }

    // View Time Range.
    if (changes.viewTimeRange && !changes.viewTimeRange.firstChange) {
      const currentViewTimeRange = changes.viewTimeRange.currentValue;
      const previousViewTimeRange = changes.viewTimeRange.previousValue;

      // Make sure we don't redraw or update times unless the times actually changed.
      if (previousViewTimeRange.start !== currentViewTimeRange.start ||
          previousViewTimeRange.end !== currentViewTimeRange.end) {
        this.ctlViewTimeAxis.updateTimes(currentViewTimeRange.start, currentViewTimeRange.end);
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
      label: 'UTC',
      onHideTooltip: this.onHideTooltip.bind(this),
      onShowTooltip: this.onShowTooltip.bind(this),
      onUpdateView: this.onUpdateView.bind(this),
      scrollDelta: 21600,
      timeAxis: this.ctlTimeAxis,
      viewTimeAxis: this.ctlViewTimeAxis,
      zoomDelta: 21600,
    });

    this.ctlTimeBand.div.appendChild(this.ctlTooltip.div);

    this.ctlTimeAxis.updateTimes(this.maxTimeRange.start, this.maxTimeRange.end);
    this.ctlViewTimeAxis.updateTimes(this.viewTimeRange.start, this.viewTimeRange.end);

    this.elementRef.nativeElement.appendChild(this.ctlTimeBand.div);
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
    const container = document.querySelector('.timeline-0');
    let offsetWidth = 0;

    if (container && container.parentElement) {
      offsetWidth = container.parentElement.offsetWidth;
    }

    this.ctlTimeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
    this.ctlViewTimeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
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
}
