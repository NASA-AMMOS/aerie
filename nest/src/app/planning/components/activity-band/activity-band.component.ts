/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  AfterViewChecked,
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import * as d3 from 'd3';
import { Subject } from 'rxjs';
import {
  ActivityInstance,
  StringTMap,
  TimeRange,
} from '../../../shared/models';
import { timestamp } from '../../../shared/util/time';
import { ActivityInstanceSvg, ActivityInstanceUpdate } from '../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'activity-band',
  styleUrls: ['./activity-band.component.css'],
  templateUrl: './activity-band.component.html',
})
export class ActivityBandComponent
  implements AfterViewInit, AfterViewChecked, OnChanges, OnDestroy {
  @Input()
  height = 400;

  @Input()
  id: string;

  @Input()
  label = '';

  @Input()
  layout = 'waterfall';

  @Input()
  marginBottom = 20;

  @Input()
  marginLeft = 10;

  @Input()
  marginRight = 10;

  @Input()
  marginTop = 10;

  @Input()
  maxTimeRange: TimeRange = { end: 0, start: 0 };

  @Input()
  points: ActivityInstance[] = [];

  @Input()
  selectedActivity: ActivityInstance | null;

  @Input()
  selectedActivityColor = '#fafafa';

  @Input()
  showXAxis = true;

  @Input()
  showYAxis = false;

  @Input()
  viewTimeRange: TimeRange = { end: 0, start: 0 };

  @Output()
  selectActivity: EventEmitter<string | null> = new EventEmitter<
    string | null
  >();

  @Output()
  updateSelectedActivity: EventEmitter<
    ActivityInstanceUpdate
  > = new EventEmitter<ActivityInstanceUpdate>();

  @ViewChild('axisContainerGroup')
  axisContainerGroupTarget: ElementRef;

  @ViewChild('axisX')
  axisXTarget: ElementRef;

  @ViewChild('axisY')
  axisYTarget: ElementRef;

  public drawHeight: number;
  public drawWidth: number;

  public svgPoints: ActivityInstanceSvg[] = [];
  public svgPointsMap: StringTMap<ActivityInstanceSvg> = {};

  private el: HTMLElement;
  private ngUnsubscribe: Subject<{}> = new Subject();
  private xScale: d3.ScaleTime<number, number> = d3.scaleTime();
  private yScale: d3.ScaleLinear<number, number> = d3.scaleLinear();

  constructor(el: ElementRef) {
    this.el = el.nativeElement;
    this.el.style.setProperty('--band-height', `${this.height}px`);
  }

  ngOnChanges(changes: SimpleChanges): void {
    let shouldRedraw = false;
    const shouldResize = false;

    this.setDrawBounds();

    if (changes.points) {
      shouldRedraw = true;
    }

    if (changes.viewTimeRange) {
      shouldRedraw = true;
      this.setXScale();
    }

    // Only resize OR redraw once to maintain performance.
    if (shouldResize) {
      this.resize();
    } else if (shouldRedraw) {
      this.redraw();
    }
  }

  ngOnDestroy(): void {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  ngAfterViewInit(): void {
    this.redraw();
  }

  ngAfterViewChecked(): void {
    this.setDragSelectedActivityEvents();
    this.setDragHandles('left');
    this.setDragHandles('right');
    this.setUpTooltips();
  }

  /**
   * Global Event. Called on window resize.
   */
  @HostListener('window:resize', ['$event'])
  onResize(): void {
    this.resize();
  }

  /**
   * trackBy for bands list.
   */
  pointsTrackByFn(_: number, item: ActivityInstanceSvg): string {
    return item.activityId;
  }

  /**
   * Helper. Finds the mouse (x, y) position of an SVG element based on a screen mouse click.
   * TODO: Replace any with a concrete type.
   */
  getMousePosition(svg: SVGGElement, event: MouseEvent) {
    const CTM = svg.getScreenCTM() as any;
    return {
      x: (event.clientX - CTM.e) / CTM.a,
      y: (event.clientY - CTM.f) / CTM.d,
    };
  }

  /**
   * Draw SVG activities.
   */
  drawSvgActivities(): void {
    if (this.layout === 'waterfall') {
      this.drawSvgActivitiesWaterfall();
    } else {
      this.drawSvgActivitiesPacked();
    }
  }

  /**
   * Draw SVG activities in a "packed" layout.
   */
  drawSvgActivitiesPacked(): void {
    this.svgPoints = [];
    this.svgPointsMap = {};
    // TODO.
  }

  /**
   * Draw SVG activities in a 'waterfall' layout.
   */
  drawSvgActivitiesWaterfall(): void {
    this.svgPoints = [];
    this.svgPointsMap = {};

    const rowHeight = Math.max(
      5,
      Math.floor(this.drawHeight / this.points.length),
    );
    const activityHeight = Math.min(20, rowHeight - Math.ceil(rowHeight / 3));
    let rowY = 0;

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];

      const start = this.xScale(point.start * 1000);
      const end = this.xScale((point.start + point.duration) * 1000);
      const range = end - start;

      const fill = point.color;
      const height = activityHeight;
      const stroke = 'black';
      const width = Math.max(1.0, range);
      const y = (point.y !== null ? point.y : rowY) as number;

      const labelFill = 'black';
      const labelFontFamily = 'Verdana';
      const labelFontSize = 10;
      const labelWidth = this.getLabelWidth(
        point.name,
        `${labelFontSize}px ${labelFontFamily}`,
      );
      const labelX = this.getLabelXPosition(start, width, labelWidth);
      const labelY = this.getLabelYPosition(y, height, labelFontSize);

      let showLabel = false;
      if (height > 5) {
        showLabel = true;
      }

      const svgPoint = {
        ...point,
        fill,
        height,
        labelFill,
        labelFontFamily,
        labelFontSize,
        labelX,
        labelY,
        showLabel,
        stroke,
        width,
        x: start,
        y,
      };

      this.svgPoints.push(svgPoint);
      this.svgPointsMap[point.activityId] = svgPoint;

      if (rowY + rowHeight + activityHeight <= this.drawHeight) {
        rowY += rowHeight;
      }
    }
  }

  /**
   * Builds and draws the x-axis.
   */
  drawXAxis(): void {
    if (this.showXAxis) {
      const xAxis = d3
        .axisBottom(this.xScale)
        .ticks(5)
        .tickFormat(d3.timeFormat('%B %d, %Y'))
        .tickSizeInner(-this.drawHeight);

      d3.select(this.axisXTarget.nativeElement).call(xAxis);
    }
  }

  /**
   * Builds and draws the y-axis.
   */
  drawYAxis(): void {
    if (this.showYAxis) {
      const yAxis = d3.axisLeft(this.yScale);
      const axis = d3.select(this.axisYTarget.nativeElement);
      const container = d3.select(this.axisContainerGroupTarget.nativeElement);

      axis.call(yAxis);

      container
        .append('text')
        .attr('transform', 'rotate(-90)')
        .attr('y', -30)
        .attr('x', 0 - this.drawHeight / 2)
        .attr('dy', '1em')
        .style('text-anchor', 'middle')
        .text(this.label);
    }
  }

  /**
   * Returns the label-x position for an activity bar.
   */
  getLabelXPosition(x: number, width: number, textLength: number): number {
    return x + 0.5 * width - 0.5 * textLength;
  }

  /**
   * Returns the label-y position for an activity bar.
   */
  getLabelYPosition(y: number, height: number, labelFontSize: number): number {
    return y + height / 2 + labelFontSize / 2;
  }

  /**
   * Uses canvas.measureText to compute and return the width of the given text of given font in pixels.
   * @see https://stackoverflow.com/questions/118241/calculate-text-width-with-javascript/21015393#21015393
   */
  getLabelWidth(text: string, font: string): number {
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d') as CanvasRenderingContext2D;
    context.font = font;
    const metrics = context.measureText(text);
    return metrics.width;
  }

  /**
   * Get the y-value for the left and right drag handle of a selected activity rect.
   */
  getYDragHandle(y: number, height: number): number {
    return y + 0.5 * height;
  }

  /**
   * Get the x-value for the right drag handle of a selected activity rect.
   */
  getXDragHandleRight(x: number, width: number): number {
    return x + width;
  }

  /**
   * Set the x-scale.
   * Note the view time ranges start and end are in seconds so we need to convert
   * them to milliseconds since thats that the date constructor expects.
   */
  setXScale(): void {
    this.xScale = d3
      .scaleTime()
      .domain([
        new Date(this.viewTimeRange.start * 1000),
        new Date(this.viewTimeRange.end * 1000),
      ])
      .range([0, this.drawWidth]);
  }

  /**
   * Set the y-scale.
   */
  setYScale(): void {
    this.yScale = d3
      .scaleLinear()
      .domain([0, this.drawHeight])
      .range([this.drawHeight, 0]);
  }

  /**
   * Click callback. Called when an activity <g> element is clicked.
   */
  onActivitySelected(event: MouseEvent, id: string): void {
    if (!this.selectedActivity || this.selectedActivity.activityId !== id) {
      this.selectActivity.emit(id);
    }
  }

  /**
   * Sets up on hover tooltip for activity instances
   */
  setUpTooltips(): void {
    for (let i = 0; i < this.svgPoints.length; i++) {
      const point = this.svgPoints[i];
      const id = point.activityId;

      // TODO: Change tooltip color to the same color as the activity instance in point.color
      const tooltipTemplate = `
      <div id="activity-tooltip">
        <p class="activity-tooltip-name">
          ${point.name}
        </p>
        <p>
          ${point.activityType}
        </p>
        <p>
          ${point.activityId}
        </p>
        <p>
          ${point.duration}
        </p>
        <p>
          ${point.intent}
        </p>
      </div>
    `;
      const rectTarget = d3.select(`#rect-${id}`);

      const tooltip = d3
        .select('#activity-tooltip-container')
        .html(tooltipTemplate);

      rectTarget
        .on('mouseover', () => {
          tooltip
            .transition()
            .ease(d3.easeLinear)
            .duration(100)
            .style('opacity', 1);
        })
        .on('mousemove', () => {
          const { xPosition, yPosition } = this.calculateTooltipPosition(
            d3.event,
          );

          tooltip
            .style('top', `${yPosition}px`)
            .style('left', `${xPosition}px`);
        })
        .on('mouseout', () => {
          tooltip
            .transition()
            .ease(d3.easeLinear)
            .duration(100)
            .style('opacity', 0);
        });
    }
  }

  /**
   * Calculates the position for the activity instance tooltip
   */
  calculateTooltipPosition(event: MouseEvent) {
    const xTolerance = 300;
    const containerWidth = this.drawWidth;
    // Offsets are used to position tooltip so it isn't out of bounds
    const xOffset = -85;
    const yOffset = -350;
    let xPosition = event.clientX + xOffset;
    let yPosition = event.clientY + yOffset;

    if (containerWidth - xPosition < xTolerance) {
      xPosition = d3.event.clientX - xTolerance + xOffset;
    }

    if (yPosition < 0) {
      yPosition += 200;
    }

    return { xPosition, yPosition };
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
    this.setYScale();
    this.drawXAxis();
    this.drawYAxis();
    this.drawSvgActivities();
  }

  /**
   * Sets drag events for start and end handles for the selected activity instance
   */
  setDragHandles(selector: string): void {
    if (this.selectedActivity) {
      const id = this.selectedActivity.activityId;
      const rectTarget = this.el.querySelector(`#rect-${id}`) as SVGRectElement;
      const handle = this.el.querySelector(
        `#circle-drag-handle-${selector}-${id}`,
      ) as SVGElement;
      const point = this.svgPointsMap[id];
      const tooltip = this.el.querySelector(
        '#activity-tooltip-container',
      ) as HTMLElement;

      const dragHandler = d3
        .drag()
        .on('drag', () => {
          const currentPosition = this.getMousePosition(
            rectTarget,
            d3.event.sourceEvent,
          );

          const newX = currentPosition.x;
          const newXTime = this.xScale.invert(newX).getTime() / 1000;
          let newDuration;

          if (selector === 'left') {
            newDuration = point.duration - (newXTime - point.start);
            this.svgPointsMap[id].start = newXTime;
            this.svgPointsMap[id].x = newX;
          } else if (selector === 'right') {
            newDuration = newXTime - point.start;
          } else {
            // TODO: Error handling or extend for up and down direction
            newDuration = point.duration;
          }

          this.svgPointsMap[id].duration = newDuration;

          tooltip.style.visibility = 'hidden';
          this.updateRect(id, point.x, null);
        })
        .on('end', () => {
          tooltip.style.visibility = 'visible';
          this.setNewStartAndEnd(id, point.x, null, point.duration);
        });

      dragHandler(d3.select(handle));
    }
  }

  /**
   * Sets drag events for the selected activity rect.
   */
  setDragSelectedActivityEvents(): void {
    if (this.selectedActivity) {
      const id = this.selectedActivity.activityId;
      const gTarget = this.el.querySelector(`#g-${id}`) as SVGGElement;
      const rectTarget = this.el.querySelector(`#rect-${id}`) as SVGRectElement;
      const rectTargetHeight = parseFloat(rectTarget.getAttribute(
        'height',
      ) as string);
      const point = this.svgPointsMap[id];
      const startTooltip = d3.select('#activity-tooltip-start');
      const tooltip = this.el.querySelector(
        '#activity-tooltip-container',
      ) as HTMLElement;

      let offsetX = 0;
      let offsetY = 0;

      const dragHandler = d3
        .drag()
        .on('start', () => {
          const clickPosition = this.getMousePosition(
            rectTarget,
            d3.event.sourceEvent,
          );
          offsetX = clickPosition.x - point.x;
          offsetY = clickPosition.y - point.y;

          startTooltip
            .transition()
            .ease(d3.easeLinear)
            .duration(100)
            .style('opacity', 1);

          // Hides the metadata tooltip during drag
          tooltip.style.visibility = 'hidden';
        })
        .on('drag', () => {
          const currentPosition = this.getMousePosition(
            rectTarget,
            d3.event.sourceEvent,
          );
          const x = currentPosition.x - offsetX;
          const y = currentPosition.y - offsetY;

          if (y >= 0 && y <= this.drawHeight - rectTargetHeight) {
            const tooltipX = x < 0 ? 0 : x;
            const tooltipY = y <= 5 ? 125 : y;
            let xDeltaSvg = currentPosition.x - offsetX;
            const flipSign = xDeltaSvg < 0;
            xDeltaSvg = flipSign ? -xDeltaSvg : xDeltaSvg;
            let newStartMs = this.xScale.invert(xDeltaSvg).getTime() / 1000;
            newStartMs = flipSign ? -newStartMs : newStartMs;

            startTooltip
              .style('top', `${tooltipY - 100}px`)
              .style('left', `${tooltipX}px`)
              .html(
                `<p><strong>New Start Time:</strong></p><p>${timestamp(
                  newStartMs,
                )}</p>`,
              );

            this.updateRect(id, x, y);
          }
        })
        .on('end', () => {
          const pos = this.getMousePosition(rectTarget, d3.event.sourceEvent);
          const x = pos.x - offsetX;
          let y = pos.y - offsetY;

          // Clamp y.
          if (y < 0) {
            y = 0;
          } else if (y > this.drawHeight - rectTargetHeight) {
            y = this.drawHeight - rectTargetHeight;
          }

          // Sets the metadata tooltip to appear after drag is done
          const { xPosition, yPosition } = this.calculateTooltipPosition(
            d3.event.sourceEvent,
          );

          tooltip.style.top = `${yPosition}px`;
          tooltip.style.left = `${xPosition}px`;
          tooltip.style.visibility = 'visible';

          startTooltip
            .transition()
            .ease(d3.easeLinear)
            .duration(100)
            .style('opacity', 0);

          this.setNewStartAndEnd(id, x, y, null);
        });

      dragHandler(d3.select(gTarget));
    }
  }

  /**
   * Sets the drawHeight and drawWidth of this component.
   * Make sure drawHeight and drawWidth are set on each change.
   */
  setDrawBounds(): void {
    this.drawHeight = this.height - this.marginTop - this.marginBottom;
    this.drawWidth = this.el.clientWidth - this.marginLeft - this.marginRight;
  }

  /**
   * Emits new start and end time for an activity after a drag event.
   */
  setNewStartAndEnd(
    id: string,
    x: number,
    newY: number | null,
    newDuration: number | null,
  ): void {
    const point = this.svgPointsMap[id];
    const newStart = this.xScale.invert(x).getTime() / 1000;
    const duration = newDuration || point.duration;
    const newEnd = newStart + duration;
    const y = newY || point.y;

    // If no attributes have changed, skip the update
    if (newStart === point.start && duration === point.duration && y === newY) {
      return;
    }

    this.updateSelectedActivity.emit({
      activityId: id,
      duration,
      end: newEnd,
      start: newStart,
      y,
    });
  }

  /**
   * Updates the activity band svg drawing based on drag interactions
   */
  updateRect(id: string, x: number, newY: number | null) {
    const point = this.svgPointsMap[id];

    const start = this.xScale(point.start * 1000);
    const end = this.xScale((point.start + point.duration) * 1000);
    const width = end - start;
    const y = newY || point.y;

    const labelWidth = this.getLabelWidth(
      point.name,
      `${point.labelFontSize}px ${point.labelFontFamily}`,
    );
    const labelX = this.getLabelXPosition(x, width, labelWidth);
    const labelY = this.getLabelYPosition(y, point.height, point.labelFontSize);

    d3.select(`#rect-${id}`)
      .attr('x', x)
      .attr('width', width)
      .attr('y', y);

    d3.select(`#text-${id}`)
      .attr('x', labelX)
      .attr('y', labelY);

    d3.select(`#circle-drag-handle-left-${id}`)
      .attr('cx', x)
      .attr('cy', this.getYDragHandle(y, point.height));

    d3.select(`#circle-drag-handle-right-${id}`)
      .attr('cx', this.getXDragHandleRight(x, width))
      .attr('cy', this.getYDragHandle(y, point.height));
  }
}
