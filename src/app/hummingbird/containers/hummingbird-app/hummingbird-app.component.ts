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
import { ToggleNavigationDrawer } from '../../../shared/actions/config.actions';
import { SetLine, SetText } from '../../actions/editor.actions';
import { HummingbirdAppState } from '../../hummingbird-store';

import {
  FetchCommandDictionaryList,
  SelectCommand,
  SelectCommandDictionary,
} from '../../actions/command-dictionary.actions';

import {
  getCommands,
  getCommandsByName,
  getDictionaries,
  getSelected,
  getText,
} from '../../selectors';

import {
  HbCommand,
  HbCommandDictionary,
  StringTMap,
} from '../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hummingbird-app',
  styleUrls: ['./hummingbird-app.component.css'],
  templateUrl: './hummingbird-app.component.html',
})
export class HummingbirdAppComponent {
  commands$: Observable<HbCommand[] | null>;
  commandsByName$: Observable<StringTMap<HbCommand> | null>;
  dictionaries$: Observable<HbCommandDictionary[]>;
  selectedDictionaryId$: Observable<string | null>;
  text$: Observable<string>;

  constructor(private store: Store<HummingbirdAppState>) {
    this.commands$ = this.store.pipe(select(getCommands));
    this.commandsByName$ = this.store.pipe(select(getCommandsByName));
    this.dictionaries$ = this.store.pipe(select(getDictionaries));
    this.selectedDictionaryId$ = this.store.pipe(select(getSelected));
    this.text$ = this.store.pipe(select(getText));

    this.store.dispatch(new FetchCommandDictionaryList());
  }

  /**
   * Called when the raven-seq-editor cursor value changes
   */
  onCursorLineChanged(line: number): void {
    this.store.dispatch(new SetLine(line));
  }

  /**
   * The hamburger menu was clicked
   */
  onMenuClicked(): void {
    this.store.dispatch(new ToggleNavigationDrawer());
  }

  /**
   * A Command from the list of commands was selected.
   */
  onSelectedCommand(command: string): void {
    this.store.dispatch(new SelectCommand(command));
  }

  /**
   * A dictionary from the list of dictionaries was selected
   * @param dictionary The dictionary which was selected
   */
  onSelectedDictionary(selectedId: string): void {
    this.store.dispatch(new SelectCommandDictionary(selectedId));
  }

  /**
   * Called with the raven-seq-editor value changes.
   */
  onValueChanged(value: string): void {
    this.store.dispatch(new SetText(value));
  }
}
