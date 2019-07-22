/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { OverlayModule } from '@angular/cdk/overlay';
import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import {
  MatButtonModule,
  MatIconModule,
  MatSidenavModule,
} from '@angular/material';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { EffectsModule } from '@ngrx/effects';
import { StoreRouterConnectingModule } from '@ngrx/router-store';
import { StoreModule } from '@ngrx/store';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import { SortablejsModule } from 'angular-sortablejs/dist';
import { AngularSplitModule } from 'angular-split';
import { ContextMenuModule } from 'ngx-contextmenu';
import { ToastrModule } from 'ngx-toastr';
import { RouterEffects } from '../../libs/ngrx-router';
import { environment } from '../environments/environment';
import { AppRoutingModule } from './app-routing.module';
import { metaReducers, ROOT_REDUCERS } from './app-store';
import { AppComponent } from './app.component';
import { NestAboutDialogModule } from './shared/components/nest-about-dialog/nest-about-dialog.module';
import { NestAppNavModule } from './shared/components/nest-app-nav/nest-app-nav.module';
import { ConfigEffects, DialogEffects, NavEffects } from './shared/effects';
import { ToastEffects } from './shared/effects/toast.effects';

@NgModule({
  bootstrap: [AppComponent],
  declarations: [AppComponent],
  imports: [
    BrowserAnimationsModule,
    HttpClientModule,
    AppRoutingModule,
    EffectsModule.forRoot([
      ConfigEffects,
      DialogEffects,
      NavEffects,
      RouterEffects,
      ToastEffects,
    ]),
    OverlayModule,
    SortablejsModule.forRoot({}),
    AngularSplitModule.forRoot(),
    ContextMenuModule.forRoot(),
    StoreModule.forRoot(ROOT_REDUCERS, {
      metaReducers,
      runtimeChecks: {
        strictActionImmutability: !environment.production,
        strictActionSerializability: false,
        strictStateImmutability: !environment.production,
        strictStateSerializability: false,
      },
    }),
    StoreRouterConnectingModule.forRoot({
      stateKey: 'router',
    }),
    ToastrModule.forRoot({
      countDuplicates: true,
      maxOpened: 3,
      preventDuplicates: true,
      resetTimeoutOnDuplicate: true,
    }),
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    NestAboutDialogModule,
    NestAppNavModule,

    // StoreDevtoolsModule must come AFTER StoreModule.
    // To avoid interrupting alphabetical order (and since it's meant for dev only),
    // we'll put it in its own section of the imports list.
    StoreDevtoolsModule.instrument({
      logOnly: environment.production,
      maxAge: 10,
    }),
  ],
})
export class AppModule {}
