/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
} from '@angular/core';

import { select, Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AppState } from './app-store';
import { NestModule } from './shared/models';

import { NavigationDrawerStates } from './shared/actions/config.actions';
import * as fromConfig from './shared/reducers/config.reducer';

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
        <mat-sidenav #sidenav mode="side" opened>
          <raven-app-nav
            [modules]="appModules"
            [iconsOnly]="navigationDrawerState === 'collapsed'">
          </raven-app-nav>
        </mat-sidenav>
        <mat-sidenav-content #sidenavContent class="app-sidenav-content">
          <router-outlet></router-outlet>
        </mat-sidenav-content>
      </mat-sidenav-container>
    </div>
  `,
})
export class AppComponent implements OnDestroy {
  appModules: NestModule[] = [];
  navigationDrawerState: NavigationDrawerStates =
    NavigationDrawerStates.Collapsed;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<AppState>,
  ) {
    this.store
      .pipe(
        select(fromConfig.getAppModules),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe((appModules: NestModule[]) => {
        this.appModules = appModules;
        this.markForCheck();
      });

    // Navigation drawer state
    this.store
      .pipe(
        select(fromConfig.getNavigationDrawerState),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(state => {
        this.navigationDrawerState = state;
        this.markForCheck();
      });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Helper. Marks this component for change detection check,
   * and then detects changes on the next tick.
   *
   * @todo Find out how we can remove this.
   */
  markForCheck() {
    this.changeDetector.markForCheck();
    setTimeout(() => {
      if (!this.changeDetector['destroyed']) {
        this.changeDetector.detectChanges();
      }
    });
  }
}
