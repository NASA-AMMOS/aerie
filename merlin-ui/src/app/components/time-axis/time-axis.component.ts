import {
  AfterViewInit,
  ChangeDetectionStrategy,
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
import { getDoyTimestamp } from '../../functions';
import { TimeRange } from '../../types';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-time-axis',
  styleUrls: ['./time-axis.component.css'],
  templateUrl: `./time-axis.component.html`,
})
export class TimeAxisComponent implements AfterViewInit, OnChanges {
  @Input()
  height = 60;

  @Input()
  marginBottom = 30;

  @Input()
  marginLeft = 70;

  @Input()
  marginRight = 70;

  @Input()
  marginTop = 10;

  @Input()
  maxTimeRange: TimeRange = { start: 0, end: 0 };

  @Input()
  viewTimeRange: TimeRange = { start: 0, end: 0 };

  @Output()
  updateViewTimeRange: EventEmitter<TimeRange> = new EventEmitter<TimeRange>();

  @ViewChild('axisX', { static: true })
  axisX: ElementRef;

  @ViewChild('brush', { static: true })
  brush: ElementRef;

  public drawHeight: number = this.height;
  public drawWidth: number;

  constructor(private ref: ElementRef) {
    this.ref.nativeElement.style.setProperty('--height', `${this.height}px`);
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.setDrawBounds();

    if (changes.maxTimeRange && !changes.maxTimeRange.isFirstChange()) {
      this.redraw();
    }
  }

  ngAfterViewInit(): void {
    d3.select(this.brush.nativeElement).on('mousemove', () => {
      this.showTooltip(d3.event);
    });

    d3.select(this.brush.nativeElement).on('mouseleave', () => {
      this.hideTooltip();
    });
  }

  @HostListener('window:resize', ['$event'])
  onResize(): void {
    this.resize();
  }

  drawXAxis(): void {
    const x = this.getXScale();

    const xAxis = d3
      .axisBottom(x.nice())
      .ticks(5)
      .tickFormat((date: Date) => getDoyTimestamp(date.getTime(), false))
      .tickSizeInner(-this.drawHeight)
      .tickPadding(10);

    d3.select(this.axisX.nativeElement)
      .call(xAxis)
      .selectAll('text')
      .style('font-size', '12px');
  }

  drawXBrush(): void {
    const x = this.getXScale();

    const xBrush = d3
      .brushX()
      .extent([
        [0, 0],
        [this.drawWidth, this.drawHeight],
      ])
      .on('start', () => {
        this.showTooltip(d3.event.sourceEvent);
      })
      .on('brush', () => {
        this.showTooltip(d3.event.sourceEvent);
      })
      .on('end', () => {
        this.xBrushEnd();
      });

    const brush = d3.select(this.brush.nativeElement).call(xBrush);
    const range = [
      new Date(this.viewTimeRange.start),
      new Date(this.viewTimeRange.end),
    ];
    brush.call(xBrush.move, range.map(x));
  }

  getDomain(): Date[] {
    return [new Date(this.maxTimeRange.start), new Date(this.maxTimeRange.end)];
  }

  getMousePosition(svg: SVGGElement, event: MouseEvent) {
    const CTM: DOMMatrix = svg.getScreenCTM();
    return {
      x: (event.clientX - CTM.e) / CTM.a,
      y: (event.clientY - CTM.f) / CTM.d,
    };
  }

  getXScale(): d3.ScaleTime<number, number> {
    return d3
      .scaleTime()
      .domain(this.getDomain())
      .rangeRound([0, this.drawWidth]);
  }

  hideTooltip() {
    d3.select('app-tooltip')
      .style('opacity', 0)
      .style('z-index', -1)
      .html('');
  }

  resize(): void {
    this.redraw();
  }

  redraw(): void {
    this.setDrawBounds();
    this.drawXAxis();
    this.drawXBrush();
  }

  setDrawBounds(): void {
    this.drawHeight = this.height - this.marginTop - this.marginBottom;
    this.drawWidth =
      this.ref.nativeElement.clientWidth - this.marginLeft - this.marginRight;
  }

  showTooltip(event: MouseEvent | null): void {
    if (event) {
      const { clientX, clientY } = event;
      const { x } = this.getMousePosition(this.brush.nativeElement, event);

      const xScale = this.getXScale();
      const unixEpochTime = xScale.invert(x).getTime();
      const doyTimestamp = getDoyTimestamp(unixEpochTime);

      const appTooltip = d3.select('app-tooltip');
      appTooltip.html(`${doyTimestamp}`); // Set html first so we can calculate the true width.

      const node = appTooltip.node() as HTMLElement;
      const { width } = node.getBoundingClientRect();
      let xPosition = clientX;
      if (this.drawWidth - xPosition < 0) {
        xPosition = clientX - width;
      }

      appTooltip
        .style('opacity', 0.9)
        .style('left', `${xPosition}px`)
        .style('top', `${clientY}px`)
        .style('z-index', 5);
    }
  }

  xBrushEnd(): void {
    if (!d3.event.sourceEvent) {
      return;
    }
    if (!d3.event.selection) {
      return;
    }

    const x = this.getXScale();
    const [start, end] = d3.event.selection.map(x.invert);

    this.updateViewTimeRange.emit({
      end: end.getTime(),
      start: start.getTime(),
    });
  }
}
