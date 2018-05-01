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
  OnDestroy,
} from '@angular/core';

import { Store } from '@ngrx/store';

import { Observable } from 'rxjs/Observable';
import { combineLatest } from 'rxjs/observable/combineLatest';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs/Subject';

import * as fromSourceExplorer from './../../reducers/source-explorer';
import * as fromTimeline from './../../reducers/timeline';

import * as layoutActions from './../../actions/layout';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  styleUrls: ['./app.component.css'],
  templateUrl: './app.component.html',
})
export class AppComponent implements OnDestroy {
  loading$: Observable<boolean>;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private store: Store<fromSourceExplorer.SourceExplorerState>,
  ) {
    // Combine all fetch pending observables for use in progress bar.
    this.loading$ = combineLatest(
      this.store.select(fromSourceExplorer.getPending),
      this.store.select(fromTimeline.getPending),
      (sourceExplorerPending, timelinePending) => sourceExplorerPending || timelinePending,
    ).pipe(
      takeUntil(this.ngUnsubscribe),
    );
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

  toggleRightDrawer() {
    this.store.dispatch(new layoutActions.ToggleRightDrawer());
  }

  toggleSouthBandsDrawer() {
    this.store.dispatch(new layoutActions.ToggleSouthBandsDrawer());
  }
}
