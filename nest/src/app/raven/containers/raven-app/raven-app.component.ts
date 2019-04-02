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
  Component,
  HostListener,
  OnDestroy,
} from '@angular/core';
import { select, Store } from '@ngrx/store';
import { combineLatest, Observable, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import * as configActions from '../../../shared/actions/config.actions';
import { getVersion } from '../../../shared/selectors';
import * as dialogActions from '../../actions/dialog.actions';
import * as layoutActions from '../../actions/layout.actions';
import * as timelineActions from '../../actions/timeline.actions';
import { SourceExplorerState } from '../../reducers/source-explorer.reducer';
import {
  getLayoutPending,
  getMode,
  getSelectedBandId,
  getSituationalAwarenessPending,
  getSourceExplorerPending,
  getTimelinePending,
} from '../../selectors';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-app',
  styleUrls: ['./raven-app.component.css'],
  templateUrl: './raven-app.component.html',
})
export class RavenAppComponent implements OnDestroy {
  about$: Observable<string>;
  showProgressBar$: Observable<boolean>;
  mode$: Observable<string>;
  selectedBandId$: Observable<string>;

  about: string;
  mode: string;
  selectedBandId: string;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(private store: Store<SourceExplorerState>) {
    this.about$ = this.getAbout();
    this.mode$ = this.store.pipe(select(getMode));
    this.showProgressBar$ = this.getShowProgressBar();
    this.selectedBandId$ = this.store.pipe(select(getSelectedBandId));

    this.about$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(about => (this.about = about));

    this.mode$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(mode => (this.mode = mode));

    this.selectedBandId$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(selectedBandId => (this.selectedBandId = selectedBandId));
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Global Event. Called on keydown event.
   * Since this is a global listener, do not `preventDefault` in here or it
   * will nuke all other key presses.
   */
  @HostListener('window:keydown', ['$event'])
  onResize(e: KeyboardEvent): void {
    if (
      e.ctrlKey &&
      e.shiftKey &&
      e.code === 'Digit1' &&
      this.mode !== 'minimal'
    ) {
      // Ctrl+Shift+1.
      this.store.dispatch(new layoutActions.ToggleLeftPanel());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Digit2') {
      // Ctrl+Shift+2.
      this.store.dispatch(new layoutActions.ToggleRightPanel());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Digit3') {
      // Ctrl+Shift+3.
      this.store.dispatch(new layoutActions.ToggleSouthBandsPanel());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Digit4') {
      // Ctrl+Shift+4.
      this.store.dispatch(new layoutActions.ToggleDetailsPanel());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Digit5') {
      // Ctrl+Shift+5.
      this.store.dispatch(new layoutActions.ToggleGlobalSettingsDrawer());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Equal') {
      // Ctrl+Shift+Equal.
      this.store.dispatch(new timelineActions.ZoomInViewTimeRange());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Minus') {
      // Ctrl+Shift+Minus.
      this.store.dispatch(new timelineActions.ZoomOutViewTimeRange());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'ArrowRight') {
      // Ctrl+Shift+ArrowRight.
      this.store.dispatch(new timelineActions.PanRightViewTimeRange());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'ArrowLeft') {
      // Ctrl+Shift+ArrowLeft.
      this.store.dispatch(new timelineActions.PanLeftViewTimeRange());
    }
  }

  /**
   * Returns an Observable that we use to show a progress bar.
   */
  getShowProgressBar(): Observable<boolean> {
    return combineLatest(
      this.store.pipe(select(getLayoutPending)),
      this.store.pipe(select(getSituationalAwarenessPending)),
      this.store.pipe(select(getSourceExplorerPending)),
      this.store.pipe(select(getTimelinePending)),
    ).pipe(
      map(pending => pending[0] || pending[1] || pending[2] || pending[3]),
    );
  }

  /**
   * Get a string for the about dialog.
   */
  getAbout(): Observable<string> {
    return this.store.pipe(select(getVersion)).pipe(
      map(
        v => `
        Raven ${v.version}\n
        Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED.
        United States Government sponsorship acknowledged.
        Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.\n
      `,
      ),
    );
  }

  /**
   * The hamburger menu was clicked
   */
  onMenuClicked() {
    this.store.dispatch(new configActions.ToggleNestNavigationDrawer());
  }

  toggleAboutDialog() {
    this.store.dispatch(
      new dialogActions.OpenConfirmDialog('Close', this.about, '400px'),
    );
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

  toggleSituationalAwarenessDrawer() {
    this.store.dispatch(new layoutActions.ToggleSituationalAwarenessDrawer());
  }

  toggleSouthBandsPanel() {
    this.store.dispatch(new layoutActions.ToggleSouthBandsPanel());
  }

  toggleTimeCursorDrawer() {
    this.store.dispatch(new layoutActions.ToggleTimeCursorDrawer());
  }
}
