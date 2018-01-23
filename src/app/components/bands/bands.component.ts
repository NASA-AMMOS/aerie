import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  HostListener,
  Input,
  Output,
  SimpleChanges,
} from '@angular/core';
import { OnChanges, OnInit } from '@angular/core/src/metadata/lifecycle_hooks';
import { SortablejsOptions } from 'angular-sortablejs';

import {
  RavenBand,
  RavenSortMessage,
  RavenTimeRange,
  StringTMap,
} from './../../models';

export interface BandClickEvent extends Event {
  detail: StringTMap<string>;
}

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

  @Output() bandClick: EventEmitter<string> = new EventEmitter<string>();
  @Output() newSort: EventEmitter<StringTMap<RavenSortMessage>> = new EventEmitter<StringTMap<RavenSortMessage>>();

  sortablejsOptions: SortablejsOptions;
  sortedAndFilteredBands: RavenBand[];

  ngOnInit() {
    this.sortablejsOptions = {
      animation: 100,
      delay: 0,
      ghostClass: 'sortable-placeholder',
      group: 'bands',
      onAdd: this.onSort.bind(this),
      onEnd: this.onSort.bind(this),
      onRemove: this.onSort.bind(this),
      scroll: true,
      scrollSensitivity: 50,
      scrollSpeed: 10,
      sort: true,
    };
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.bands) {
      this.sortedAndFilteredBands =
        [...this.bands]
        .filter(band => band.containerId === this.containerId)
        .sort((a, b) =>  a.sortOrder - b.sortOrder);
    }
  }

  /**
   * Event. Called when a `falcon-band-click` event is fired from a falcon band.
   */
  @HostListener('falcon-band-click', ['$event'])
  onBandClick(e: BandClickEvent) {
    e.stopPropagation();
    this.bandClick.emit(e.detail.bandId);
  }

  /**
   * Helper to sort bands after a sortablejs message.
   * By the time sortedAndFiltered bands gets to this function they should be in their new order.
   * We use that new order to build a dictionary of bands by id to update the store.
   *
   * TODO: Replace 'any' with a concrete type.
   */
  onSort(e: any) {
    const sort: StringTMap<RavenSortMessage> = {};

    this.sortedAndFilteredBands.forEach((b, index) => {
      sort[b.id] = {
        containerId: this.containerId,
        sortOrder: index,
      };
    });

    this.newSort.emit(sort);
  }
}
