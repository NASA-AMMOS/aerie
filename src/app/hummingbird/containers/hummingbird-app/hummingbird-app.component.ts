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

import { select, Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { HBCommand } from '../../../shared/models/hb-command';
import { HBCommandDictionary } from '../../../shared/models/hb-command-dictionary';
import { HummingbirdAppState } from '../../hummingbird-store';

import {
  FetchCommandDictionaryList,
  SelectCommandDictionary,
} from '../../actions/command-dictionary.actions';

import * as fromCommandDictionary from '../../reducers/command-dictionary.reducer';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'hummingbird-app',
  styleUrls: ['./hummingbird-app.component.css'],
  templateUrl: './hummingbird-app.component.html',
})
export class HummingbirdAppComponent implements OnDestroy {
  /**
   * List of all available dictionaries to select from
   */
  dictionaries: HBCommandDictionary[];

  /**
   * List of all commands from the selected dictionary
   */
  commands: HBCommand[] | null = [];

  /**
   * Currently active dictionary
   */
  selectedDictionaryId: string | null;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<HummingbirdAppState>
  ) {
    this.store
      .pipe(
        select(fromCommandDictionary.getDictionaries),
        takeUntil(this.ngUnsubscribe)
      )
      .subscribe(dictionaries => {
        this.dictionaries = dictionaries;
        this.markForCheck();
      });

    this.store
      .pipe(
        select(fromCommandDictionary.getCommands),
        takeUntil(this.ngUnsubscribe)
      )
      .subscribe(commands => {
        this.commands = commands;
        this.markForCheck();
      });

    this.store
      .pipe(
        select(fromCommandDictionary.getSelected),
        takeUntil(this.ngUnsubscribe)
      )
      .subscribe(selected => {
        this.selectedDictionaryId = selected || null;
        this.markForCheck();
      });

    this.store.dispatch(new FetchCommandDictionaryList());
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Helper. Marks this component for change detection check,
   * and then detects changes on the next tick.
   *
   * @todo Find out how we can remove this.
   */
  markForCheck() {
    this.changeDetector.markForCheck();
    setTimeout(() => {
      if (!this.changeDetector['destroyed']) {
        this.changeDetector.detectChanges();
      }
    });
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
}
