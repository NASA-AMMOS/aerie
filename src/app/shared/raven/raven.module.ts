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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SortablejsModule } from 'angular-sortablejs';
import { FalconModule } from './../falcon';
import { MaterialModule } from './../material';

import {
  RavenActivityPointComponent,
  RavenBandsComponent,
  RavenConfirmDialogComponent,
  RavenCustomMetadataComponent,
  RavenEpochsComponent,
  RavenFileImportDialogComponent,
  RavenFileMetadataComponent,
  RavenLayoutApplyDialogComponent,
  RavenResourcePointComponent,
  RavenSettingsBandsComponent,
  RavenSettingsGlobalComponent,
  RavenStatePointComponent,
  RavenStateSaveDialogComponent,
  RavenTreeComponent,
} from './components';

import {
  DhmsPipe,
  KeyByPipe,
  TimestampPipe,
  ToKeyValueArrayPipe,
} from './pipes';

export const DECLARATIONS = [
  DhmsPipe,
  KeyByPipe,
  TimestampPipe,
  ToKeyValueArrayPipe,
  RavenActivityPointComponent,
  RavenBandsComponent,
  RavenConfirmDialogComponent,
  RavenCustomMetadataComponent,
  RavenEpochsComponent,
  RavenFileImportDialogComponent,
  RavenFileMetadataComponent,
  RavenLayoutApplyDialogComponent,
  RavenResourcePointComponent,
  RavenSettingsBandsComponent,
  RavenSettingsGlobalComponent,
  RavenStatePointComponent,
  RavenStateSaveDialogComponent,
  RavenTreeComponent,
];

export const ENTRY_COMPONENTS = [
  RavenConfirmDialogComponent,
  RavenFileImportDialogComponent,
  RavenLayoutApplyDialogComponent,
  RavenStateSaveDialogComponent,
];

export const MODULES = [
  CommonModule,
  FalconModule,
  FormsModule,
  MaterialModule,
  ReactiveFormsModule,
  SortablejsModule,
];

@NgModule({
  declarations: DECLARATIONS,
  entryComponents: ENTRY_COMPONENTS,
  exports: DECLARATIONS,
  imports: MODULES,
})
export class RavenModule {}
