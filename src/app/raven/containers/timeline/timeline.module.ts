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
import { AngularSplitModule } from 'angular-split';
import { RavenKeyByPipeModule } from '../../../shared/pipes';
import { SourceExplorerModule } from '../source-explorer/source-explorer.module';
import { TimelineComponent } from './timeline.component';

import {
  MatButtonModule,
  MatCardModule,
  MatIconModule,
  MatSidenavModule,
  MatTabsModule,
} from '@angular/material';

import {
  RavenActivityPointModule,
  RavenBandsModule,
  RavenEpochsModule,
  RavenLayoutApplyModule,
  RavenOutputModule,
  RavenResourcePointModule,
  RavenSettingsBandsModule,
  RavenSettingsGlobalModule,
  RavenStatePointModule,
  RavenTableModule,
  RavenTimeBandModule,
  RavenTimeCursorModule,
  RavenTimeScrollBarModule,
} from '../../../shared/components/modules';

@NgModule({
  declarations: [
    TimelineComponent,
  ],
  exports: [
    TimelineComponent,
  ],
  imports: [
    AngularSplitModule,
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTabsModule,
    MatSidenavModule,
    RavenActivityPointModule,
    RavenBandsModule,
    RavenEpochsModule,
    RavenKeyByPipeModule,
    RavenLayoutApplyModule,
    RavenOutputModule,
    RavenResourcePointModule,
    RavenSettingsBandsModule,
    RavenSettingsGlobalModule,
    RavenStatePointModule,
    RavenTableModule,
    RavenTimeBandModule,
    RavenTimeCursorModule,
    RavenTimeScrollBarModule,
    SourceExplorerModule,
  ],
})
export class TimelineModule {}
