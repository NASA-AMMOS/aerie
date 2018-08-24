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
import { AgGridModule } from 'ag-grid-angular';
import { SortablejsModule } from 'angular-sortablejs';
import { MaterialModule } from './material';

import {
  // Modules + Components (TODO: Make all modules).
  HBCodeMirrorModule,
  HBCommandLoaderModule,
  RavenActivityBandModule,
  RavenActivityPointComponent,
  RavenBandsComponent,
  RavenCategoryComponent,
  RavenCompositeBandModule,
  RavenConfirmDialogComponent,
  RavenCustomFilterComponent,
  RavenCustomFilterDialogComponent,
  RavenCustomGraphableComponent,
  RavenCustomGraphDialogComponent,
  RavenCustomMetadataComponent,
  RavenDividerBandModule,
  RavenEpochsComponent,
  RavenFileComponent,
  RavenFileImportDialogComponent,
  RavenFileMetadataComponent,
  RavenFilterComponent,
  RavenFolderComponent,
  RavenGraphableComponent,
  RavenGraphableFilterComponent,
  RavenLayoutApplyComponent,
  RavenOutputComponent,
  RavenPanToDurationComponent,
  RavenPinDialogComponent,
  RavenResourceBandModule,
  RavenResourcePointComponent,
  RavenSettingsBandsComponent,
  RavenSettingsGlobalComponent,
  RavenShareableLinkDialogComponent,
  RavenStateBandModule,
  RavenStatePointComponent,
  RavenStateSaveDialogComponent,
  RavenTableComponent,
  RavenTableDetailComponent,
  RavenTimeBandComponent,
  RavenTimeCursorComponent,
  RavenTimeScrollBarComponent,
  RavenTreeComponent,
} from './components';

import {
  RavenDhmsPipe,
  RavenKeyByPipe,
  RavenResourcePointValuePipe,
  RavenTimestampPipe,
  RavenToKeyValueArrayPipe,
} from './pipes';

export const APP_MODULES = [
  HBCodeMirrorModule,
  HBCommandLoaderModule,
  RavenActivityBandModule,
  RavenCompositeBandModule,
  RavenDividerBandModule,
  RavenResourceBandModule,
  RavenStateBandModule,
];

export const DECLARATIONS = [
  // Components.
  RavenActivityPointComponent,
  RavenBandsComponent,
  RavenCategoryComponent,
  RavenConfirmDialogComponent,
  RavenCustomFilterComponent,
  RavenCustomFilterDialogComponent,
  RavenCustomGraphableComponent,
  RavenCustomGraphDialogComponent,
  RavenCustomMetadataComponent,
  RavenEpochsComponent,
  RavenFileComponent,
  RavenFileImportDialogComponent,
  RavenFileMetadataComponent,
  RavenFilterComponent,
  RavenFolderComponent,
  RavenGraphableComponent,
  RavenGraphableFilterComponent,
  RavenLayoutApplyComponent,
  RavenOutputComponent,
  RavenPanToDurationComponent,
  RavenPinDialogComponent,
  RavenResourcePointComponent,
  RavenSettingsBandsComponent,
  RavenSettingsGlobalComponent,
  RavenShareableLinkDialogComponent,
  RavenStatePointComponent,
  RavenStateSaveDialogComponent,
  RavenTableComponent,
  RavenTableDetailComponent,
  RavenTimeBandComponent,
  RavenTimeCursorComponent,
  RavenTimeScrollBarComponent,
  RavenTreeComponent,

  // Pipes.
  RavenDhmsPipe,
  RavenKeyByPipe,
  RavenResourcePointValuePipe,
  RavenTimestampPipe,
  RavenToKeyValueArrayPipe,
];

export const ENTRY_COMPONENTS = [
  RavenConfirmDialogComponent,
  RavenCustomFilterDialogComponent,
  RavenCustomGraphDialogComponent,
  RavenFileImportDialogComponent,
  RavenPinDialogComponent,
  RavenShareableLinkDialogComponent,
  RavenStateSaveDialogComponent,
];

export const EXPORTS = [
  ...DECLARATIONS,
  ...APP_MODULES,
];

export const MODULES = [
  CommonModule,
  FormsModule,
  MaterialModule,
  ReactiveFormsModule,
  SortablejsModule,
  AgGridModule.withComponents([
    RavenTableDetailComponent,
  ]),
  ...APP_MODULES,
];

@NgModule({
  declarations: DECLARATIONS,
  entryComponents: ENTRY_COMPONENTS,
  exports: EXPORTS,
  imports: MODULES,
})
export class SharedModule {}
