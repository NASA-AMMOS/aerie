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
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';

import {
  RavenTimeRange,
} from './../../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'falcon-time-band',
  styleUrls: ['./falcon-time-band.component.css'],
  templateUrl: './falcon-time-band.component.html',
})

export class FalconTimeBandComponent implements OnChanges, AfterViewInit {
  @Input() labelWidth: number;
  @Input() maxTimeRange: RavenTimeRange;
  @Input() showTooltip: boolean;
  @Input() viewTimeRange: RavenTimeRange;

  @Output() updateViewTimeRange: EventEmitter<RavenTimeRange> = new EventEmitter<RavenTimeRange>();

  ctlTimeAxis = new (window as any).TimeAxis({ end: 0, start: 0 });
  ctlTimeBand: any;
  ctlTooltip = new (window as any).Tooltip({});
  ctlViewTimeAxis = new (window as any).TimeAxis({ end: 0, start: 0 });

  constructor(public elementRef: ElementRef) {
    this.ctlTimeBand = new (window as any).TimeBand({
      font: 'normal 9px Verdana',
      height: 37,
      label: 'UTC',
      minorLabels: [],
      onHideTooltip: this.onHideTooltip.bind(this),
      onShowTooltip: this.onShowTooltip.bind(this),
      onUpdateView: this.onUpdateView.bind(this),
      scrollDelta: 21600,
      timeAxis: this.ctlTimeAxis,
      viewTimeAxis: this.ctlViewTimeAxis,
      zoomDelta: 21600,
    });

    elementRef.nativeElement.appendChild(this.ctlTimeBand.div);
  }

  ngAfterViewInit() {
    this.resize();
  }

  ngOnChanges(changes: SimpleChanges) {
    let shouldResize = false;
    let shouldRedraw = false;

    // Label Width.
    if (changes.labelWidth && !changes.labelWidth.firstChange) {
      shouldResize = true;
    }

    // Max Time Range.
    if (changes.maxTimeRange) {
      const currentMaxTimeRange = changes.maxTimeRange.currentValue;

      if (changes.maxTimeRange.firstChange) {
        this.ctlTimeAxis.updateTimes(currentMaxTimeRange.start, currentMaxTimeRange.end);
      } else {
        const previousMaxTimeRange = changes.maxTimeRange.previousValue;

        // Make sure we don't redraw or update times unless the times actually changed.
        if (previousMaxTimeRange.start !== currentMaxTimeRange.start ||
          previousMaxTimeRange.end !== currentMaxTimeRange.end) {
          this.ctlViewTimeAxis.updateTimes(currentMaxTimeRange.start, currentMaxTimeRange.end);
          shouldRedraw = true;
        }
      }
    }

    // View Time Range.
    if (changes.viewTimeRange) {
      const currentViewTimeRange = changes.viewTimeRange.currentValue;

      if (changes.viewTimeRange.firstChange) {
        this.ctlViewTimeAxis.updateTimes(currentViewTimeRange.start, currentViewTimeRange.end);
      } else {
        const previousViewTimeRange = changes.viewTimeRange.previousValue;

        // Make sure we don't redraw or update times unless the times actually changed.
        if (previousViewTimeRange.start !== currentViewTimeRange.start ||
            previousViewTimeRange.end !== currentViewTimeRange.end) {
          this.ctlViewTimeAxis.updateTimes(currentViewTimeRange.start, currentViewTimeRange.end);
          shouldRedraw = true;
        }
      }
    }

    if (shouldResize) {
      this.resize();
    } else if (shouldRedraw) {
      this.redraw();
    }
  }

  /**
   *
   */
  onHideTooltip() {
    this.ctlTooltip.hide();
  }

  /**
   *
   */
  onShowTooltip(e: any, text: string) {
    if (this.showTooltip) {
      this.ctlTooltip.show(text, e.clientX, e.clientY);
    }
  }

  /**
   *
   */
  onUpdateView(start: number, end: number) {
    if (start !== 0 && end !== 0 && start < end) {
      this.updateViewTimeRange.emit({ end, start });
    }
  }

  /**
   *
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
   *
   */
  redraw() {
    console.log('time-band redraw');
    this.ctlTimeBand.revalidate();
    this.ctlTimeBand.repaint();
  }

  /**
   *
   */
  resize() {
    console.log('time-band resize');
    this.updateTimeAxisXCoordinates();
    this.redraw();
  }
}
