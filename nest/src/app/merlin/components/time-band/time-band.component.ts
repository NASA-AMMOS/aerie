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
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import * as d3 from 'd3';
import { TimeRange } from '../../../shared/models';

@Component({
  selector: 'time-band',
  styleUrls: ['./time-band.component.css'],
  templateUrl: './time-band.component.html',
})
export class TimeBandComponent implements AfterViewInit, OnChanges {
  @Input()
  height = 50;

  @Input()
  marginBottom = 20;

  @Input()
  marginLeft = 10;

  @Input()
  marginRight = 10;

  @Input()
  marginTop = 10;

  @Input()
  maxTimeRange: TimeRange;

  @Input()
  viewTimeRange: TimeRange;

  @Output()
  updateViewTimeRange: EventEmitter<TimeRange> = new EventEmitter<TimeRange>();

  @ViewChild('axisX', { static: true })
  axisXTarget: ElementRef;

  @ViewChild('brush', { static: true })
  brushTarget: ElementRef;

  public drawHeight: number;
  public drawWidth: number;
  public el: HTMLElement;

  constructor(el: ElementRef) {
    this.el = el.nativeElement;
    this.el.style.setProperty('--time-band-height', `${this.height}px`);
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.setDrawBounds();

    if (changes.viewTimeRange && changes.viewTimeRange.isFirstChange()) {
      this.redraw();
    }
  }

  ngAfterViewInit(): void {
    this.redraw();
  }

  @HostListener('window:resize', ['$event'])
  onResize(): void {
    this.resize();
  }

  /**
   * Get the domain of this band.
   */
  getDomain(): Date[] {
    return [
      new Date(this.maxTimeRange.start * 1000),
      new Date(this.maxTimeRange.end * 1000),
    ];
  }

  /**
   * Get the x-scale.
   */
  getXScale(): d3.ScaleTime<number, number> {
    return d3
      .scaleTime()
      .domain(this.getDomain())
      .rangeRound([0, this.drawWidth]);
  }

  /**
   * Draw the x-axis.
   */
  drawXAxis(): void {
    const x = this.getXScale();

    const xAxis = d3
      .axisBottom(x)
      .ticks(5)
      .tickFormat(d3.timeFormat('%B %d, %Y'))
      .tickSizeInner(-this.drawHeight);

    d3.select(this.axisXTarget.nativeElement).call(xAxis);
  }

  /**
   * Draw the x-brush.
   */
  drawXBrush(): void {
    const x = this.getXScale();

    const xBrush = d3
      .brushX()
      .handleSize(1)
      .extent([[0, 0], [this.drawWidth, this.drawHeight]])
      .on('end', () => this.xBrushEnd());

    const brush = d3.select(this.brushTarget.nativeElement).call(xBrush);

    d3.select('.handle--e').attr('fill', '#000');
    d3.select('.handle--w').attr('fill', '#000');

    brush.call(xBrush.move, this.getDomain().map(x));
  }

  /**
   * Called to resize the band.
   */
  resize(): void {
    this.redraw();
  }

  /**
   * Called to redraw the band.
   */
  redraw(): void {
    this.setDrawBounds();
    this.drawXAxis();
    this.drawXBrush();
  }

  /**
   * Sets the drawHeight and drawWidth of this component.
   * Make sure this is called on each change.
   */
  setDrawBounds(): void {
    this.drawHeight = this.height - this.marginTop - this.marginBottom;
    this.drawWidth = this.el.clientWidth - this.marginLeft - this.marginRight;
  }

  /**
   * Called when the x-brush finishes brushing.
   */
  xBrushEnd(): void {
    if (!d3.event.sourceEvent) {
      return;
    } // Only transition after input.
    if (!d3.event.selection) {
      return;
    } // Ignore empty selections.

    const x = this.getXScale();
    const viewTimeRange = d3.event.selection.map(x.invert);

    this.updateViewTimeRange.emit({
      end: viewTimeRange[1].getTime() / 1000,
      start: viewTimeRange[0].getTime() / 1000,
    });
  }
}
