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
  HostListener,
  OnDestroy,
} from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';

import 'rxjs/add/operator/takeUntil';

import * as fromConfig from './../../reducers/config';
import * as fromLayout from './../../reducers/layout';
import * as fromTimeline from './../../reducers/timeline';

import * as timelineActions from './../../actions/timeline';

import {
  RavenBand,
  RavenSettingsUpdate,
  RavenSortMessage,
  RavenTimeRange,
  StringTMap,
} from './../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-timeline',
  styleUrls: ['./timeline.component.css'],
  templateUrl: './timeline.component.html',
})
export class TimelineComponent implements OnDestroy {
  bands: RavenBand[];
  itarMessage: string;
  labelWidth: number;
  maxTimeRange: RavenTimeRange;
  selectedBandId: string;
  viewTimeRange: RavenTimeRange;

  showDetailsDrawer$: Observable<boolean>;
  showLeftDrawer$: Observable<boolean>;
  showSouthBandsDrawer$: Observable<boolean>;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(private store: Store<fromTimeline.TimelineState | fromConfig.ConfigState>) {
    this.store.select(fromTimeline.getBands).takeUntil(this.ngUnsubscribe).subscribe(bands => this.bands = bands);
    this.store.select(fromConfig.getItarMessage).takeUntil(this.ngUnsubscribe).subscribe(itarMessage => this.itarMessage = itarMessage);
    this.store.select(fromTimeline.getLabelWidth).takeUntil(this.ngUnsubscribe).subscribe(labelWidth => this.labelWidth = labelWidth);
    this.store.select(fromTimeline.getMaxTimeRange).takeUntil(this.ngUnsubscribe).subscribe(maxTimeRange => this.maxTimeRange = maxTimeRange);
    this.store.select(fromTimeline.getSelectedBandId).takeUntil(this.ngUnsubscribe).subscribe(selectedBandId => this.selectedBandId = selectedBandId);
    this.store.select(fromTimeline.getViewTimeRange).takeUntil(this.ngUnsubscribe).subscribe(viewTimeRange => this.viewTimeRange = viewTimeRange);

    this.showDetailsDrawer$ = this.store.select(fromLayout.getShowDetailsDrawer).takeUntil(this.ngUnsubscribe);
    this.showLeftDrawer$ = this.store.select(fromLayout.getShowLeftDrawer).takeUntil(this.ngUnsubscribe);
    this.showSouthBandsDrawer$ = this.store.select(fromLayout.getShowSouthBandsDrawer).takeUntil(this.ngUnsubscribe);
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Call to update a setting in all bands.
   */
  updateAllBands(update: RavenSettingsUpdate): void {
    this.store.dispatch(new timelineActions.SettingsUpdateAllBands(update.prop, update.value));
  }

  /**
   * Event. Called when a band is clicked in an raven-bands component.
   */
  onBandClick(bandId: string): void {
    this.store.dispatch(new timelineActions.SelectBand(bandId));
  }

  /**
   * Event. Called when a `newSort` event is fired from raven-bands.
   */
  onSort(sort: StringTMap<RavenSortMessage>): void {
    this.store.dispatch(new timelineActions.SortBands(sort));
  }

  /**
   * Event. Called when an `update-band` event is fired from the raven-settings component.
   */
  onUpdateBand(update: RavenSettingsUpdate): void {
    this.store.dispatch(new timelineActions.SettingsUpdateBand(update.prop, update.value));
  }

  /**
   * Event. Called when a `falcon-update-view-time-range` event is fired from the falcon-timeline.
   * Using a HostListener here instead of a template event binding because multiple elements emit this event.
   *
   * TODO: Replace 'any' with a concrete type.
   */
  @HostListener('falcon-update-view-time-range', ['$event'])
  onUpdateViewTimeRange(e: any): void {
    e.preventDefault();
    e.stopPropagation();
    this.store.dispatch(new timelineActions.UpdateViewTimeRange(e.detail));
  }

  /**
   * Event. After a split pane drag, trigger a window resize event so the bands are properly sized.
   */
  onDragEnd(): void {
    dispatchEvent(new Event('resize'));
  }
}
