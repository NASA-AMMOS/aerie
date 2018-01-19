/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, ChangeDetectionStrategy } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';

import * as fromConfig from './../../reducers/config';
import * as fromLayout from './../../reducers/layout';
import * as fromTimeline from './../../reducers/timeline';

import * as timelineActions from './../../actions/timeline';

import {
  RavenBand,
  RavenTimeRange,
} from './../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-timeline',
  styleUrls: ['./timeline.component.css'],
  templateUrl: './timeline.component.html',
})
export class TimelineComponent {
  bands$: Observable<RavenBand[]>;
  itarMessage$: Observable<string>;
  labelWidth$: Observable<number>;
  maxTimeRange$: Observable<RavenTimeRange>;
  selectedBand$: Observable<RavenBand | null>;
  showDetailsDrawer$: Observable<boolean>;
  showLeftDrawer$: Observable<boolean>;
  showSouthBandsDrawer$: Observable<boolean>;
  viewTimeRange$: Observable<RavenTimeRange>;

  constructor(private store: Store<fromTimeline.TimelineState | fromConfig.ConfigState>) {
    this.bands$ = this.store.select(fromTimeline.getBands);
    this.itarMessage$ = this.store.select(fromConfig.getItarMessage);
    this.labelWidth$ = this.store.select(fromTimeline.getLabelWidth);
    this.maxTimeRange$ = this.store.select(fromTimeline.getMaxTimeRange);
    this.selectedBand$ = this.store.select(fromTimeline.getSelectedBand);
    this.showDetailsDrawer$ = this.store.select(fromLayout.getShowDetailsDrawer);
    this.showLeftDrawer$ = this.store.select(fromLayout.getShowLeftDrawer);
    this.showSouthBandsDrawer$ = this.store.select(fromLayout.getShowSouthBandsDrawer);
    this.viewTimeRange$ = this.store.select(fromTimeline.getViewTimeRange);
  }

  /**
   * Event. Called when a `falcon-timeline-band-click` event is fired from falcon-timeline.
   *
   * TODO: Replace 'any' with a concrete type.
   */
  onBandClick(e: any) {
    this.store.dispatch(new timelineActions.SelectBand(e.detail.band.id));
  }

  /**
   * Event. Called when a `falcon-settings-update-all-bands` event is fired from the falcon-settings-band.
   *
   * TODO: Replace 'any' with a concrete type.
   */
  onUpdateAllBands(e: any) {
    const { prop, value } = e.detail;
    this.store.dispatch(new timelineActions.SettingsUpdateAllBands(prop, value));
  }

  /**
   * Event. Called when a `falcon-settings-update-band` event is fired from the falcon-settings-band.
   *
   * TODO: Replace 'any' with a concrete type.
   */
  onUpdateBand(e: any) {
    const { prop, value } = e.detail;
    this.store.dispatch(new timelineActions.SettingsUpdateBand(prop, value));
  }

  /**
   * Event. Called when a `falcon-timeline-update-view-time-range` event is fired from the falcon-timeline.
   *
   * TODO: Replace 'any' with a concrete type.
   */
  onUpdateViewTimeRange(e: any) {
    this.store.dispatch(new timelineActions.UpdateViewTimeRange(e.detail));
  }

  /**
   * After a split pane drag, trigger a window resize event so the bands are properly sized.
   */
  onDragEnd() {
    window.dispatchEvent(new Event('resize'));
  }
}
