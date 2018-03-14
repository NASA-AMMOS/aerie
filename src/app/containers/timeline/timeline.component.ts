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
  ChangeDetectorRef,
  Component,
  HostListener,
  OnDestroy,
} from '@angular/core';

import { Store } from '@ngrx/store';

import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs/Subject';

import * as fromConfig from './../../reducers/config';
import * as fromLayout from './../../reducers/layout';
import * as fromTimeline from './../../reducers/timeline';

import * as layoutActions from './../../actions/layout';
import * as sourceExplorerActions from './../../actions/source-explorer';
import * as timelineActions from './../../actions/timeline';

import {
  RavenActivityPoint,
  RavenCompositeBand,
  RavenPoint,
  RavenSettingsUpdate,
  RavenSortMessage,
  RavenStatePoint,
  RavenSubBand,
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
  chartSize = 75;
  // Config state.
  itarMessage: string;

  // Layout state.
  showDetailsDrawer: boolean;
  showLeftDrawer: boolean;
  showSouthBandsDrawer: boolean;

  // Timeline state.
  bands: RavenCompositeBand[];
  labelWidth: number;
  maxTimeRange: RavenTimeRange;
  overlayMode: boolean;
  selectedBandId: string;
  viewTimeRange: RavenTimeRange;
  selectedDataPoint: RavenPoint;
  showDataPointDrawer: boolean;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<fromTimeline.TimelineState | fromConfig.ConfigState>,
  ) {
    // Config state.
    this.store.select(fromConfig.getItarMessage).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(itarMessage => {
      this.itarMessage = itarMessage;
      this.changeDetector.markForCheck();
    });

    // Layout state.
    this.store.select(fromLayout.getShowDrawers).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(state => {
      this.showDetailsDrawer = state.showDetailsDrawer;
      this.showLeftDrawer = state.showLeftDrawer;
      this.showSouthBandsDrawer = state.showSouthBandsDrawer;
      this.showDataPointDrawer = state.showDataPointDrawer;
      this.changeDetector.markForCheck();
      dispatchEvent(new Event('resize')); // Trigger a window resize to make sure bands properly resize anytime our layout changes.
    });

    // Timeline state.
    this.store.select(fromTimeline.getTimelineState).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(state => {
      this.bands = state.bands;
      this.labelWidth = state.labelWidth;
      this.maxTimeRange = state.maxTimeRange;
      this.overlayMode = state.overlayMode;
      this.selectedBandId = state.selectedBandId;
      this.viewTimeRange = state.viewTimeRange;
      this.selectedDataPoint = state.selectedDataPoint;
      this.changeDetector.markForCheck();
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  getChartSize(): number {
    console.log('in getChartSize');
    return this.showDataPointDrawer ? 60 : 75;
  }
  selectedDataPointIsActivity(): boolean {
    return (<RavenActivityPoint>this.selectedDataPoint).activityName !== undefined;
  }

  selectedDataPointIsState(): boolean {
    return (<RavenStatePoint>this.selectedDataPoint).end !== undefined;
  }

  /**
   * Event. Called when a band is clicked in an raven-bands component.
   */
  onBandClick(bandId: string): void {
    this.store.dispatch(new timelineActions.SelectBand(bandId));
  }

  onDataPointClick(detail: any): void {
    this.store.dispatch(new timelineActions.SelectDataPoint(detail.ctlData.interval, detail.ctlData.band.id));
    if (!this.showDataPointDrawer) {
      this.store.dispatch(new layoutActions.ToggleDataPointDrawer());
      this.chartSize = 60;
      dispatchEvent(new Event('resize'));
    }
  }
  /**
   * Event. Called when a `delete-band` event is fired from the raven-settings component.
   */
  onDeleteBand(subBand: RavenSubBand): void {
    this.store.dispatch(new sourceExplorerActions.RemoveBands(subBand.sourceId, [subBand.id]));
  }

  /**
   * Event. Called when a `new-sort` event is fired from raven-bands.
   */
  onSort(sort: StringTMap<RavenSortMessage>): void {
    this.store.dispatch(new timelineActions.SortBands(sort));
  }

  /**
   * Event. Called when an `update-band` event is fired from the raven-settings component.
   */
  onUpdateBand(e: RavenSettingsUpdate): void {
    this.store.dispatch(new timelineActions.UpdateBand(e.bandId, e.update));
  }

  /**
   * Event. Called when an `update-sub-band` event is fired from the raven-settings component.
   */
  onUpdateSubBand(e: RavenSettingsUpdate): void {
    this.store.dispatch(new timelineActions.UpdateSubBand(e.bandId, e.subBandId, e.update));
  }

  /**
   * Event. Called when an `update-timeline` event is fired from the raven-settings component.
   */
  onUpdateTimeline(e: RavenSettingsUpdate): void {
    this.store.dispatch(new timelineActions.UpdateTimeline(e.update));
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
