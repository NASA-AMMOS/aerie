/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  Input,
} from '@angular/core';
import { select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { MpsCommand, StringTMap } from '../../../shared/models';
import { EditorActions, FileActions } from '../../actions';
import { FalconAppState } from '../../falcon-store';
import { CurrentLine, Editor, EditorOptions, SequenceTab } from '../../models';
import {
  CommandDictionarySelectors,
  EditorSelectors,
  FileSelectors,
} from '../../selectors';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'editor-workspace',
  styleUrls: ['./editor-workspace.component.css'],
  templateUrl: './editor-workspace.component.html',
})
export class EditorWorkspaceComponent implements AfterViewInit {
  @Input()
  editor: Editor;

  @Input()
  editorOptions: EditorOptions;

  activeEditor$: Observable<string>;
  commands$: Observable<MpsCommand[] | null> = this.store.pipe(
    select(CommandDictionarySelectors.getCommands),
  );
  commandsByName$: Observable<StringTMap<MpsCommand> | null> = this.store.pipe(
    select(CommandDictionarySelectors.getCommandsByName),
  );
  currentTab$: Observable<string | null>;
  currentLine$: Observable<CurrentLine | null>;
  file$: Observable<SequenceTab | null>;
  openedTabs$: Observable<SequenceTab[] | null>;

  constructor(private store: Store<FalconAppState>) {}

  ngAfterViewInit() {
    this.activeEditor$ = this.store.pipe(select(FileSelectors.getActiveEditor));
    this.currentTab$ = this.store.pipe(
      select(FileSelectors.getCurrentTab, { editorId: this.editor.id }),
    );
    this.currentLine$ = this.store.pipe(select(EditorSelectors.getCurrentLine));
    this.file$ = this.store.pipe(
      select(FileSelectors.getCurrentFile, { editorId: this.editor.id }),
    );
    this.openedTabs$ = this.store.pipe(
      select(FileSelectors.getOpenedTabs, { editorId: this.editor.id }),
    );
  }

  onCloseTab({ tabId, editorId }: { tabId: string; editorId: string }) {
    this.store.dispatch(
      FileActions.closeTab({ editorId, docIdToClose: tabId }),
    );
  }

  onCreateTab(editorId: string) {
    this.store.dispatch(FileActions.createTab({ editorId }));
  }

  onSwitchTab({ tabId, editorId }: { tabId: string; editorId: string }) {
    this.store.dispatch(FileActions.switchTab({ editorId, switchToId: tabId }));
  }

  onUpdateTab({
    id,
    text,
    editorId,
  }: {
    id: string;
    text: string;
    editorId: string;
  }) {
    this.store.dispatch(
      FileActions.updateTab({ editorId, docIdToUpdate: id, text }),
    );
  }

  onSetCurrentLine(currentLine: CurrentLine) {
    this.store.dispatch(EditorActions.setCurrentLine({ currentLine }));
  }

  getFile() {
    if (this.editor && this.editor.openedTabs && this.editor.currentTab) {
      return this.editor.openedTabs[this.editor.currentTab];
    }
    return null;
  }
}
