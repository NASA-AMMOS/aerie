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
import { fromEvent, Subject, Subscription } from 'rxjs';
import { filter, map, mergeMap, takeUntil, tap } from 'rxjs/operators';
import { ActivityInstance } from '../../../../../../schemas';
import {
  RavenActivitySvg,
  RavenActivityUpdate,
  RavenTimeRange,
  StringTMap,
} from '../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'nest-activity-band',
  styleUrls: ['./nest-activity-band.component.css'],
  templateUrl: './nest-activity-band.component.html',
})
export class NestActivityBandComponent
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
  maxTimeRange: RavenTimeRange = { end: 0, start: 0 };

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
  viewTimeRange: RavenTimeRange = { end: 0, start: 0 };

  @Output()
  selectActivity: EventEmitter<string | null> = new EventEmitter<
    string | null
  >();

  @Output()
  updateSelectedActivity: EventEmitter<RavenActivityUpdate> = new EventEmitter<
    RavenActivityUpdate
  >();

  @ViewChild('axisContainerGroup')
  axisContainerGroupTarget: ElementRef;

  @ViewChild('axisX')
  axisXTarget: ElementRef;

  @ViewChild('axisY')
  axisYTarget: ElementRef;

  public drawHeight: number;
  public drawWidth: number;

  public svgPoints: RavenActivitySvg[] = [];
  public svgPointsMap: StringTMap<RavenActivitySvg> = {};

  private dragSubscription: Subscription;
  private el: HTMLElement;
  private ngUnsubscribe: Subject<{}> = new Subject();

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

    if (changes.viewTimeRange && !changes.viewTimeRange.firstChange) {
      shouldRedraw = true;
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
  pointsTrackByFn(_: number, item: RavenActivitySvg): string {
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

    const xScale = this.getXScale();
    const rowHeight = Math.max(
      5,
      Math.floor(this.drawHeight / this.points.length),
    );
    const activityHeight = Math.min(20, rowHeight - Math.ceil(rowHeight / 3));
    let rowY = 0;

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];

      const start = xScale(point.start * 1000);
      const end = xScale((point.start + point.duration) * 1000);
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
      const x = this.getXScale();
      const xAxis = d3
        .axisBottom(x)
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
      const y = this.getYScale();
      const yAxis = d3.axisLeft(y);
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
   * Returns x-scale.
   * Note the view time ranges start and end are in seconds so we need to convert
   * them to milliseconds since thats that the date constructor expects.
   */
  getXScale(): d3.ScaleTime<number, number> {
    return d3
      .scaleTime()
      .domain([
        new Date(this.viewTimeRange.start * 1000),
        new Date(this.viewTimeRange.end * 1000),
      ])
      .range([0, this.drawWidth]);
  }

  /**
   * Returns y-scale.
   */
  getYScale(): d3.ScaleLinear<number, number> {
    return d3
      .scaleLinear()
      .domain([0, this.drawHeight])
      .range([this.drawHeight, 0]);
  }

  /**
   * Click callback. Called when an activity <g> element is clicked.
   */
  onActivitySelected(id: string): void {
    this.selectActivity.emit(id);
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
    this.drawYAxis();
    this.drawSvgActivities();
  }

  /**
   * Helper that sets drag events for the selected activity.
   */
  setDragSelectedActivityEvents(): void {
    if (this.selectedActivity) {
      if (this.dragSubscription) {
        this.dragSubscription.unsubscribe();
      }

      const id = this.selectedActivity.activityId;
      const gTarget = this.el.querySelector(`#g-${id}`) as SVGGElement;
      const rectTarget = this.el.querySelector(`#rect-${id}`) as SVGRectElement;

      const rectTargetHeight = parseFloat(rectTarget.getAttribute(
        'height',
      ) as string);

      const mousedown = fromEvent(gTarget, 'mousedown');
      const mousemove = fromEvent(document, 'mousemove');
      const mouseup = fromEvent(document, 'mouseup');

      this.dragSubscription = mousedown
        .pipe(
          mergeMap((md: MouseEvent) => {
            md.preventDefault();

            const rectX = parseFloat(rectTarget.getAttribute('x') as string);
            const rectY = parseFloat(rectTarget.getAttribute('y') as string);

            const offset = this.getMousePosition(rectTarget, md);
            offset.x -= rectX;
            offset.y -= rectY;

            return mousemove.pipe(
              map((mm: MouseEvent) => {
                mm.preventDefault();
                const pos = this.getMousePosition(rectTarget, mm);
                return {
                  x: pos.x - offset.x,
                  y: pos.y - offset.y,
                };
              }),
              takeUntil(
                mouseup.pipe(
                  tap((mu: MouseEvent) => {
                    mu.preventDefault();
                    const pos = this.getMousePosition(rectTarget, mu);
                    const x = pos.x - offset.x;
                    let y = pos.y - offset.y;

                    // Clamp y.
                    if (y < 0) {
                      y = 0;
                    } else if (y > this.drawHeight - rectTargetHeight) {
                      y = this.drawHeight - rectTargetHeight;
                    }

                    this.setNewStartAndEnd(id, x, y);
                  }),
                ),
              ),
            );
          }),
          filter(({ y }) => y >= 0 && y <= this.drawHeight - rectTargetHeight),
          takeUntil(this.ngUnsubscribe),
        )
        .subscribe(({ x, y }) => {
          const point = this.svgPointsMap[id];

          const labelWidth = this.getLabelWidth(
            point.name,
            `${point.labelFontSize}px ${point.labelFontFamily}`,
          );
          const labelX = this.getLabelXPosition(x, point.width, labelWidth);
          const labelY = this.getLabelYPosition(
            y,
            point.height,
            point.labelFontSize,
          );

          d3.select(`#rect-${id}`)
            .attr('x', x)
            .attr('y', y);

          d3.select(`#text-${id}`)
            .attr('x', labelX)
            .attr('y', labelY);

          d3.select(`#circle-drag-handle-left-${id}`)
            .attr('cx', x)
            .attr('cy', this.getYDragHandle(y, point.height));

          d3.select(`#circle-drag-handle-right-${id}`)
            .attr('cx', this.getXDragHandleRight(x, point.width))
            .attr('cy', this.getYDragHandle(y, point.height));
        });
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
  setNewStartAndEnd(id: string, x: number, y: number) {
    const point = this.svgPointsMap[id];
    const xScale = this.getXScale();
    const newStart = xScale.invert(x).getTime() / 1000;
    const duration = point.duration;
    const newEnd = newStart + duration;

    this.updateSelectedActivity.emit({
      activityId: id,
      end: newEnd,
      start: newStart,
      y,
    });
  }
}
