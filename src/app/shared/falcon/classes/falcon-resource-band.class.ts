/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  RavenPoint,
  RavenResourceBand,
  RavenResourcePoint,
  RavenTimeRange,
} from './../../models';

export class FalconResourceBand {
  autoTickValues: boolean;
  color: number[];
  fill: boolean;
  fillColor: number[];
  height: number;
  heightPadding: number;
  hideTicks: boolean;
  id: string;
  interpolation: string;
  label: string;
  labelColor: number[];
  labelWidth: number;
  maxTimeRange: RavenTimeRange;
  minorLabels: string[];
  name: string;
  points: RavenResourcePoint[];
  rescale: boolean;
  selectedPoint: RavenPoint;
  showIcon: boolean;
  showTooltip: boolean;
  tickValues: string[];
  viewTimeRange: RavenTimeRange;

  ctlIntervalById: any;
  ctlResourceBand: any;

  /**
   * Default constructor.
   */
  constructor(ctlCompositeBand: any, ctlTimeAxis: any, ctlViewTimeAxis: any, band: RavenResourceBand) {
    this.ctlResourceBand = new (window as any).ResourceBand({
      autoScale: (window as any).ResourceBand.VISIBLE_INTERVALS,
      autoTickValues: band.autoTickValues,
      height: band.height,
      heightPadding: band.heightPadding,
      hideTicks: false,
      interpolation: band.interpolation,
      intervals: [],
      label: band.label,
      labelColor: band.labelColor,
      minorLabels: band.minorLabels,
      name: band.name,
      // onDblLeftClick: this._onDblLeftClick.bind(this),
      // onHideTooltip: this._onHideTooltip.bind(this),
      // onLeftClick: this._onLeftClick.bind(this),
      // onRightClick: this._onRightClick.bind(this),
      // onShowTooltip: this._onShowTooltip.bind(this),
      // onUpdateView: this._onUpdateView.bind(this),
      painter: new (window as any).ResourcePainter({
        color: band.color,
        fill: band.fill,
        fillColor: band.fillColor,
        showIcon: band.showIcon,
      }),
      rescale: band.rescale,
      tickValues: [],
      timeAxis: ctlTimeAxis,
      viewTimeAxis: ctlViewTimeAxis,
    });

    this.setIntervals(band);
  }

  /**
   * Helper. Sets intervals for the CTL resource band.
   */
  setIntervals(band: RavenResourceBand) {
    this.ctlIntervalById = {};
    const intervals = [];

    for (let i = 0, l = band.points.length; i < l; ++i) {
      const point = band.points[i];

      const interval = new (window as any).DrawableInterval({
        color: band.color,
        end: point.start,
        endValue: point.value,
        icon: 'circle',
        id: point.id,
        // onGetTooltipText: this._onGetTooltipText.bind(this),
        opacity: 0.9,
        properties: {
          Value: point.value,
        },
        start: point.start,
        startValue: point.value,
      });

      // Set the unique ID separately since it is not a DrawableInterval prop.
      interval.uniqueId = point.uniqueId;

      this.ctlIntervalById[interval.uniqueId] = interval;
      intervals.push(this.ctlIntervalById[interval.uniqueId]);
    }

    intervals.sort((window as any).DrawableInterval.earlyStartEarlyEnd);

    this.ctlResourceBand.setIntervals(intervals); // This resets interpolation in CTL so we must re-set it on the next line.
    this.ctlResourceBand.setInterpolation(band.interpolation);
  }
}
