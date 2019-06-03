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
import { MpsCommand, StringTMap } from '../../../shared/models';
import { OpenEditorHelpDialog } from '../../actions/editor.actions';
import {
  CloseTab,
  CreateTab,
  SwitchTab,
  UpdateTab,
} from '../../actions/file.actions';
import { SequenceTab } from '../../models';
import {
  getCommands,
  getCommandsByName,
  getCurrentFile,
  getCurrentTab,
  getOpenedTabs,
  getOpenedTabsByName,
} from '../../selectors';
import { SequencingAppState } from '../../sequencing-store';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'sequencing-workspace',
  styleUrls: ['./sequencing-workspace.component.css'],
  templateUrl: './sequencing-workspace.component.html',
})
export class SequencingWorkspaceComponent {
  commands$: Observable<MpsCommand[] | null>;
  commandsByName$: Observable<StringTMap<MpsCommand> | null>;
  currentTab$: Observable<string | null>;
  file$: Observable<SequenceTab | null>;
  openedTabs$: Observable<SequenceTab[]>;
  openedTabsByName$: Observable<StringTMap<SequenceTab> | null>;

  constructor(private store: Store<SequencingAppState>) {
    this.commands$ = this.store.pipe(select(getCommands));
    this.commandsByName$ = this.store.pipe(select(getCommandsByName));
    this.currentTab$ = this.store.pipe(select(getCurrentTab));
    this.file$ = this.store.pipe(select(getCurrentFile));
    this.openedTabs$ = this.store.pipe(select(getOpenedTabs));
    this.openedTabsByName$ = this.store.pipe(select(getOpenedTabsByName));
  }

  onCloseTab(id: string) {
    this.store.dispatch(new CloseTab(id));
  }

  onCreateTab() {
    this.store.dispatch(new CreateTab());
  }

  onOpenEditorHelpDialog(): void {
    this.store.dispatch(new OpenEditorHelpDialog());
  }

  onSwitchTab(switchToId: string) {
    this.store.dispatch(new SwitchTab(switchToId));
  }

  onUpdateTab({ id, text }: { id: string; text: string }) {
    this.store.dispatch(new UpdateTab(id, text));
  }
}
