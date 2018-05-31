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
import { MaterialModule } from './../material';

import {
  RavenActivityBandComponent,
  RavenActivityPointComponent,
  RavenBandsComponent,
  RavenCategoryComponent,
  RavenCompositeBandComponent,
  RavenConfirmDialogComponent,
  RavenCustomGraphableComponent,
  RavenCustomGraphDialogComponent,
  RavenCustomMetadataComponent,
  RavenDividerBandComponent,
  RavenEpochsComponent,
  RavenFileComponent,
  RavenFileImportDialogComponent,
  RavenFileMetadataComponent,
  RavenFilterComponent,
  RavenFolderComponent,
  RavenGraphableComponent,
  RavenGraphableFilterComponent,
  RavenLayoutApplyDialogComponent,
  RavenPinDialogComponent,
  RavenResourceBandComponent,
  RavenResourcePointComponent,
  RavenSettingsBandsComponent,
  RavenSettingsGlobalComponent,
  RavenStateBandComponent,
  RavenStatePointComponent,
  RavenStateSaveDialogComponent,
  RavenTimeBandComponent,
  RavenTimeScrollBarComponent,
  RavenTreeComponent,
} from './components';

import {
  RavenDhmsPipe,
  RavenKeyByPipe,
  RavenTimestampPipe,
  RavenToKeyValueArrayPipe,
} from './pipes';

export const DECLARATIONS = [
  // Components.
  RavenActivityBandComponent,
  RavenActivityPointComponent,
  RavenBandsComponent,
  RavenCategoryComponent,
  RavenCompositeBandComponent,
  RavenConfirmDialogComponent,
  RavenCustomGraphableComponent,
  RavenCustomGraphDialogComponent,
  RavenCustomMetadataComponent,
  RavenDividerBandComponent,
  RavenEpochsComponent,
  RavenFileComponent,
  RavenFileImportDialogComponent,
  RavenFileMetadataComponent,
  RavenFilterComponent,
  RavenFolderComponent,
  RavenGraphableComponent,
  RavenGraphableFilterComponent,
  RavenLayoutApplyDialogComponent,
  RavenPinDialogComponent,
  RavenResourceBandComponent,
  RavenResourcePointComponent,
  RavenSettingsBandsComponent,
  RavenSettingsGlobalComponent,
  RavenStateBandComponent,
  RavenStatePointComponent,
  RavenStateSaveDialogComponent,
  RavenTimeBandComponent,
  RavenTimeScrollBarComponent,
  RavenTreeComponent,

  // Pipes.
  RavenDhmsPipe,
  RavenKeyByPipe,
  RavenTimestampPipe,
  RavenToKeyValueArrayPipe,
];

export const ENTRY_COMPONENTS = [
  RavenConfirmDialogComponent,
  RavenCustomGraphDialogComponent,
  RavenFileImportDialogComponent,
  RavenLayoutApplyDialogComponent,
  RavenPinDialogComponent,
  RavenStateSaveDialogComponent,
];

export const MODULES = [
  CommonModule,
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
