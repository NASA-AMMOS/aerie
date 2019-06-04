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
import { OpenEditorHelpDialog } from '../../actions/editor.actions';
import {
  CloseTab,
  CreateTab,
  SwitchTab,
  UpdateTab,
} from '../../actions/file.actions';
import { SequenceTab } from '../../models';
import { Editor } from '../../reducers/file.reducer';
import {
  getCommands,
  getCommandsByName,
  getCurrentFile,
  getCurrentTab,
  getOpenedTabs,
} from '../../selectors';
import { SequencingAppState } from '../../sequencing-store';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'sequencing-workspace',
  styleUrls: ['./sequencing-workspace.component.css'],
  templateUrl: './sequencing-workspace.component.html',
})
export class SequencingWorkspaceComponent implements AfterViewInit {
  @Input()
  editor: Editor;

  commands$: Observable<MpsCommand[] | null>;
  commandsByName$: Observable<StringTMap<MpsCommand> | null>;
  currentTab$: Observable<string | null>;
  file$: Observable<SequenceTab | null>;
  openedTabs$: Observable<SequenceTab[]>;

  constructor(private store: Store<SequencingAppState>) {
    this.commands$ = this.store.pipe(select(getCommands));
    this.commandsByName$ = this.store.pipe(select(getCommandsByName));
  }

  ngAfterViewInit() {
    this.currentTab$ = this.store.pipe(
      select(getCurrentTab, { editorId: this.editor.id }),
    );
    this.file$ = this.store.pipe(
      select(getCurrentFile, { editorId: this.editor.id }),
    );
    this.openedTabs$ = this.store.pipe(
      select(getOpenedTabs, { editorId: this.editor.id }),
    );
  }

  onCloseTab(id: string, editorId: string) {
    this.store.dispatch(new CloseTab(id, editorId));
  }

  onCreateTab(editorId: string) {
    this.store.dispatch(new CreateTab(editorId));
  }

  onOpenEditorHelpDialog(): void {
    this.store.dispatch(new OpenEditorHelpDialog());
  }

  onSwitchTab(switchToId: string, editorId: string) {
    this.store.dispatch(new SwitchTab(switchToId, editorId));
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
    this.store.dispatch(new UpdateTab(id, text, editorId));
  }
}
