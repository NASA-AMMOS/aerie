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
import { Observable, Subscription } from 'rxjs';
import { ConfigActions } from '../../../shared/actions';
import { StringTMap } from '../../../shared/models';
import {
  CommandDictionaryActions,
  EditorActions,
  FileActions,
  LayoutActions,
} from '../../actions';
import { getCommandTemplate } from '../../code-mirror-languages/mps/helpers';
import { FalconAppState } from '../../falcon-store';
import { CurrentLine, Editor } from '../../models';
import {
  CommandDictionarySelectors,
  EditorSelectors,
  FileSelectors,
  LayoutSelectors,
} from '../../selectors';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'falcon-app',
  styleUrls: ['./falcon-app.component.css'],
  templateUrl: './falcon-app.component.html',
})
export class FalconAppComponent implements OnDestroy {
  activeEditor$: Observable<string> = this.store.pipe(
    select(FileSelectors.getActiveEditor),
  );
  commands$: Observable<any[] | null> = this.store.pipe(
    select(CommandDictionarySelectors.getCommands),
  );
  commandsByName$: Observable<StringTMap<any> | null> = this.store.pipe(
    select(CommandDictionarySelectors.getCommandsByName),
  );
  currentLine$: Observable<CurrentLine | null> = this.store.pipe(
    select(EditorSelectors.getCurrentLine),
  );
  dictionaries$: Observable<any[]> = this.store.pipe(
    select(CommandDictionarySelectors.getDictionaries),
  );
  hasFiles$: Observable<boolean> = this.store.pipe(
    select(FileSelectors.hasFiles),
  );
  leftPanelSize$: Observable<number> = this.store.pipe(
    select(LayoutSelectors.getLeftPanelSize),
  );
  leftPanelVisible$: Observable<boolean> = this.store.pipe(
    select(LayoutSelectors.getLeftPanelVisible),
  );
  middlePanelSize$: Observable<number> = this.store.pipe(
    select(LayoutSelectors.getMiddlePanelSize),
  );
  middlePanelVisible$: Observable<boolean> = this.store.pipe(
    select(LayoutSelectors.getMiddlePanelVisible),
  );
  rightPanelSize$: Observable<number> = this.store.pipe(
    select(LayoutSelectors.getRightPanelSize),
  );
  rightPanelVisible$: Observable<boolean> = this.store.pipe(
    select(LayoutSelectors.getRightPanelVisible),
  );
  selectedDictionaryId$: Observable<string | null> = this.store.pipe(
    select(CommandDictionarySelectors.getSelectedDictionaryId),
  );
  editorsList$: Observable<Editor[]> = this.store.pipe(
    select(FileSelectors.getEditorsList),
  );
  editorPanelsDirection$: Observable<string> = this.store.pipe(
    select(LayoutSelectors.getEditorPanelsDirection),
  );
  showLoadingBar$: Observable<boolean> = this.store.pipe(
    select(LayoutSelectors.getShowLoadingBar),
  );

  commandsByName: StringTMap<any>;
  commandFilterQuery = '';
  editorOptions = {
    autocomplete: true,
    darkTheme: true,
    showTooltips: false,
  };

  private subscriptions = new Subscription();

  constructor(private store: Store<FalconAppState>) {
    this.subscriptions.add(
      this.commandsByName$.subscribe((commandsByName: StringTMap<any>) => {
        this.commandsByName = commandsByName;
      }),
    );

    this.store.dispatch(FileActions.fetchChildren({ parentId: 'root' }));
    this.store.dispatch(CommandDictionaryActions.fetchCommandDictionaries());
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  dragEnd(event: { gutterNum: number; sizes: Array<number> }) {
    this.store.dispatch(LayoutActions.setPanelSizes({ sizes: event.sizes }));
  }

  onMenuClicked(): void {
    this.store.dispatch(ConfigActions.toggleNestNavigationDrawer());
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
    this.store.dispatch(
      EditorActions.addText({ editorId, text: commandTemplate }),
    );
  }

  onSelectDictionary(selectedId: string): void {
    this.store.dispatch(
      CommandDictionaryActions.selectCommandDictionary({ selectedId }),
    );
  }

  toggleLeftPanelVisible() {
    this.store.dispatch(LayoutActions.toggleLeftPanelVisible());
  }

  toggleRightPanelVisible() {
    this.store.dispatch(LayoutActions.toggleRightPanelVisible());
  }

  editorTrackByFn(_: number, item: any): string {
    return item.id;
  }

  addEditor() {
    this.store.dispatch(FileActions.addEditor());
  }

  toggleEditorOption(key: string) {
    this.editorOptions = {
      ...this.editorOptions,
      [key]: !this.editorOptions[key],
    };
  }

  toggleEditorPanelsDirection() {
    this.store.dispatch(LayoutActions.toggleEditorPanelsDirection());
  }

  onOpenEditorHelpDialog(): void {
    this.store.dispatch(EditorActions.openEditorHelpDialog());
  }

  onSetActiveEditor(editorId: string) {
    this.store.dispatch(FileActions.setActiveEditor({ editorId }));
  }

  onSetCurrentLine(currentLine: CurrentLine) {
    this.store.dispatch(EditorActions.setCurrentLine({ currentLine }));
  }
}
