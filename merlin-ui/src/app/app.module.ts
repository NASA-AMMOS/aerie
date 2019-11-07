import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { EffectsModule } from '@ngrx/effects';
import { RouterState, StoreRouterConnectingModule } from '@ngrx/router-store';
import { StoreModule } from '@ngrx/store';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import { AngularSplitModule } from 'angular-split';
import { ToastrModule } from 'ngx-toastr';
import { RouterEffects } from '../../libs/ngrx-router';
import { AppRoutingModule } from './app-routing.module';
import { metaReducers, ROOT_REDUCERS } from './app-store';
import { AppComponent } from './app.component';
import { ContainersModule } from './containers';
import { MerlinEffects, NavEffects, ToastEffects } from './effects';
import { MaterialModule } from './material';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    HttpClientModule,
    AngularSplitModule.forRoot(),
    ToastrModule.forRoot({
      countDuplicates: true,
      maxOpened: 4,
      preventDuplicates: true,
      resetTimeoutOnDuplicate: true,
    }),
    StoreModule.forRoot(ROOT_REDUCERS, {
      metaReducers,
      runtimeChecks: {
        strictStateImmutability: true,
        strictActionImmutability: true,
        strictStateSerializability: true,
        // False since we are sending a file in the adaptations.component.
        strictActionSerializability: false,
      },
    }),
    StoreRouterConnectingModule.forRoot({
      routerState: RouterState.Minimal,
    }),
    StoreDevtoolsModule.instrument({
      name: 'merlin-ui',
    }),
    EffectsModule.forRoot([
      RouterEffects,
      MerlinEffects,
      NavEffects,
      ToastEffects,
    ]),
    ContainersModule,
    MaterialModule,
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
