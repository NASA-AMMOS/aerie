/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';

import {
  SortablejsOptions,
} from 'angular-sortablejs';

import {
  FalconCompositeBandLeftClickEvent,
  RavenBandLeftClick,
  RavenCompositeBand,
  RavenPoint,
  RavenSortMessage,
  RavenTimeRange,
  StringTMap,
} from './../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-bands',
  styleUrls: ['./raven-bands.component.css'],
  templateUrl: './raven-bands.component.html',
})
export class RavenBandsComponent implements OnChanges, OnInit {
  @Input() bands: RavenCompositeBand[];
  @Input() containerId: string;
  @Input() labelWidth: number;
  @Input() maxTimeRange: RavenTimeRange;
  @Input() selectedBandId: string;
  @Input() selectedPoint: RavenPoint;
  @Input() viewTimeRange: RavenTimeRange;

  @Output() bandClick: EventEmitter<string> = new EventEmitter<string>();
  @Output() bandLeftClick: EventEmitter<RavenBandLeftClick> = new EventEmitter<RavenBandLeftClick>();
  @Output() newSort: EventEmitter<StringTMap<RavenSortMessage>> = new EventEmitter<StringTMap<RavenSortMessage>>();

  sortablejsOptions: SortablejsOptions;
  sortedAndFilteredBands: RavenCompositeBand[];

  ngOnInit() {
    this.sortablejsOptions = {
      animation: 0,
      delay: 0,
      ghostClass: 'sortable-placeholder',
      group: 'bands',
      onAdd: this.onSort.bind(this),
      onEnd: this.onSort.bind(this),
      onRemove: this.onSort.bind(this),
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
          .filter(band => band.containerId === this.containerId)
          .sort((a, b) => a.sortOrder - b.sortOrder);
    }
  }

  /**
   * trackBy for bands list.
   * Returns a custom id that is just the band id concatenated with all the subBand ids,
   * separated by a forward-slash.
   * This is so anytime subBands change (i.e. added/removed) we re-render the band.
   */
  bandsTrackByFn(index: number, item: RavenCompositeBand) {
    let id = item.id;

    for (let i = 0, l = item.subBands.length; i < l; ++i) {
      id += `/${item.subBands[i].id}`;
    }

    return id;
  }

  /**
   * Event. Called when a falcon band is clicked.
   */
  onBandClick(bandId: string) {
    this.bandClick.emit(bandId);
  }

  /**
   * Event. Called when a `falcon-composite-band-left-click` event is fired from a falcon band.
   */
  @HostListener('falcon-composite-band-left-click', ['$event'])
  onDataItemLeftClick(e: FalconCompositeBandLeftClickEvent) {
    e.preventDefault();
    e.stopPropagation();
    const { band, interval } = e.detail.ctlData;
    this.bandLeftClick.emit({ bandId: band.id, pointId: interval.uniqueId });
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

    for (let i = 0, l = this.sortedAndFilteredBands.length; i < l; ++i) {
      const band = this.sortedAndFilteredBands[i];

      sort[band.id] = {
        containerId: this.containerId,
        sortOrder: i,
      };
    }

    if (Object.keys(sort).length) {
      this.newSort.emit(sort);
    }
  }
}
