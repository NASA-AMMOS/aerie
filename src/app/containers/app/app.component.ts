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
  HostListener,
  OnDestroy,
} from '@angular/core';

import { Store } from '@ngrx/store';

import {
  combineLatest,
  Observable,
  Subject,
} from 'rxjs';

import {
  map,
  takeUntil,
  tap,
} from 'rxjs/operators';

import * as fromConfig from './../../reducers/config';
import * as fromLayout from './../../reducers/layout';
import * as fromSourceExplorer from './../../reducers/source-explorer';
import * as fromTimeline from './../../reducers/timeline';

import * as dialogActions from './../../actions/dialog';
import * as layoutActions from './../../actions/layout';
import * as timelineActions from './../../actions/timeline';

import {
  RavenTimeRange,
  RavenVersion,
} from './../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  styleUrls: ['./app.component.css'],
  templateUrl: './app.component.html',
})
export class AppComponent implements OnDestroy {
  loading$: Observable<boolean>;

  info: string;
  mode: string;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<fromSourceExplorer.SourceExplorerState>,
  ) {
    // Combine all fetch pending observables for use in progress bar.
    this.loading$ = combineLatest(
      this.store.select(fromSourceExplorer.getPending),
      this.store.select(fromTimeline.getPending),
    ).pipe(
      map(loading => loading[0] || loading[1]),
      tap(() => this.markForCheck()),
    );

    // Config version.
    this.store.select(fromConfig.getVersion).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(version => {
      this.info = this.getInfo(version);
    });

    // Layout mode.
    this.store.select(fromLayout.getMode).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(mode => {
      this.mode = mode;
      this.markForCheck();
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Global Event. Called on keydown event.
   */
  @HostListener('document:keydown', ['$event'])
  onResize(e: KeyboardEvent): void {
    if (e.keyCode === 49 && this.mode !== 'minimal') { // 1 key.
      this.store.dispatch(new layoutActions.ToggleLeftPanel());
    } else if (e.keyCode === 50) { // 2 key.
      this.store.dispatch(new layoutActions.ToggleRightPanel());
    } else if (e.keyCode === 51) { // 3 key.
      this.store.dispatch(new layoutActions.ToggleSouthBandsPanel());
    } else if (e.keyCode === 52) { // 4 key.
      this.store.dispatch(new layoutActions.ToggleDetailsPanel());
    } else if (e.keyCode === 53) { // 5 key.
      this.store.dispatch(new layoutActions.ToggleGlobalSettingsDrawer());
    } else if (e.keyCode === 61) { // + key.
      this.store.dispatch(new timelineActions.ZoomInViewTimeRange());
    } else if (e.keyCode === 173) { // - key.
      this.store.dispatch(new timelineActions.ZoomOutViewTimeRange());
    }
  }

  /**
   * Helper. Marks this component for change detection check,
   * and then detects changes on the next tick.
   *
   * TODO: Find out how we can remove this.
   */
  markForCheck() {
    this.changeDetector.markForCheck();
    setTimeout(() => this.changeDetector.detectChanges());
  }

  /**
   * Get a string for the info tooltip.
   */
  getInfo(ravenVersion: RavenVersion): string {
    return `
      Raven ${ravenVersion.version} - ${ravenVersion.branch} - ${ravenVersion.commit}\n
      Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED.
      United States Government sponsorship acknowledged.
      Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.\n
    `;
  }

  onPanLeft() {
    this.store.dispatch(new timelineActions.PanLeftViewTimeRange());
  }

  onPanRight() {
    this.store.dispatch(new timelineActions.PanRightViewTimeRange());
  }

  onPanTo(viewTimeRange: RavenTimeRange) {
    this.store.dispatch(new timelineActions.UpdateViewTimeRange(viewTimeRange));
  }

  onReset() {
    this.store.dispatch(new timelineActions.ResetViewTimeRange());
  }

  onZoomIn() {
    this.store.dispatch(new timelineActions.ZoomInViewTimeRange());
  }

  onZoomOut() {
    this.store.dispatch(new timelineActions.ZoomOutViewTimeRange());
  }

  toggleAboutDialog() {
    this.store.dispatch(new dialogActions.OpenConfirmDialog('Close', this.info, '400px'));
  }

  toggleDetailsPanel() {
    this.store.dispatch(new layoutActions.ToggleDetailsPanel());
  }

  toggleEpochsDrawer() {
    this.store.dispatch(new layoutActions.ToggleEpochsDrawer());
  }

  toggleGlobalSettingsDrawer() {
    this.store.dispatch(new layoutActions.ToggleGlobalSettingsDrawer());
  }

  toggleLeftPanel() {
    this.store.dispatch(new layoutActions.ToggleLeftPanel());
  }

  toggleOutputDrawer() {
    this.store.dispatch(new layoutActions.ToggleOutputDrawer());
  }

  toggleRightPanel() {
    this.store.dispatch(new layoutActions.ToggleRightPanel());
  }

  toggleShareableLinkDialog() {
    this.store.dispatch(new dialogActions.OpenShareableLinkDialog('600px'));
  }

  toggleSouthBandsPanel() {
    this.store.dispatch(new layoutActions.ToggleSouthBandsPanel());
  }

  toggleTimeCursorDrawer() {
    this.store.dispatch(new layoutActions.ToggleTimeCursorDrawer());
  }
}
