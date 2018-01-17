/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';

import * as fromLayout from './../../reducers/layout';
import * as layoutActions from './../../actions/layout';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  styleUrls: ['./app.component.css'],
  templateUrl: './app.component.html',
})
export class AppComponent {
  loading$: Observable<boolean>;

  constructor(private store: Store<fromLayout.LayoutState>) {
    this.loading$ = this.store.select(fromLayout.getLoading);
  }

  toggleDetailsDrawer() {
    this.store.dispatch(new layoutActions.ToggleDetailsDrawer());
  }

  toggleLeftDrawer() {
    this.store.dispatch(new layoutActions.ToggleLeftDrawer());
    window.dispatchEvent(new Event('resize')); // Trigger a window resize to make sure bands properly resize.
  }

  toggleSouthBandsDrawer() {
    this.store.dispatch(new layoutActions.ToggleSouthBandsDrawer());
  }
}
