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
  Output,
  SimpleChanges,
} from '@angular/core';

import {
  RavenResourceBand,
  RavenSubBand,
  RavenTimeRange,
} from './../../../models';

import {
  FalconResourceBand,
} from './../../classes';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'falcon-composite-band',
  styleUrls: ['./falcon-composite-band.component.css'],
  templateUrl: './falcon-composite-band.component.html',
})

export class FalconCompositeBandComponent implements AfterViewInit, OnChanges {
  @Input() height: number;
  @Input() heightPadding: number;
  @Input() id: string;
  @Input() labelWidth: number;
  @Input() maxTimeRange: RavenTimeRange;
  @Input() showTooltip: boolean;
  @Input() subBands: RavenSubBand[];
  @Input() viewTimeRange: RavenTimeRange;

  @Output() bandClick: EventEmitter<string> = new EventEmitter<string>();

  ctlCompositeBand: any;
  ctlTimeAxis = new (window as any).TimeAxis({ end: 0, start: 0 });
  ctlViewTimeAxis = new (window as any).TimeAxis({ end: 0, start: 0 });

  constructor(public elementRef: ElementRef) {
    this.ctlCompositeBand = new (window as any).CompositeBand({
      timeAxis: this.ctlTimeAxis,
      viewTimeAxis: this.ctlViewTimeAxis,
    });

    elementRef.nativeElement.appendChild(this.ctlCompositeBand.div);
  }

  ngAfterViewInit() {
    this.resize();
  }

  ngOnChanges(changes: SimpleChanges) {
    console.log('changes: ', changes);

    let shouldResize = false;
    let shouldRedraw = false;

    // Height.
    if (changes.height) {
      this.ctlCompositeBand.height = changes.height.currentValue;

      if (!changes.height.firstChange) {
        shouldRedraw = true;
      }
    }

    // Height Padding.
    if (changes.heightPadding) {
      this.ctlCompositeBand.heightPadding = changes.heightPadding.currentValue;

      if (!changes.heightPadding.firstChange) {
        shouldRedraw = true;
      }
    }

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

    // Sub Band.
    if (changes.subBands) {
      if (changes.subBands.firstChange) {
        for (let i = 0, l = changes.subBands.currentValue.length; i < l; ++i) {
          const subBand: RavenSubBand = changes.subBands.currentValue[i];

          if (subBand.type === 'resource') {
            const resourceBand = new FalconResourceBand(
              this.ctlCompositeBand,
              this.ctlTimeAxis,
              this.ctlViewTimeAxis,
              subBand as RavenResourceBand,
            );
            this.ctlCompositeBand.addBand(resourceBand.ctlResourceBand);
          }
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
  @HostListener('click', ['$event'])
  onBandClick() {
    this.bandClick.emit(this.id);
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

    for (let i = 0, l = this.ctlCompositeBand.bands.length; i < l; ++i) {
      const subBand = this.ctlCompositeBand.bands[i];
      subBand.timeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
      subBand.viewTimeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
    }
  }

  /**
   *
   */
  redraw() {
    console.log('composite-band redraw');
    this.ctlCompositeBand.revalidate();
    this.ctlCompositeBand.repaint();
  }

  /**
   *
   */
  resize() {
    console.log('composite-band resize');
    this.updateTimeAxisXCoordinates();
    this.redraw();
  }
}
