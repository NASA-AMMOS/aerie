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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { SortablejsModule } from 'angular-sortablejs/dist';

import { EffectsModule } from '@ngrx/effects';
import { StoreRouterConnectingModule } from '@ngrx/router-store';
import { StoreModule } from '@ngrx/store';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';

import { ToastrModule } from 'ngx-toastr';

import { AppRoutingModule } from './app-routing.module';
import { metaReducers, reducers } from './app-store';

import { environment } from '../environments/environment';

import {
  AppComponent,
} from './app.component';

import {
  MaterialModule,
} from './shared/material';

export const MODULES = [
  CommonModule,
  BrowserAnimationsModule,
  HttpClientModule,
  AppRoutingModule,
  MaterialModule,
  SortablejsModule.forRoot({}),
  StoreModule.forRoot(reducers, { metaReducers }),
  StoreRouterConnectingModule.forRoot({ stateKey: 'router' }),
  StoreDevtoolsModule.instrument({ logOnly: environment.production, maxAge: 10 }),
  EffectsModule.forRoot([]),
  ToastrModule.forRoot(),
];

@NgModule({
  bootstrap: [
    AppComponent,
  ],
  declarations: [
    AppComponent,
  ],
  imports: MODULES,
})
export class AppModule {}
