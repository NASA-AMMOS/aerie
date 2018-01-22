import { ChangeDetectionStrategy, Component, Input, SimpleChanges } from '@angular/core';
import { OnChanges, OnInit } from '@angular/core/src/metadata/lifecycle_hooks';
import { SortablejsOptions } from 'angular-sortablejs';

import {
  RavenBand,
  RavenTimeRange,
} from './../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-bands',
  styleUrls: ['./bands.component.css'],
  templateUrl: './bands.component.html',
})
export class BandsComponent implements OnChanges, OnInit {
  @Input() bands: RavenBand[];
  @Input() containerId: string;
  @Input() labelWidth: number;
  @Input() maxTimeRange: RavenTimeRange;
  @Input() viewTimeRange: RavenTimeRange;

  sortablejsOptions: SortablejsOptions;
  sortedAndFilteredBands: RavenBand[];

  ngOnInit() {
    this.sortablejsOptions = {
      animation: 100,
      delay: 0,
      ghostClass: 'sortable-placeholder',
      group: {
        name: `falcon-timeline-bands-${this.containerId}`,
        put: [],
      },
      onEnd: (event: any) => {
        // TODO.
      },
      onUpdate: (event: any) => {
        // TODO.
      },
      scroll: true,
      scrollSensitivity: 30,
      scrollSpeed: 10,
      sort: true,
    };
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.bands) {
      this.sortedAndFilteredBands =
        [...this.bands]
        .sort((a, b) =>  a.sortOrder - b.sortOrder)
        .filter(band => band.containerId === this.containerId);

      this.asyncResize();
    }
  }

  /**
   * CTL is really bad at redrawing when scroll-bars appear.
   * So we add a setTimeout to make sure we resize some time after a scroll-bar potentially appears.
   */
  asyncResize() {
    setTimeout(() => { window.dispatchEvent(new Event('resize')); }, 0);
  }
}
