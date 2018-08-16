/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';

import { SortablejsModule } from 'angular-sortablejs';
import { AngularSplitModule } from 'angular-split';

import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';

import { reducers } from './raven-store';

import { MaterialModule } from './../shared/material';
import { SharedModule } from './../shared/shared.module';

import {
  RavenAppComponent,
  SourceExplorerComponent,
  TimelineComponent,
} from './containers';

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
  RavenGuard,
} from './guards';

import {
  RavenRoutingModule,
} from './raven-routing.module';

export const DECLARATIONS = [
  RavenAppComponent,
  SourceExplorerComponent,
  TimelineComponent,
];

export const EFFECTS = [
  DialogEffects,
  EpochsEffects,
  LayoutEffects,
  OutputEffects,
  RouterEffects,
  SourceExplorerEffects,
  TimeCursorEffects,
  TimelineEffects,
  ToastEffects,
];

export const MODULES = [
  CommonModule,
  HttpClientModule,
  AngularSplitModule,
  FlexLayoutModule,
  SortablejsModule,
  RavenRoutingModule,
  MaterialModule,
  SharedModule,
  StoreModule.forFeature('raven', reducers),
  EffectsModule.forFeature(EFFECTS),
];

export const PROVIDERS = [
  RavenGuard,
];

@NgModule({
  declarations: DECLARATIONS,
  exports: DECLARATIONS,
  imports: MODULES,
  providers: PROVIDERS,
})
export class RavenModule {}
