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

import { RavenTimeRange } from '../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-time-scroll-bar',
  styles: [
    `
      :host {
        display: block;
      }
    `,
  ],
  template: ``,
})
export class RavenTimeScrollBarComponent
  implements AfterViewInit, OnChanges, OnInit {
  @Input()
  labelWidth: number;

  @Input()
  maxTimeRange: RavenTimeRange;

  @Input()
  viewTimeRange: RavenTimeRange;

  @Output()
  updateViewTimeRange: EventEmitter<RavenTimeRange> = new EventEmitter<
    RavenTimeRange
  >();

  ctlTimeAxis = new (window as any).TimeAxis({ end: 0, start: 0 });
  ctlTimeScrollBar: any;
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
    this.ctlTimeScrollBar = new (window as any).TimeScrollBar({
      font: 'normal 9px Verdana',
      height: 15,
      label: '',
      onUpdateView: this.onUpdateView.bind(this),
      timeAxis: this.ctlTimeAxis,
      updateOnDrag: false,
      viewTimeAxis: this.ctlViewTimeAxis,
    });

    this.ctlTimeAxis.updateTimes(
      this.maxTimeRange.start,
      this.maxTimeRange.end,
    );
    this.ctlViewTimeAxis.updateTimes(
      this.viewTimeRange.start,
      this.viewTimeRange.end,
    );

    this.elementRef.nativeElement.appendChild(this.ctlTimeScrollBar.div);
  }

  /**
   * Global Event. Called on window resize.
   */
  @HostListener('window:resize', ['$event'])
  onResize(e: Event): void {
    this.resize();
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

    this.ctlTimeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
    this.ctlViewTimeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
  }

  /**
   * Helper. Call when a time-scroll-bar should be redrawn.
   */
  redraw() {
    this.ctlTimeScrollBar.revalidate();
    this.ctlTimeScrollBar.repaint();
  }

  /**
   * Helper. Call when a time-scroll-bar should be resized.
   * Note that this triggers a redraw.
   */
  resize() {
    this.updateTimeAxisXCoordinates();
    this.redraw();
  }
}
