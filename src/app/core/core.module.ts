
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { AppComponent } from './containers/app/app.component';
import { PageNotFoundComponent } from './containers/page-not-found/page-not-found.component';
import { MaterialModule } from '../shared/material';

export const COMPONENTS = [
  AppComponent,
  PageNotFoundComponent
];

@NgModule({
  declarations: COMPONENTS,
  exports: COMPONENTS,
  imports: [
    CommonModule,
    RouterModule,
    MaterialModule
  ],
})
export class CoreModule {
  static forRoot() {
    return {
      ngModule: CoreModule,
      providers: [],
    };
  }
}
