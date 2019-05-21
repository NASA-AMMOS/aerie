/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ToggleNestNavigationDrawer } from '../../../shared/actions/config.actions';
import {
  CommandDictionary,
  MpsCommand,
  StringTMap,
} from '../../../shared/models';
import {
  FetchCommandDictionaries,
  SelectCommandDictionary,
} from '../../actions/command-dictionary.actions';
import { AddText, OpenEditorHelpDialog } from '../../actions/editor.actions';
import { getCommandTemplate } from '../../code-mirror-languages/mps/helpers';
import {
  getCommands,
  getCommandsByName,
  getDictionaries,
  getSelectedDictionaryId,
} from '../../selectors';
import { SequencingAppState } from '../../sequencing-store';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'sequencing-app',
  styleUrls: ['./sequencing-app.component.css'],
  templateUrl: './sequencing-app.component.html',
})
export class SequencingAppComponent implements OnDestroy {
  commands$: Observable<MpsCommand[] | null>;
  commandsByName$: Observable<StringTMap<MpsCommand> | null>;
  dictionaries$: Observable<CommandDictionary[]>;
  selectedDictionaryId$: Observable<string | null>;

  commandsByName: StringTMap<MpsCommand>;
  commandFilterQuery = '';

  // TODO: Move to reducer when implementing user config for app
  panels = {
    leftPanel: { size: 20, visible: true },
    middlePanel: { size: 60, visible: true },
    rightPanel: { size: 20, visible: true },
  };

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(private store: Store<SequencingAppState>) {
    this.commands$ = this.store.pipe(select(getCommands));
    this.commandsByName$ = this.store.pipe(select(getCommandsByName));
    this.dictionaries$ = this.store.pipe(select(getDictionaries));
    this.selectedDictionaryId$ = this.store.pipe(
      select(getSelectedDictionaryId),
    );

    this.commandsByName$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((commandsByName: StringTMap<MpsCommand>) => {
        this.commandsByName = commandsByName;
      });

    this.store.dispatch(new FetchCommandDictionaries());
  }

  ngOnDestroy(): void {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  onMenuClicked(): void {
    this.store.dispatch(new ToggleNestNavigationDrawer());
  }

  onOpenEditorHelpDialog(): void {
    this.store.dispatch(new OpenEditorHelpDialog());
  }

  onSelectCommand(commandName: string): void {
    const commandTemplate = getCommandTemplate(
      commandName,
      this.commandsByName,
    );
    this.store.dispatch(new AddText(commandTemplate));
  }

  onSelectDictionary(selectedId: string): void {
    this.store.dispatch(new SelectCommandDictionary(selectedId));
  }

  dragEnd(event: { gutterNum: number; sizes: Array<number> }) {
    const { sizes } = event;
    this.panels.leftPanel.size = sizes[0];
    this.panels.middlePanel.size = sizes[1];
    this.panels.rightPanel.size = sizes[2];
  }

  togglePanelView(type: string) {
    switch (type) {
      case 'leftPanel':
        this.panels.leftPanel.visible = !this.panels.leftPanel.visible;
        break;
      case 'rightPanel':
        this.panels.rightPanel.visible = !this.panels.rightPanel.visible;
        break;
      default:
        for (const panel in this.panels) {
          this.panels[panel].visible = true;
        }
    }
  }
}
