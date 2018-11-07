/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { HbCommand } from '../../../shared/models/hb-command';
import { HbCommandDictionary } from '../../../shared/models/hb-command-dictionary';

import * as configActions from '../../../shared/actions/config.actions';
import {
  FetchCommandDictionaryList,
  SelectCommandDictionary,
} from '../../actions/command-dictionary.actions';

import { HummingbirdAppState } from '../../hummingbird-store';
import { getCommands, getDictionaries, getSelected } from '../../selectors';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hummingbird-app',
  styleUrls: ['./hummingbird-app.component.css'],
  templateUrl: './hummingbird-app.component.html',
})
export class HummingbirdAppComponent {
  /**
   * List of all available dictionaries to select from
   */
  dictionaries$: Observable<HbCommandDictionary[]>;

  /**
   * List of all commands from the selected dictionary
   */
  commands$: Observable<HbCommand[] | null>;

  /**
   * Currently active dictionary
   */
  selectedDictionaryId$: Observable<string | null>;

  constructor(private store: Store<HummingbirdAppState>) {
    this.commands$ = this.store.pipe(select(getCommands));
    this.dictionaries$ = this.store.pipe(select(getDictionaries));
    this.selectedDictionaryId$ = this.store.pipe(select(getSelected));

    this.store.dispatch(new FetchCommandDictionaryList());
  }

  /**
   * A dictionary from the list of dictionaries was selected
   * @param dictionary The dictionary which was selected
   */
  onSelectedDictionary(selectedId: string) {
    this.store.dispatch(new SelectCommandDictionary(selectedId));
  }

  /**
   * A Command from the list of commands was selected
   * @todo Implement once we know what we want a command selection to do
   * @param command The command which was selected
   */
  onSelectedCommand(name: string) {
    // STUB
  }

  /**
   * The hamburger menu was clicked
   */
  onMenuClicked() {
    this.store.dispatch(new configActions.ToggleNavigationDrawer());
  }
}
