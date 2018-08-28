/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { HBCommandDictionary } from '../../../shared/models/hb-command-dictionary';
import { FetchCommandDictionaryList, SelectCommandDictionary } from '../../actions/command-dictionary';
import { HummingbirdAppState } from '../../hummingbird-store';

import * as fromCommandDictionary from '../../reducers/command-dictionary';

@Component({
  selector: 'hummingbird-app',
  styleUrls: ['./hummingbird-app.component.css'],
  templateUrl: './hummingbird-app.component.html',
})
export class HummingbirdAppComponent implements OnDestroy {

  /**
   * List of all available dictionaries to select from
   */
  dictionaries: Array<HBCommandDictionary>;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(private store: Store<HummingbirdAppState>) {
    this.store.select(fromCommandDictionary.getCommandDictionaryState).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(state => {
      this.dictionaries = state.list || [];
    });

    this.store.dispatch(new FetchCommandDictionaryList());
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * A dictionary from the list of dictionaries was selected
   * @param dictionary The dictionary which was selected
   */
  onSelectedDictionary(dictionary: HBCommandDictionary ) {
    this.store.dispatch(new SelectCommandDictionary(dictionary));
  }
}
