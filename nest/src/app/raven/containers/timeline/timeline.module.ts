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
import {
  MatButtonModule,
  MatCardModule,
  MatIconModule,
  MatSidenavModule,
  MatTabsModule,
} from '@angular/material';
import { AngularSplitModule } from 'angular-split';
import {
  RavenActivityPointModule,
  RavenBandsModule,
  RavenEpochsModule,
  RavenGuideBandModule,
  RavenLayoutApplyModule,
  RavenManageGraphModule,
  RavenOutputModule,
  RavenResourcePointModule,
  RavenSettingsGlobalModule,
  RavenSituationalAwarenessModule,
  RavenStatePointModule,
  RavenTableModule,
  RavenTimeBandModule,
  RavenTimeCursorModule,
  RavenTimeScrollBarModule,
} from '../../components';
import { RavenKeyByPipeModule } from '../../pipes';
import { SourceExplorerModule } from '../source-explorer/source-explorer.module';
import { TimelineComponent } from './timeline.component';

@NgModule({
  declarations: [TimelineComponent],
  exports: [TimelineComponent],
  imports: [
    AngularSplitModule.forChild(),
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTabsModule,
    MatSidenavModule,
    RavenActivityPointModule,
    RavenBandsModule,
    RavenEpochsModule,
    RavenGuideBandModule,
    RavenKeyByPipeModule,
    RavenLayoutApplyModule,
    RavenManageGraphModule,
    RavenOutputModule,
    RavenResourcePointModule,
    RavenSettingsGlobalModule,
    RavenSituationalAwarenessModule,
    RavenStatePointModule,
    RavenTableModule,
    RavenTimeBandModule,
    RavenTimeCursorModule,
    RavenTimeScrollBarModule,
    SourceExplorerModule,
  ],
})
export class TimelineModule {}
