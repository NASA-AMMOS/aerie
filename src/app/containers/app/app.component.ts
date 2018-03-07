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
import { Subject } from 'rxjs/Subject';

import 'rxjs/add/observable/combineLatest';
import 'rxjs/add/operator/takeUntil';

import * as fromDisplay from './../../reducers/display';
import * as fromSourceExplorer from './../../reducers/source-explorer';

import * as layoutActions from './../../actions/layout';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  styleUrls: ['./app.component.css'],
  templateUrl: './app.component.html',
})
export class AppComponent implements OnDestroy {
  fetchGraphDataRequestPending$: Observable<boolean>;
  fetchInitialSourcesRequestPending$: Observable<boolean>;
  fetchSourcesRequestPending$: Observable<boolean>;
  stateLoadPending$: Observable<boolean>;

  loading: boolean;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(private changeDetector: ChangeDetectorRef, private store: Store<fromSourceExplorer.SourceExplorerState>) {
    this.fetchGraphDataRequestPending$ = this.store.select(fromSourceExplorer.getFetchGraphDataRequestPending).takeUntil(this.ngUnsubscribe);
    this.fetchInitialSourcesRequestPending$ = this.store.select(fromSourceExplorer.getFetchInitialSourcesRequestPending).takeUntil(this.ngUnsubscribe);
    this.fetchSourcesRequestPending$ = this.store.select(fromSourceExplorer.getFetchSourcesRequestPending).takeUntil(this.ngUnsubscribe);
    this.stateLoadPending$ = this.store.select(fromDisplay.getStateLoadPending).takeUntil(this.ngUnsubscribe);

    // Combine fetch pending observables for use in progress bar.
    Observable.combineLatest(
      this.fetchGraphDataRequestPending$,
      this.fetchInitialSourcesRequestPending$,
      this.fetchSourcesRequestPending$,
      this.stateLoadPending$,
      (fetchGraphData, fetchInitialSources, fetchSources, stateLoadPending) => {
        return fetchGraphData || fetchInitialSources || fetchSources || stateLoadPending;
      },
    ).takeUntil(this.ngUnsubscribe).subscribe(loading => {
      this.loading = loading;
      this.changeDetector.markForCheck();
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
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
