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
import { combineLatest, Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { DialogActions } from '../../../shared/actions';
import { LayoutActions, TimelineActions } from '../../actions';
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
  showProgressBar$: Observable<boolean>;
  mode$: Observable<string>;
  selectedBandId$: Observable<string>;

  about: string;
  mode: string;
  selectedBandId: string;

  private subscriptions = new Subscription();

  constructor(private store: Store<SourceExplorerState>) {
    this.mode$ = this.store.pipe(select(getMode));
    this.showProgressBar$ = this.getShowProgressBar();
    this.selectedBandId$ = this.store.pipe(select(getSelectedBandId));

    this.subscriptions.add(this.mode$.subscribe(mode => (this.mode = mode)));
    this.subscriptions.add(
      this.selectedBandId$.subscribe(
        selectedBandId => (this.selectedBandId = selectedBandId),
      ),
    );
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
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
      this.store.dispatch(LayoutActions.toggleLeftPanel());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Digit2') {
      // Ctrl+Shift+2.
      this.store.dispatch(LayoutActions.toggleRightPanel());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Digit3') {
      // Ctrl+Shift+3.
      this.store.dispatch(LayoutActions.toggleSouthBandsPanel());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Digit4') {
      // Ctrl+Shift+4.
      this.store.dispatch(LayoutActions.toggleDetailsPanel());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Digit5') {
      // Ctrl+Shift+5.
      this.store.dispatch(LayoutActions.toggleGlobalSettingsDrawer({}));
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Equal') {
      // Ctrl+Shift+Equal.
      this.store.dispatch(TimelineActions.zoomInViewTimeRange());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'Minus') {
      // Ctrl+Shift+Minus.
      this.store.dispatch(TimelineActions.zoomOutViewTimeRange());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'ArrowRight') {
      // Ctrl+Shift+ArrowRight.
      this.store.dispatch(TimelineActions.panRightViewTimeRange());
    } else if (e.ctrlKey && e.shiftKey && e.code === 'ArrowLeft') {
      // Ctrl+Shift+ArrowLeft.
      this.store.dispatch(TimelineActions.panLeftViewTimeRange());
    }
  }

  /**
   * Returns an Observable that we use to show a progress bar.
   */
  getShowProgressBar(): Observable<boolean> {
    return combineLatest([
      this.store.pipe(select(getLayoutPending)),
      this.store.pipe(select(getSituationalAwarenessPending)),
      this.store.pipe(select(getSourceExplorerPending)),
      this.store.pipe(select(getTimelinePending)),
    ]).pipe(
      map(pending => pending[0] || pending[1] || pending[2] || pending[3]),
    );
  }

  onAboutClicked() {
    this.store.dispatch(DialogActions.openAboutDialog({ width: '400px' }));
  }

  toggleDetailsPanel() {
    this.store.dispatch(LayoutActions.toggleDetailsPanel());
  }

  toggleEpochsDrawer() {
    this.store.dispatch(LayoutActions.toggleEpochsDrawer({}));
  }

  toggleGlobalSettingsDrawer() {
    this.store.dispatch(LayoutActions.toggleGlobalSettingsDrawer({}));
  }

  toggleLeftPanel() {
    this.store.dispatch(LayoutActions.toggleLeftPanel());
  }

  toggleOutputDrawer() {
    this.store.dispatch(LayoutActions.toggleOutputDrawer({}));
  }

  toggleRightPanel() {
    this.store.dispatch(LayoutActions.toggleRightPanel());
  }

  toggleSituationalAwarenessDrawer() {
    this.store.dispatch(LayoutActions.toggleSituationalAwarenessDrawer({}));
  }

  toggleSouthBandsPanel() {
    this.store.dispatch(LayoutActions.toggleSouthBandsPanel());
  }

  toggleTimeCursorDrawer() {
    this.store.dispatch(LayoutActions.toggleTimeCursorDrawer({}));
  }
}
