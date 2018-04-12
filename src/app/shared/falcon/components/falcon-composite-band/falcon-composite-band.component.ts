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
  RavenBandLeftClick,
  RavenPoint,
  RavenSubBand,
  RavenTimeRange,
} from './../../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'falcon-composite-band',
  styleUrls: ['./falcon-composite-band.component.css'],
  templateUrl: './falcon-composite-band.component.html',
})
export class FalconCompositeBandComponent implements AfterViewInit, OnChanges, OnInit {
  @Input() height: number;
  @Input() heightPadding: number;
  @Input() id: string;
  @Input() labelWidth: number;
  @Input() maxTimeRange: RavenTimeRange;
  @Input() selectedPoint: RavenPoint;
  @Input() showTooltip: boolean;
  @Input() subBands: RavenSubBand[];
  @Input() viewTimeRange: RavenTimeRange;

  @Output() bandLeftClick: EventEmitter<RavenBandLeftClick> = new EventEmitter<RavenBandLeftClick>();
  @Output() updateViewTimeRange: EventEmitter<RavenTimeRange> = new EventEmitter<RavenTimeRange>();

  ctlCompositeBand: any;
  ctlTimeAxis = new (window as any).TimeAxis({ end: 0, start: 0 });
  ctlViewTimeAxis = new (window as any).TimeAxis({ end: 0, start: 0 });
  selectedPointColor = [255, 254, 13];

  constructor(public elementRef: ElementRef) {}

  ngAfterViewInit() {
    this.resize();
  }

  ngOnChanges(changes: SimpleChanges) {
    let shouldRedraw = false;
    let shouldResize = false;
    let shouldUpdateTicks = false;

    // Height.
    if (changes.height && !changes.height.firstChange) {
      this.ctlCompositeBand.height = this.height;

      for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
        this.ctlCompositeBand.bands[i].height = this.height;
      }

      shouldRedraw = true;
      shouldUpdateTicks = true;
    }

    // Height Padding.
    if (changes.heightPadding && !changes.heightPadding.firstChange) {
      this.ctlCompositeBand.heightPadding = this.heightPadding;

      for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
        this.ctlCompositeBand.bands[i].heightPadding = this.heightPadding;
      }

