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
  selectedBand$: Observable<RavenBand | null>;
  showDetailsDrawer$: Observable<boolean>;
  showLeftDrawer$: Observable<boolean>;
  showSouthBandsDrawer$: Observable<boolean>;

  constructor(private store: Store<fromTimeline.TimelineState | fromConfig.ConfigState>) {
    this.bands$ = this.store.select(fromTimeline.getBands);
    this.itarMessage$ = this.store.select(fromConfig.getItarMessage);
    this.labelWidth$ = this.store.select(fromTimeline.getLabelWidth);
    this.selectedBand$ = this.store.select(fromTimeline.getSelectedBand);
    this.showDetailsDrawer$ = this.store.select(fromLayout.getShowDetailsDrawer);
    this.showLeftDrawer$ = this.store.select(fromLayout.getShowLeftDrawer);
    this.showSouthBandsDrawer$ = this.store.select(fromLayout.getShowSouthBandsDrawer);
  }

  /**
   * Event. Called when a `falcon-timeline-band-click` event is fired from falcon-timeline.
   */
  onBandClick(e: any) {
    this.store.dispatch(new timelineActions.SelectBand(e.detail.band.id));
  }

  /**
   * Event. Called when a `falcon-settings-update-all-bands` event is fired from the falcon-settings-band.
   */
  onUpdateAllBands(e: any) {
    const { prop, value } = e.detail;
    this.store.dispatch(new timelineActions.SettingsUpdateAllBands(prop, value));
  }

  /**
   * Event. Called when a `falcon-settings-update-band` event is fired from the falcon-settings-band.
   */
  onUpdateBand(e: any) {
    const { prop, value } = e.detail;
    this.store.dispatch(new timelineActions.SettingsUpdateBand(prop, value));
  }

  /**
   * After a split pane drag, trigger a window resize event so the bands are properly sized.
   */
  onDragEnd() {
    window.dispatchEvent(new Event('resize'));
  }
}
