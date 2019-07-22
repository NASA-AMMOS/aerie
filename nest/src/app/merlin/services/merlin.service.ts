/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { ConfigActions } from '../../shared/actions';
import { MerlinAppState } from '../merlin-store';
import { getShowLoadingBar } from '../selectors';

/**
 * This is a set of commonly used selectors and functions used
 * across different containers in the merlin module.
 */
@Injectable()
export class MerlinService {
  showLoadingBar$: Observable<boolean>;

  constructor(private store: Store<MerlinAppState>) {
    this.showLoadingBar$ = this.store.pipe(select(getShowLoadingBar));
  }

  onToggleNestNavigationDrawer() {
    this.store.dispatch(ConfigActions.toggleNestNavigationDrawer());
  }
}