      shouldRedraw = true;
      shouldUpdateTicks = true;
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
      if (previousMaxTimeRange.start !== currentMaxTimeRange.start ||
          previousMaxTimeRange.end !== currentMaxTimeRange.end) {
        this.ctlViewTimeAxis.updateTimes(currentMaxTimeRange.start, currentMaxTimeRange.end);
        shouldRedraw = true;
      }
    }

    // Selected Point.
    if (changes.selectedPoint && !changes.selectedPoint.firstChange) {
      const newSelectedPoint = changes.selectedPoint.currentValue;
      const oldSelectedPoint = changes.selectedPoint.previousValue;

      for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
        const subBand = this.ctlCompositeBand.bands[i];

        if (newSelectedPoint && oldSelectedPoint && subBand.intervalsById) {
          const newInterval = subBand.intervalsById[newSelectedPoint.uniqueId];
          const oldInterval = subBand.intervalsById[oldSelectedPoint.uniqueId];

          if (newInterval) {
            // Set the new interval to the highlight color.
            newInterval.originalColor = newInterval.color;
            newInterval.color = this.selectedPointColor;
            shouldRedraw = true;
          }

          if (oldInterval) {
            // Reset the old interval to it's original color.
            oldInterval.color = oldInterval.originalColor;
            shouldRedraw = true;
          }
        } else if (!newSelectedPoint && oldSelectedPoint && subBand.intervalsById) {
          const interval = subBand.intervalsById[oldSelectedPoint.uniqueId];

          if (interval) {
            // Set the interval to it's original color.
            interval.color = interval.originalColor;
            shouldRedraw = true;
          }
        } else if (newSelectedPoint && !oldSelectedPoint && subBand.intervalsById) {
          const interval = subBand.intervalsById[newSelectedPoint.uniqueId];

          if (interval) {
            // Set the interval to the highlight color.
            interval.originalColor = interval.color;
            interval.color = this.selectedPointColor;
            shouldRedraw = true;
          }
        }
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
        shouldUpdateTicks = true;
      }
    }

    // Only update ticks for resource bands once to maintain performance.
    if (shouldUpdateTicks) {
      this.updateTickValues();
    }

    // Only resize OR redraw once to maintain performance.
    if (shouldResize) {
      this.resize();
    } else if (shouldRedraw) {
      this.redraw();
    }
  }

  ngOnInit() {
    this.ctlCompositeBand = new (window as any).CompositeBand({
      height: this.height,
      heightPadding: this.heightPadding,
      id: this.id,
      onDblLeftClick: this.onDblLeftClick.bind(this),
      onHideTooltip: this.onHideTooltip.bind(this),
      onLeftClick: this.onLeftClick.bind(this),
      onRightClick: this.onRightClick.bind(this),
      onShowTooltip: this.onShowTooltip.bind(this),
      onUpdateView: this.onUpdateView.bind(this),
      timeAxis: this.ctlTimeAxis,
      viewTimeAxis: this.ctlViewTimeAxis,
    });

    this.ctlTimeAxis.updateTimes(this.maxTimeRange.start, this.maxTimeRange.end);
    this.ctlViewTimeAxis.updateTimes(this.viewTimeRange.start, this.viewTimeRange.end);

    this.elementRef.nativeElement.appendChild(this.ctlCompositeBand.div);
  }

  /**
   * trackBy for bands list.
   */
  subBandsTrackByFn(index: number, item: RavenSubBand) {
    return item.id;
  }

  /**
   * Global Event. Called on window resize.
   */
  @HostListener('window:resize', ['$event'])
  onResize(e: Event): void {
    this.resize();
  }

  /**
   * CTL Event. Called when you double-left-click a composite band.
   */
  onDblLeftClick(e: MouseEvent) {
    // TODO.
  }

  /**
   * CTL Event. Called when a tooltip is hidden.
   */
  onHideTooltip() {
    // TODO.
  }

  /**
   * CTL Event. Called when you left-click a composite band.
   */
  onLeftClick(e: MouseEvent, ctlData: any) {
    if (ctlData.interval) {
      this.bandLeftClick.emit({ bandId: ctlData.band.id, subBandId: ctlData.interval.subBandId, pointId: ctlData.interval.uniqueId });
    } else {
      this.bandLeftClick.emit({ bandId: ctlData.band.id, subBandId: null, pointId: null });
    }
  }

  /**
   * CTL Event. Called when you right-click a composite band.
   */
  onRightClick(e: MouseEvent) {
    // TODO.
  }

  /**
   * CTL Event. Called when tooltip is shown.
   */
  onShowTooltip(e: MouseEvent, text: string) {
    // TODO.
  }

  /**
   * CTL Event. Called when the view is updated.
   */
  onUpdateView(start: number, end: number) {
    if (this.viewTimeRange.end !== end || this.viewTimeRange.start !== start &&
        start !== 0 && end !== 0 && start < end) {
      this.updateViewTimeRange.emit({ end, start });
    }
  }

  /**
   * Event. Called when a sub-band is added.
   */
  onAddSubBand(subBand: any) {
    this.ctlCompositeBand.addBand(subBand);
    this.updateTickValues();

    // Only redraw if we have more than one sub-band since the first
    // added sub-band will have already been drawn upon component creation.
    if (this.ctlCompositeBand.bands.length > 1) {
      this.redraw();
    }
  }

  /**
   * Event. Called when a sub-band is removed.
   */
  onRemoveSubBand(subBandId: string) {
    this.ctlCompositeBand.removeBand(subBandId);

    // Only redraw if there are any sub-bands left.
    // The band should be destroyed when all sub-bands are removed.
    if (this.ctlCompositeBand.bands.length) {
      this.redraw();
    }
  }

  /**
   * Event. Called for resource bands when we need to update interpolation.
   * We need this as a separate event because of the `setInterpolation` call.
   */
  onUpdateInterpolation(update: any) {
    const { subBandId, interpolation } = update;

    for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
      const subBand = this.ctlCompositeBand.bands[i];

      if (subBand.id === subBandId) {
        subBand.interpolation = interpolation;
        subBand.setInterpolation(interpolation);
        this.redraw();
        return;
      }
    }
  }

  /**
   * Event. Called when a (non-resource) sub-band emits intervals.
   */
  onUpdateIntervals(update: any) {
    const { subBandId, intervals, intervalsById } = update;

    for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
      const subBand = this.ctlCompositeBand.bands[i];

      if (subBand.id === subBandId) {
        subBand.setIntervals(intervals);
        subBand.intervalsById = intervalsById;

        if (subBand.type === 'resource') {
          // Note: setIntervals resets interpolation for resources in CTL,
          // so we must re-set it on the next line.
          subBand.setInterpolation(subBand.interpolation);
        }

        this.redraw();
        return;
      }
    }
  }

  /**
   * Event. Called when we need to update a sub-band.
   * The update object has the option of specifying a subObject to update in the sub-band if needed.
   */
  onUpdateSubBand(update: any) {
    const { subBandId, subObject, prop, value } = update;

    for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
      const subBand = this.ctlCompositeBand.bands[i];

      if (subBand.id === subBandId) {
        if (subObject) {
          subBand[subObject][prop] = value;
        } else {
          subBand[prop] = value;
        }
        this.redraw();
        return;
      }
    }
  }

  /**
   * Helper. Updates tick values for a resource band.
   * Make sure this is only called when needed.
   */
  updateTickValues() {
    for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
      const subBand = this.ctlCompositeBand.bands[i];

      if (subBand.type === 'resource') {
        subBand.computeMinMaxPaintValues();
        subBand.tickValues = (window as any).Util.getLinearTickValues(subBand.minPaintValue, subBand.maxPaintValue, this.height);
      }
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

    for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
      const subBand = this.ctlCompositeBand.bands[i];
      subBand.timeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
      subBand.viewTimeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
    }
  }

  /**
   * Helper. Call when a composite-band should be redrawn.
   */
  redraw() {
    this.ctlCompositeBand.revalidate();
    this.ctlCompositeBand.repaint();
  }

  /**
   * Helper. Call when a composite-band should be resized.
   * Note that this triggers a redraw.
   */
  resize() {
    this.updateTimeAxisXCoordinates();
    this.redraw();
  }
}
