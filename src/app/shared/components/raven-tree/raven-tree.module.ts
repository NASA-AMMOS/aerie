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
import { RavenCategoryModule } from '../raven-category/raven-category.module';
import { RavenCustomFilterModule } from '../raven-custom-filter/raven-custom-filter.module';
import { RavenCustomGraphableModule } from '../raven-custom-graphable/raven-custom-graphable.module';
import { RavenFileModule } from '../raven-file/raven-file.module';
import { RavenFilterModule } from '../raven-filter/raven-filter.module';
import { RavenFolderModule } from '../raven-folder/raven-folder.module';
import { RavenGraphableFilterModule } from '../raven-graphable-filter/raven-graphable-filter.module';
import { RavenGraphableModule } from '../raven-graphable/raven-graphable.module';
import { RavenTreeComponent } from './raven-tree.component';

@NgModule({
  declarations: [
    RavenTreeComponent,
  ],
  exports: [
    RavenTreeComponent,
  ],
  imports: [
    CommonModule,
    RavenCategoryModule,
    RavenCustomFilterModule,
    RavenCustomGraphableModule,
    RavenFileModule,
    RavenFilterModule,
    RavenFolderModule,
    RavenGraphableFilterModule,
    RavenGraphableModule,
  ],
})
export class RavenTreeModule {}
