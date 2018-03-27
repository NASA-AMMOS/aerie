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
  OnDestroy,
} from '@angular/core';

import { Store } from '@ngrx/store';

import { Observable } from 'rxjs/Observable';
import { combineLatest } from 'rxjs/observable/combineLatest';
import { takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs/Subject';

import * as fromDisplay from './../../reducers/display';
import * as fromSourceExplorer from './../../reducers/source-explorer';
import * as fromTimeline from './../../reducers/timeline';

import * as layoutActions from './../../actions/layout';
import * as timelineActions from './../../actions/timeline';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  styleUrls: ['./app.component.css'],
  templateUrl: './app.component.html',
})
export class AppComponent implements OnDestroy {
  loading$: Observable<boolean>;

  resourceColor: string;
  labelWidth: number;
  tooltip: boolean;
  currentTimeCursor: boolean;
  labelFontStyle: string;
  labelFontSize: number;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<fromSourceExplorer.SourceExplorerState | fromTimeline.TimelineState>,
  ) {
    // Combine all fetch pending observables for use in progress bar.
    this.loading$ = combineLatest(
      this.store.select(fromDisplay.getPending),
      this.store.select(fromSourceExplorer.getPending),
      (displayPending, sourceExplorerPending) => {
        return displayPending || sourceExplorerPending;
      },
    ).pipe(
      tap(() => this.changeDetector.markForCheck()),
      takeUntil(this.ngUnsubscribe),
    );

    // Timeline state.
    this.store.select(fromTimeline.getTimelineState).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(state => {
      this.resourceColor = state.resourceColor;
      this.labelWidth = state.labelWidth;
      this.tooltip = state.tooltip;
      this.currentTimeCursor = state.currentTimeCursor;
      this.labelFontSize = state.labelFontSize;
      this.labelFontStyle = state.labelFontStyle;
      this.changeDetector.markForCheck();
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  toggleDetailsDrawer() {
    this.store.dispatch(new layoutActions.ToggleDetailsDrawer());
  }

  toggleLeftDrawer() {
    this.store.dispatch(new layoutActions.ToggleLeftDrawer());
  }

  toggleSouthBandsDrawer() {
    this.store.dispatch(new layoutActions.ToggleSouthBandsDrawer());
  }

  onChangeLabelWidth(labelWidth: number) {
    this.store.dispatch(new timelineActions.ChangeLabelWidth(labelWidth));
  }

  onChangeLabelFontSize(labelFontSize: number) {
    this.store.dispatch(new timelineActions.ChangeLabelFontSize(labelFontSize));
  }

  onChangeLabelFontStyle(labelFontStyle: string) {
    this.store.dispatch(new timelineActions.ChangeLabelFontStyle(labelFontStyle));
  }
  onChangeResourceColor(resourceColor: string) {
    this.store.dispatch(new timelineActions.ChangeResourceColor(resourceColor));
  }

  onChangeTooltip(tooltip: boolean) {
    this.store.dispatch(new timelineActions.ChangeTooltip(tooltip));
  }

  onChangeCurrentTimeCursor(currentTimeCursor: boolean) {
    this.store.dispatch(new timelineActions.ChangeCurrentTimeCursor(currentTimeCursor));
  }
}
