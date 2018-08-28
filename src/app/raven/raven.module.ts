/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { RavenAppModule } from './containers/raven-app/raven-app.module';
import { RavenGuard } from './guards';
import { RavenRoutingModule } from './raven-routing.module';
import { reducers } from './raven-store';

import {
  DialogEffects,
  EpochsEffects,
  LayoutEffects,
  OutputEffects,
  RouterEffects,
  SourceExplorerEffects,
  TimeCursorEffects,
  TimelineEffects,
  ToastEffects,
} from './effects';

import {
  RavenConfirmDialogModule,
  RavenCustomFilterDialogModule,
  RavenCustomGraphDialogModule,
  RavenFileImportDialogModule,
  RavenPinDialogModule,
  RavenShareableLinkDialogModule,
  RavenStateSaveDialogModule,
} from '../shared/components/modules';

import {
  RavenConfirmDialogComponent,
  RavenCustomFilterDialogComponent,
  RavenCustomGraphDialogComponent,
  RavenFileImportDialogComponent,
  RavenPinDialogComponent,
  RavenShareableLinkDialogComponent,
  RavenStateSaveDialogComponent,
} from '../shared/components/components';

@NgModule({
  entryComponents: [
    RavenConfirmDialogComponent,
    RavenCustomFilterDialogComponent,
    RavenCustomGraphDialogComponent,
    RavenFileImportDialogComponent,
    RavenPinDialogComponent,
    RavenShareableLinkDialogComponent,
    RavenStateSaveDialogComponent,
  ],
  imports: [
    HttpClientModule,
    RavenRoutingModule,
    StoreModule.forFeature('raven', reducers),
    EffectsModule.forFeature([
      DialogEffects,
      EpochsEffects,
      LayoutEffects,
      OutputEffects,
      RouterEffects,
      SourceExplorerEffects,
      TimeCursorEffects,
      TimelineEffects,
      ToastEffects,
    ]),
    RavenAppModule,
    RavenConfirmDialogModule,
    RavenCustomFilterDialogModule,
    RavenCustomGraphDialogModule,
    RavenFileImportDialogModule,
    RavenPinDialogModule,
    RavenShareableLinkDialogModule,
    RavenStateSaveDialogModule,
  ],
  providers: [
    RavenGuard,
  ],
})
export class RavenModule {}
