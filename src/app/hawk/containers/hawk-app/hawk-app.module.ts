/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { NgModule } from '@angular/core';
import { MatDialogModule, MatToolbarModule } from '@angular/material';
import { RavenActivityTypeListModule } from '../../../shared/components/modules';
import { HawkAppComponent } from './hawk-app.component';

@NgModule({
  declarations: [HawkAppComponent],
  exports: [HawkAppComponent],
  imports: [RavenActivityTypeListModule, MatDialogModule, MatToolbarModule],
})
export class HawkAppModule {}
