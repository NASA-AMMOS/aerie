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
import { SequenceFile } from '../../../../../../sequencing/src/models';
import { StringTMap } from '../../../shared/models';
import { FetchChildren } from '../../actions/file.actions';
import { getFiles, getRootFileChildIds } from '../../selectors';
import { SequencingAppState } from '../../sequencing-store';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'file-explorer',
  styleUrls: ['./file-explorer.component.css'],
  templateUrl: './file-explorer.component.html',
})
export class FileExplorerComponent {
  files$: Observable<StringTMap<SequenceFile>>;
  rootFileChildIds$: Observable<string[]>;

  constructor(private store: Store<SequencingAppState>) {
    this.files$ = this.store.pipe(select(getFiles));
    this.rootFileChildIds$ = this.store.pipe(select(getRootFileChildIds));
  }

  onExpandFolderEvent(file: SequenceFile): void {
    this.store.dispatch(
      new FetchChildren(file.id, { expanded: !file.expanded }),
    );
  }
}
