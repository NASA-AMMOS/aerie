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

import { select, Store } from '@ngrx/store';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { toCompositeBand, toDividerBand } from '../../../shared/util';

import { RavenTimeRange } from '../../../shared/models';

import * as fromConfig from '../../../shared/reducers/config.reducer';
import * as fromLayout from '../../reducers/layout.reducer';
import * as fromSituationalAwareness from '../../reducers/situational-awareness.reducer';
import * as fromSourceExplorer from '../../reducers/source-explorer.reducer';
import * as fromTimeline from '../../reducers/timeline.reducer';

import * as configActions from '../../../shared/actions/config.actions';
import * as dialogActions from '../../actions/dialog.actions';
import * as layoutActions from '../../actions/layout.actions';
import * as timelineActions from '../../actions/timeline.actions';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-app',
  styleUrls: ['./raven-app.component.css'],
  templateUrl: './raven-app.component.html',
})
export class RavenAppComponent implements OnDestroy {
  info: string;
  loading: boolean;
  mode: string;
  selectedBandId: string;

  /**
   * Current state of the navigation drawer
   */
  navigationDrawerState: configActions.NavigationDrawerStates;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<fromSourceExplorer.SourceExplorerState>,
  ) {
    // Combine all fetch pending observable for progress bar.
    combineLatest(
      this.store.pipe(select(fromLayout.getPending)),
      this.store.pipe(select(fromSituationalAwareness.getPending)),
      this.store.pipe(select(fromSourceExplorer.getPending)),
      this.store.pipe(select(fromTimeline.getPending)),
    )
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(loading => {
        this.loading = loading[0] || loading[1] || loading[2] || loading[3];
        this.markForCheck();
      });

    // Config version.
    this.store
      .pipe(
        select(fromConfig.getVersion),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(v => {
        this.info = this.getInfo(v.version, v.branch, v.commit);
      });

    // Layout mode.
    this.store
      .pipe(
        select(fromLayout.getMode),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(mode => {
        this.mode = mode;
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

    // Timeline state.
    this.store
      .pipe(
        select(fromTimeline.getTimelineState),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(state => {
        this.selectedBandId = state.selectedBandId;
        this.markForCheck();
      });
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

  /**
   * Get a string for the info tooltip.
   */
  getInfo(version: string, branch: string, commit: string): string {
    return `
      Raven ${version} - ${branch} - ${commit}\n
      Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED.
      United States Government sponsorship acknowledged.
      Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.\n
    `;
  }

  /**
   * Event. Called when a `add-divider-band` event is fired.
   */
  onAddDividerBand(): void {
    this.store.dispatch(
      new timelineActions.AddBand(null, toCompositeBand(toDividerBand()), {
        afterBandId: this.selectedBandId,
      }),
    );
  }

  onAddGuide() {
    this.store.dispatch(new timelineActions.AddGuide());
  }

  /**
   * The hamburger menu was clicked
   */
  onMenuClicked() {
    this.store.dispatch(new configActions.ToggleNavigationDrawer());
  }

  onRemoveAllBands() {
    this.store.dispatch(new dialogActions.OpenRemoveAllBandsDialog('400px'));
  }

  onRemoveGuide() {
    this.store.dispatch(new timelineActions.RemoveGuide());
  }

  onRemoveAllGuides() {
    this.store.dispatch(new dialogActions.OpenRemoveAllGuidesDialog('400px'));
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
    this.store.dispatch(
      new dialogActions.OpenConfirmDialog('Close', this.info, '400px'),
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

  toggleShareableLinkDialog() {
    this.store.dispatch(new dialogActions.OpenShareableLinkDialog('600px'));
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
