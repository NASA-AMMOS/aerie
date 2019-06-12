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
import { AddEditor, SetActiveEditor } from '../../actions/file.actions';
import {
  SetPanelSizes,
  ToggleEditorPanelsDirection,
  ToggleLeftPanelVisible,
  ToggleRightPanelVisible,
} from '../../actions/layout.actions';
import { getCommandTemplate } from '../../code-mirror-languages/mps/helpers';
import { Editor } from '../../models';
import {
  getActiveEditor,
  getCommands,
  getCommandsByName,
  getDictionaries,
  getEditorPanelsDirection,
  getEditorsList,
  getLeftPanelSize,
  getLeftPanelVisible,
  getMiddlePanelSize,
  getMiddlePanelVisible,
  getRightPanelSize,
  getRightPanelVisible,
  getSelectedDictionaryId,
  getShowLoadingBar,
} from '../../selectors';
import { SequencingAppState } from '../../sequencing-store';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'sequencing-app',
  styleUrls: ['./sequencing-app.component.css'],
  templateUrl: './sequencing-app.component.html',
})
export class SequencingAppComponent implements OnDestroy {
  activeEditor$: Observable<string>;
  commands$: Observable<MpsCommand[] | null>;
  commandsByName$: Observable<StringTMap<MpsCommand> | null>;
  dictionaries$: Observable<CommandDictionary[]>;
  leftPanelSize$: Observable<number>;
  leftPanelVisible$: Observable<boolean>;
  middlePanelSize$: Observable<number>;
  middlePanelVisible$: Observable<boolean>;
  rightPanelSize$: Observable<number>;
  rightPanelVisible$: Observable<boolean>;
  selectedDictionaryId$: Observable<string | null>;
  editorsList$: Observable<Editor[]>;
  editorPanelsDirection$: Observable<string>;
  showLoadingBar$: Observable<boolean>;

  commandsByName: StringTMap<MpsCommand>;
  commandFilterQuery = '';
  editorOptions = {
    autocomplete: true,
    darkTheme: true,
    showTooltips: false,
  };

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(private store: Store<SequencingAppState>) {
    this.activeEditor$ = this.store.pipe(select(getActiveEditor));
    this.commands$ = this.store.pipe(select(getCommands));
    this.commandsByName$ = this.store.pipe(select(getCommandsByName));
    this.dictionaries$ = this.store.pipe(select(getDictionaries));
    this.leftPanelSize$ = this.store.pipe(select(getLeftPanelSize));
    this.leftPanelVisible$ = this.store.pipe(select(getLeftPanelVisible));
    this.middlePanelSize$ = this.store.pipe(select(getMiddlePanelSize));
    this.middlePanelVisible$ = this.store.pipe(select(getMiddlePanelVisible));
    this.rightPanelSize$ = this.store.pipe(select(getRightPanelSize));
    this.rightPanelVisible$ = this.store.pipe(select(getRightPanelVisible));
    this.selectedDictionaryId$ = this.store.pipe(
      select(getSelectedDictionaryId),
    );
    this.editorsList$ = this.store.pipe(select(getEditorsList));
    this.editorPanelsDirection$ = this.store.pipe(
      select(getEditorPanelsDirection),
    );
    this.showLoadingBar$ = this.store.pipe(select(getShowLoadingBar));

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

  dragEnd(event: { gutterNum: number; sizes: Array<number> }) {
    this.store.dispatch(new SetPanelSizes(event.sizes));
  }

  onMenuClicked(): void {
    this.store.dispatch(new ToggleNestNavigationDrawer());
  }

  onSelectCommand({
    commandName,
    editorId,
  }: {
    commandName: string;
    editorId: string;
  }): void {
    const commandTemplate = getCommandTemplate(
      commandName,
      this.commandsByName,
    );
    this.store.dispatch(new AddText(commandTemplate, editorId));
  }

  onSelectDictionary(selectedId: string): void {
    this.store.dispatch(new SelectCommandDictionary(selectedId));
  }

  toggleLeftPanelVisible() {
    this.store.dispatch(new ToggleLeftPanelVisible());
  }

  toggleRightPanelVisible() {
    this.store.dispatch(new ToggleRightPanelVisible());
  }

  editorTrackByFn(_: number, item: any): string {
    return item.id;
  }

  addEditor() {
    this.store.dispatch(new AddEditor());
  }

  toggleEditorOption(key: string) {
    this.editorOptions = {
      ...this.editorOptions,
      [key]: !this.editorOptions[key],
    };
  }

  toggleEditorPanelsDirection() {
    this.store.dispatch(new ToggleEditorPanelsDirection());
  }

  onOpenEditorHelpDialog(): void {
    this.store.dispatch(new OpenEditorHelpDialog());
  }

  onSetActiveEditor(editorId: string) {
    this.store.dispatch(new SetActiveEditor(editorId));
  }
}
