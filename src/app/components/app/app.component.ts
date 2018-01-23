/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectorRef, ChangeDetectionStrategy, Component } from '@angular/core';
import { AfterViewChecked } from '@angular/core/src/metadata/lifecycle_hooks';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/combineLatest';

import * as fromSourceExplorer from './../../reducers/source-explorer';

import * as layoutActions from './../../actions/layout';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  styleUrls: ['./app.component.css'],
  templateUrl: './app.component.html',
})
export class AppComponent implements AfterViewChecked {
  fetchGraphDataRequestPending$: Observable<boolean>;
  fetchInitialSourcesRequestPending$: Observable<boolean>;
  fetchSourcesRequestPending$: Observable<boolean>;
  loading$: Observable<boolean>;

  constructor(private changeDetector: ChangeDetectorRef, private store: Store<fromSourceExplorer.SourceExplorerState>) {
    this.fetchGraphDataRequestPending$ = this.store.select(fromSourceExplorer.getFetchGraphDataRequestPending);
    this.fetchInitialSourcesRequestPending$ = this.store.select(fromSourceExplorer.getFetchInitialSourcesRequestPending);
    this.fetchSourcesRequestPending$ = this.store.select(fromSourceExplorer.getFetchSourcesRequestPending);

    // Combine fetch pending observables for use in progress bar.
    this.loading$ = Observable.combineLatest(
      this.fetchGraphDataRequestPending$,
      this.fetchInitialSourcesRequestPending$,
      this.fetchSourcesRequestPending$,
      (fetchGraphData, fetchInitialSources, fetchSources) => fetchGraphData || fetchInitialSources || fetchSources,
    );
  }

  /**
   * This is necessary because of issue: https://github.com/angular/angular/issues/17572.
   */
  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  toggleDetailsDrawer() {
    this.store.dispatch(new layoutActions.ToggleDetailsDrawer());
    dispatchEvent(new Event('resize')); // Trigger a window resize to make sure bands properly resize.
  }

  toggleLeftDrawer() {
    this.store.dispatch(new layoutActions.ToggleLeftDrawer());
    dispatchEvent(new Event('resize')); // Trigger a window resize to make sure bands properly resize.
  }

  toggleSouthBandsDrawer() {
    this.store.dispatch(new layoutActions.ToggleSouthBandsDrawer());
    dispatchEvent(new Event('resize')); // Trigger a window resize to make sure bands properly resize.
  }
}
