/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { AppState } from './app-store';
import { NavigationDrawerStates } from './shared/actions/config.actions';
import * as dialogActions from './shared/actions/dialog.actions';
import { NestModule } from './shared/models';
import { getAppModules, getNavigationDrawerState } from './shared/selectors';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  styles: [
    `
      :host,
      .container,
      mat-sidenav-container {
        display: block;
        height: 100%;
      }
    `,
  ],
  template: `
    <div class="container">
      <mat-sidenav-container autosize>
        <mat-sidenav
          #sidenav
          mode="side"
          [opened]="(navigationDrawerState | async) !== 'closed'"
        >
          <nest-app-nav
            [modules]="appModules | async"
            [iconsOnly]="(navigationDrawerState | async) === 'collapsed'"
            (aboutClicked)="onAboutClicked()"
          >
          </nest-app-nav>
        </mat-sidenav>
        <mat-sidenav-content #sidenavContent class="app-sidenav-content">
        <main>
          <router-outlet></router-outlet>
        </main>
        </mat-sidenav-content>
      </mat-sidenav-container>
    </div>
  `,
})
export class AppComponent {
  appModules: Observable<NestModule[]>;
  navigationDrawerState: Observable<NavigationDrawerStates>;

  constructor(private store: Store<AppState>) {
    this.appModules = this.store.pipe(select(getAppModules));
    this.navigationDrawerState = this.store.pipe(
      select(getNavigationDrawerState),
    );
  }

  /**
   * Callback event. Called when the About button is clicked inside the `nest-app-nav` component.
   */
  onAboutClicked() {
    this.store.dispatch(new dialogActions.OpenAboutDialog('400px'));
  }
}
