/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  MatButtonModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatMenuModule,
  MatSidenavModule,
  MatTabsModule,
  MatToolbarModule,
  MatTooltipModule,
} from '@angular/material';
import { AngularSplitModule } from 'angular-split';
import { NestAppHeaderModule } from '../../../shared/components';
import { SeqCommandListModule, SeqCommandLoaderModule } from '../../components';
import { SequencingWorkspaceModule } from '../sequencing-workspace/sequencing-workspace.module';
import { SequencingAppComponent } from './sequencing-app.component';

@NgModule({
  declarations: [SequencingAppComponent],
  exports: [SequencingAppComponent],
  imports: [
    AngularSplitModule.forChild(),
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatSidenavModule,
    MatTabsModule,
    MatToolbarModule,
    MatTooltipModule,
    NestAppHeaderModule,
    SequencingWorkspaceModule,
    SeqCommandListModule,
    SeqCommandLoaderModule,
  ],
})
export class SequencingAppModule {}
