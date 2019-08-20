/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { SequenceFile } from '../../../models';

@Component({
  selector: 'nest-tree-folder',
  styleUrls: ['./nest-tree-folder.component.css'],
  templateUrl: './nest-tree-folder.component.html',
})
export class NestTreeFolderComponent {
  @Input()
  file: SequenceFile;

  @Output()
  expandFolderEvent: EventEmitter<SequenceFile> = new EventEmitter<
    SequenceFile
  >();
}
