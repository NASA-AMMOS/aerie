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
  ViewChild,
} from '@angular/core';

import {
  RavenBandLeftClick,
  RavenEpoch,
  RavenPoint,
  RavenResourceBand,
  RavenSubBand,
  RavenTimeRange,
} from '../../models';

import { bandById } from '../../util/bands';
import { colorHexToRgbArray } from '../../util/color';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-composite-band',
  styleUrls: ['./raven-composite-band.component.css'],
  templateUrl: './raven-composite-band.component.html',
})
export class RavenCompositeBandComponent
  implements AfterViewInit, OnChanges, OnInit {
  @Input()
  backgroundColor = '#FFFFFF';

  @Input()
  compositeAutoScale = false;

  @Input()
  compositeLogTicks = false;

  @Input()
  compositeScientificNotation = false;

  @Input()
  compositeYAxisLabel = false;

  @Input()
  containerId = '0';

  @Input()
  cursorColor = '#ff0000';

  @Input()
  cursorTime: number | null = null;

  @Input()
  cursorWidth = 1;

  @Input()
  dayCode = '';

  @Input()
  earthSecToEpochSec = 1;

  @Input()
  epoch: RavenEpoch | null = null;

  @Input()
  excludeActivityTypes: string[];

  @Input()
  guides: number[];

  @Input()
  height = 100;

  @Input()
  heightPadding = 0;

  @Input()
  id = '';

  @Input()
  isSelected = false;

  @Input()
  labelFontSize = 9;

  @Input()
  labelWidth = 150;

  @Input()
  lastClickTime: number | null;

  @Input()
  maxTimeRange: RavenTimeRange = { end: 0, start: 0 };

  @Input()
  selectedPoint: RavenPoint | null = null;

  @Input()
  showLastClick = true;

  @Input()
  showTooltip = true;

  @Input()
  subBands: RavenSubBand[] = [];

  @Input()
  viewTimeRange: RavenTimeRange = { end: 0, start: 0 };

  @Output()
  bandLeftClick: EventEmitter<RavenBandLeftClick> = new EventEmitter<
    RavenBandLeftClick
  >();

  @Output()
  updateViewTimeRange: EventEmitter<RavenTimeRange> = new EventEmitter<
    RavenTimeRange
  >();

  @ViewChild('ctlMount')
  ctlMountElementRef: ElementRef;

  ctlCompositeBand: any;
  ctlTimeAxis = new (window as any).TimeAxis({ end: 0, start: 0 });
  ctlTooltip = new (window as any).Tooltip({});
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

    // Composite Auto Scale.
    if (changes.compositeAutoScale && !changes.compositeAutoScale.firstChange) {
      shouldUpdateTicks = true;
      shouldRedraw = true;
    }

    // Composite Log Ticks.
    if (changes.compositeLogTicks && !changes.compositeLogTicks.firstChange) {
      shouldUpdateTicks = true;
      shouldRedraw = true;
    }

    // Composite Scientific Notation.
    if (
      changes.compositeScientificNotation &&
      !changes.compositeScientificNotation.firstChange
    ) {
      shouldUpdateTicks = true;
      shouldRedraw = true;
    }

    // Composite Y-Axis Label.
    if (
      changes.compositeYAxisLabel &&
      !changes.compositeYAxisLabel.firstChange
    ) {
      this.ctlCompositeBand.compositeLabel = this.compositeYAxisLabel;

      if (this.compositeYAxisLabel) {
        // CTL draws the axis label in black when compositeLabel is set.
        this.computeMinMaxTickValuesForCompositeScale();
      } else {
        this.resetMinMaxTickValuesForEachSubBands();
      }

      shouldUpdateTicks = true;
      shouldRedraw = true;
    }

    // Guides.
    if (changes.guides && !changes.guides.firstChange) {
      this.ctlTimeAxis.guideTimes = this.guides;
      shouldRedraw = true;
    }

    // Height. This is the total visible height of the Band.
    if (changes.height && !changes.height.firstChange) {
      this.ctlCompositeBand.height = this.height;
      for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
        const ctlSubBand = this.ctlCompositeBand.bands[i];
        // CtlSubBand height needs to exclude heightPadding.
        ctlSubBand.height = ctlSubBand.heightPadding
          ? this.height - ctlSubBand.heightPadding
          : this.height;
      }

      shouldRedraw = true;
    }

    // Height Padding.
    if (changes.heightPadding && !changes.heightPadding.firstChange) {
      shouldRedraw = true;
    }

    // Label Font Size.
    if (changes.labelFontSize && !changes.labelFontSize.firstChange) {
      shouldRedraw = true;
    }

    // Label Width.
    if (changes.labelWidth && !changes.labelWidth.firstChange) {
      shouldResize = true;
    }

    // Last click time.
    if (changes.lastClickTime && !changes.lastClickTime.firstChange) {
      this.ctlTimeAxis.lastClickTime = this.showLastClick
        ? this.lastClickTime
        : null;
      shouldRedraw = true;
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
        shouldUpdateTicks = true;
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
        } else if (
          !newSelectedPoint &&
          oldSelectedPoint &&
          subBand.intervalsById
        ) {
          const interval = subBand.intervalsById[oldSelectedPoint.uniqueId];

          if (interval) {
            // Set the interval to it's original color.
            interval.color = interval.originalColor;
            shouldRedraw = true;
          }
        } else if (
          newSelectedPoint &&
          !oldSelectedPoint &&
          subBand.intervalsById
        ) {
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

    // Show Last click.
    if (changes.showLastClick && !changes.showLastClick.firstChange) {
      this.ctlTimeAxis.lastClickTime = this.showLastClick
        ? this.lastClickTime
        : null;
      shouldRedraw = true;
    }

    // Time Cursor Color.
    if (changes.cursorColor && !changes.cursorColor.firstChange) {
      this.ctlCompositeBand.decorator.timeCursorColor = colorHexToRgbArray(
        this.cursorColor,
      );
      shouldRedraw = true;
    }

    // Time Cursor Time.
    if (changes.cursorTime && !changes.cursorTime.firstChange) {
      this.ctlViewTimeAxis.now = changes.cursorTime.currentValue;
      shouldRedraw = true;
    }

    // Time Cursor Width.
    if (changes.cursorWidth && !changes.cursorWidth.firstChange) {
      this.ctlCompositeBand.decorator.timeCursorWidth = this.cursorWidth;
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
        shouldUpdateTicks = true;
      }
    }

    // Only update ticks for resource bands once to maintain performance.
    if (shouldUpdateTicks) {
      this.onUpdateTickValues();
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
      compositeYAxisLabel: false,
      height: this.height,
      heightPadding: 0,
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

    this.ctlCompositeBand.compositeLabel = this.compositeYAxisLabel;

    this.ctlCompositeBand.decorator.timeCursorWidth = this.cursorWidth;
    this.ctlCompositeBand.decorator.timeCursorColor = colorHexToRgbArray(
      this.cursorColor,
    );

    this.ctlTimeAxis.updateTimes(
      this.maxTimeRange.start,
      this.maxTimeRange.end,
    );
    this.ctlViewTimeAxis.updateTimes(
      this.viewTimeRange.start,
      this.viewTimeRange.end,
    );
    this.ctlTimeAxis.guideTimes = this.guides;
    this.ctlTimeAxis.lastClickTime = this.showLastClick
      ? this.lastClickTime
      : null;
    this.ctlViewTimeAxis.now = this.cursorTime; // Set `now` for the time-cursor so it draws upon initialization.

    this.ctlMountElementRef.nativeElement.appendChild(
      this.ctlCompositeBand.div,
    );
    this.elementRef.nativeElement.appendChild(this.ctlTooltip.div);
  }

  /**
   * Compute min, max and tick values from all resource band intervals.
   * Set computed min, max for all resource bands.
   * Only show ticks for one sub-band.
   */
  computeMinMaxTickValuesForCompositeScale() {
    let resourceBandCounter = 0;
    let min = null;
    let max = null;

    for (
      let i = 0, length = this.ctlCompositeBand.bands.length;
      i < length;
      ++i
    ) {
      const band = this.ctlCompositeBand.bands[i];

      if (band.type === 'resource') {
        band.autoScale = this.getResourceAutoScale(this.compositeAutoScale);
        // Clear tickValues here to make sure computeMinMaxValues works properly for logTicks.
        band.tickValues = [];
        band.computeMinMaxValues();
        band.computeMinMaxPaintValues();
        min = min ? Math.min(min, band.minPaintValue) : band.minPaintValue;
        max = max ? Math.max(max, band.maxPaintValue) : band.maxPaintValue;

        if (resourceBandCounter > 0) {
          // Keep only one axis for the resource bands.
          band.hideTicks = true;
        }

        resourceBandCounter++;
      }
    }

    // Set minLimit and maxLimit for all resource bands.
    for (
      let i = 0, length = this.ctlCompositeBand.bands.length;
      i < length;
      ++i
    ) {
      const band = this.ctlCompositeBand.bands[i];

      if (band.type === 'resource') {
        band.minLimit = min;
        band.maxLimit = max;
        band.maxPaintValue = max;
        band.minPaintValue = min;
        band.logTicks = this.compositeLogTicks;
        band.scientificNotation = this.compositeScientificNotation;
        band.recomputeTickValues();
      }
    }
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
    this.ctlTooltip.hide();
  }

  /**
   * CTL Event. Called when you left-click a composite band.
   */
  onLeftClick(e: MouseEvent, ctlData: any) {
    if (ctlData.interval) {
      this.bandLeftClick.emit({
        bandId: ctlData.band.id,
        pointId: ctlData.interval.uniqueId,
        subBandId: ctlData.interval.subBandId,
        time: ctlData.time,
      });
    } else {
      this.bandLeftClick.emit({
        bandId: ctlData.band.id,
        pointId: null,
        subBandId: null,
        time: ctlData.time,
      });
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
    if (this.showTooltip) {
      this.ctlTooltip.show(text, e.clientX, e.clientY);
    }
  }

  /**
   * CTL Event. Called when the view is updated.
   */
  onUpdateView(start: number, end: number) {
    if (
      this.viewTimeRange.end !== end ||
      (this.viewTimeRange.start !== start &&
        start !== 0 &&
        end !== 0 &&
        start < end)
    ) {
      this.updateViewTimeRange.emit({ end, start });
    }
  }

  /**
   * Event. Called when a sub-band is added.
   */
  onAddSubBand(subBand: any) {
    subBand.parent = this.ctlCompositeBand;
    this.ctlCompositeBand.addBand(subBand);
    this.onUpdateTickValues();
    this.setIntervalColor();
    this.redraw();
  }

  /**
   * Event. Called when a sub-band is removed.
   */
  onRemoveSubBand(subBandId: string) {
    this.ctlCompositeBand.removeBand(subBandId);
    this.onUpdateTickValues();

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
   * Event. Called when a sub-band emits intervals.
   */
  onUpdateIntervals(update: any) {
    const { subBandId, intervals, intervalsById } = update;

    for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
      const subBand = this.ctlCompositeBand.bands[i];

      if (subBand.id === subBandId) {
        subBand.intervalsById = intervalsById;
        subBand.setIntervals(intervals);
        this.setIntervalColor();
        this.onUpdateTickValues();
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
   * Event. Updates tick values for a resource band.
   * This can be called as an event from the resource-band component or directly to update resource sub-band ticks.
   * Make sure this is only called when needed for performance.
   */
  onUpdateTickValues() {
    if (this.compositeYAxisLabel) {
      this.computeMinMaxTickValuesForCompositeScale();
    } else {
      for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
        const band = this.ctlCompositeBand.bands[i];

        if (band.type === 'resource') {
          // Clear tickValues here to make sure computeMinMaxValues works properly for logTicks.
          band.tickValues = [];
          band.computeMinMaxValues();
          band.computeMinMaxPaintValues();
          band.recomputeTickValues();
          this.redraw();
        }
      }
    }
  }

  /**
   * Helper that returns an auto-scale option for a CTL resource band.
   * VISIBLE_INTERVALS indicates auto-scale of the y-axis ticks.
   * ALL_INTERVALS indicates no auto-scale of the y-axis ticks.
   */
  getResourceAutoScale(autoScale: boolean): number {
    const ctlResourceBand = (window as any).ResourceBand;
    return autoScale
      ? ctlResourceBand.VISIBLE_INTERVALS
      : ctlResourceBand.ALL_INTERVALS;
  }

  /**
   * Helper that sets an interval color if that interval is the selected point.
   * This is useful for when a composite-band is re-initialized (e.g. when it's moved) and there is
   * already a selectedPoint.
   */
  setIntervalColor() {
    if (this.selectedPoint) {
      for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
        if (this.ctlCompositeBand.bands[i].intervalsById) {
          const interval = this.ctlCompositeBand.bands[i].intervalsById[
            this.selectedPoint.uniqueId
          ];

          if (interval) {
            interval.originalColor = interval.color;
            interval.color = this.selectedPointColor;
          }
        }
      }
    }
  }

  /**
   * Helper. Recalculates x-coordinates of the band based on the label width.
   */
  updateTimeAxisXCoordinates() {
    const offsetWidth = this.elementRef.nativeElement.offsetWidth;

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
   * Helper. Reset the computed min, max and tick values for each resource subBand based on subBand intervals.
   * Change autoScale back to the original subBand setting.
   */
  resetMinMaxTickValuesForEachSubBands() {
    for (
      let i = 0, length = this.ctlCompositeBand.bands.length;
      i < length;
      ++i
    ) {
      const ctlBand = this.ctlCompositeBand.bands[i];
      if (ctlBand.type === 'resource') {
        const ravenSubBand = bandById(
          this.subBands,
          ctlBand.id,
        ) as RavenResourceBand;
        if (ravenSubBand) {
          ctlBand.autoScale = this.getResourceAutoScale(ravenSubBand.autoScale);
          ctlBand.logTicks = ravenSubBand.logTicks;
          ctlBand.scientificNotation = ravenSubBand.scientificNotation;
          ctlBand.minLimit = null;
          ctlBand.maxLimit = null;
          // Clear tickValues here to make sure computeMinMaxValues works properly for logTicks.
          ctlBand.tickValues = [];
          ctlBand.computeMinMaxValues();
          ctlBand.computeInterpolatedIntervals();
          ctlBand.recomputeTickValues();
          ctlBand.hideTicks = false;
          ctlBand.labelColor = ctlBand.painter.color;
        }
      }
    }
  }

  /**
   * Helper. Call when a composite-band should be resized.
   * Note that this triggers a redraw.
   */
  resize() {
    this.updateTimeAxisXCoordinates();
    this.redraw();
  }

  /**
   * Helper. Set VISIBLE_INTERVALS (i.e. auto-scale) or ALL_INTERVALS (i.e. no auto-scale)
   * in all ctl resource bands based on the composite auto scale flag.
   */
  setAutoScaleInResourceSubBands() {
    for (
      let i = 0, length = this.ctlCompositeBand.bands.length;
      i < length;
      ++i
    ) {
      const subBand = this.ctlCompositeBand.bands[i];

      if (subBand.type === 'resource') {
        subBand.autoScale = this.getResourceAutoScale(this.compositeAutoScale);
      }
    }
  }
}
