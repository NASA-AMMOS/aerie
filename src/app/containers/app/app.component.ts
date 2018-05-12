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
} from '@angular/core';

import { Store } from '@ngrx/store';

import {
  combineLatest,
  Observable,
} from 'rxjs';

import {
  map,
  tap,
} from 'rxjs/operators';

import * as fromSourceExplorer from './../../reducers/source-explorer';
import * as fromTimeline from './../../reducers/timeline';

import * as layoutActions from './../../actions/layout';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  styleUrls: ['./app.component.css'],
  templateUrl: './app.component.html',
})
export class AppComponent {
  loading$: Observable<boolean>;

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<fromSourceExplorer.SourceExplorerState>,
  ) {
    // Combine all fetch pending observables for use in progress bar.
    this.loading$ = combineLatest(
      this.store.select(fromSourceExplorer.getPending),
      this.store.select(fromTimeline.getPending),
    ).pipe(
      map(loading => loading[0] || loading[1]),
      tap(() => this.markForCheck()),
    );
  }

  /**
   * Helper. Marks this component for change detection check,
   * and then detects changes on the next tick.
   *
   * TODO: Find out how we can remove this.
   */
  markForCheck() {
    this.changeDetector.markForCheck();
    setTimeout(() => this.changeDetector.detectChanges());
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
