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
import { RavenActivityBandModule } from '../raven-activity-band/raven-activity-band.module';
import { RavenDividerBandModule } from '../raven-divider-band/raven-divider-band.module';
import { RavenResourceBandModule } from '../raven-resource-band/raven-resource-band.module';
import { RavenStateBandModule } from '../raven-state-band/raven-state-band.module';
import { RavenCompositeBandComponent } from './raven-composite-band.component';

@NgModule({
  declarations: [
    RavenCompositeBandComponent,
  ],
  exports: [
    RavenCompositeBandComponent,
  ],
  imports: [
    CommonModule,
    RavenActivityBandModule,
    RavenDividerBandModule,
    RavenResourceBandModule,
    RavenStateBandModule,
  ],
})
export class RavenCompositeBandModule {}
