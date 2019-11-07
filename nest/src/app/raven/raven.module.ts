/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { NgModule } from '@angular/core';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import {
  RavenConfirmDialogModule,
  RavenCustomFilterDialogModule,
  RavenCustomGraphDialogModule,
  RavenFileImportDialogModule,
  RavenFolderDialogModule,
  RavenLoadEpochDialogModule,
  RavenPinDialogModule,
  RavenSaveNewEpochFileDialogModule,
  RavenSettingsBandsDialogModule,
  RavenShareableLinkDialogModule,
  RavenStateSaveDialogModule,
} from './components';
import { RavenAppModule } from './containers/raven-app/raven-app.module';
import {
  DialogEffects,
  EpochsEffects,
  LayoutEffects,
  OutputEffects,
  RouterEffects,
  SituationalAwarenessEffects,
  SourceExplorerEffects,
  TimeCursorEffects,
  TimelineEffects,
} from './effects';
import { RavenGuard } from './guards';
import { RavenRoutingModule } from './raven-routing.module';
import { reducers } from './raven-store';

@NgModule({
  imports: [
    RavenRoutingModule,
    StoreModule.forFeature('raven', reducers),
    EffectsModule.forFeature([
      DialogEffects,
      EpochsEffects,
      LayoutEffects,
      OutputEffects,
      RouterEffects,
      SituationalAwarenessEffects,
      SourceExplorerEffects,
      TimeCursorEffects,
      TimelineEffects,
    ]),
    RavenAppModule,
    RavenConfirmDialogModule,
    RavenCustomFilterDialogModule,
    RavenCustomGraphDialogModule,
    RavenFileImportDialogModule,
    RavenFolderDialogModule,
    RavenLoadEpochDialogModule,
    RavenPinDialogModule,
    RavenSaveNewEpochFileDialogModule,
    RavenSettingsBandsDialogModule,
    RavenShareableLinkDialogModule,
    RavenStateSaveDialogModule,
  ],
  providers: [RavenGuard],
})
export class RavenModule {}
