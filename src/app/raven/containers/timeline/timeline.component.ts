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

import { MatTabChangeEvent } from '@angular/material';
import { select, Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ConfigState } from '../../../../config';

import {
  RavenActivityPointExpansion,
  RavenApplyLayoutUpdate,
  RavenBandLeftClick,
  RavenCompositeBand,
  RavenCustomFilter,
  RavenDefaultBandSettings,
  RavenEpoch,
  RavenPoint,
  RavenSituationalAwarenessPefEntry,
  RavenSortMessage,
  RavenSource,
  RavenState,
  RavenSubBand,
  RavenTimeRange,
  RavenUpdate,
  StringTMap,
} from '../../../shared/models';

import {
  getSourceIdsByLabelInBands,
  subBandById,
} from '../../../shared/util';

import * as fromConfig from '../../../shared/reducers/config.reducer';
import * as fromEpochs from '../../reducers/epochs.reducer';
import * as fromLayout from '../../reducers/layout.reducer';
import * as fromOutput from '../../reducers/output.reducer';
import * as fromSituationalAwareness from '../../reducers/situational-awareness.reducer';
import * as fromSourceExplorer from '../../reducers/source-explorer.reducer';
import * as fromTimeCursor from '../../reducers/time-cursor.reducer';
import * as fromTimeline from '../../reducers/timeline.reducer';

import * as configActions from '../../../shared/actions/config.actions';
import * as dialogActions from '../../actions/dialog.actions';
import * as epochsActions from '../../actions/epochs.actions';
import * as layoutActions from '../../actions/layout.actions';
import * as outputActions from '../../actions/output.actions';
import * as situationalAwarenessActions from '../../actions/situational-awareness.actions';
import * as sourceExplorerActions from '../../actions/source-explorer.actions';
import * as timeCursorActions from '../../actions/time-cursor.actions';
import * as timelineActions from '../../actions/timeline.actions';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-timeline',
  styleUrls: ['./timeline.component.css'],
  templateUrl: './timeline.component.html',
})
export class TimelineComponent implements OnDestroy {
  // Config state.
  baseUrl: string;
  defaultBandSettings: RavenDefaultBandSettings;
  itarMessage: string;

  // Epoch state.
  dayCode: string;
  earthSecToEpochSec: number;
  epochs: RavenEpoch[];
  inUseEpoch: RavenEpoch | null;

  // Output state.
  allInOneFile: boolean;
  allInOneFilename: string;
  decimateOutputData: boolean;
  outputFormat: string;
  outputSourceIdsByLabel: StringTMap<string[]>;

  // Layout state.
  rightPanelSelectedTabIndex: number | null;
  showActivityPointMetadata: boolean;
  showActivityPointParameters: boolean;
  showApplyLayoutDrawer: boolean;
  showDetailsPanel: boolean;
  showEpochsDrawer: boolean;
  showGlobalSettingsDrawer: boolean;
  showLeftPanel: boolean;
  showOutputDrawer: boolean;
  showRightPanel: boolean;
  showSituationalAwarenessDrawer: boolean;
  showSouthBandsPanel: boolean;
  showTimeCursorDrawer: boolean;
  timelinePanelSize: number;

  // Source Explorer state.
  currentState: RavenState | null;
  currentStateId: string;
  customFiltersBySourceId: StringTMap<RavenCustomFilter[]>;
  filtersByTarget: StringTMap<StringTMap<string[]>>;
  treeBySourceId: StringTMap<RavenSource>;

  // SituationalAwareness state.
  nowMinus: number | null;
  nowPlus: number | null;
  pageDuration: number | null;
  pefEntries: RavenSituationalAwarenessPefEntry[] | null;
  situationalAware: boolean;
  startTime: number | null;
  useNow: boolean;

  // Time cursor state.
  autoPage: boolean;
  clockRate: number;
  cursorTime: number | null;
  currentTimeDelta: number | null;
  cursorColor: string;
  cursorWidth: number;
  showTimeCursor: boolean;
  setCursorTime: number | null;

  // Timeline state.
  bands: RavenCompositeBand[];
  guides: number[];
  lastClickTime: number | null;
  maxTimeRange: RavenTimeRange;
  selectedBandId: string;
  selectedPoint: RavenPoint | null;
  selectedSubBandId: string;
  viewTimeRange: RavenTimeRange;

  // Other state (derived from store state).
  selectedSubBand: RavenSubBand | null;
  selectedSubBandPoints: RavenPoint[];
  subBandSourceIdsByLabel: StringTMap<string[]> = {};

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<
      fromTimeline.TimelineState | ConfigState | fromTimeCursor.TimeCursorState
    >,
  ) {
    // Config state.
    this.store
      .pipe(
        select(fromConfig.getItarMessage),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(itarMessage => {
        this.itarMessage = itarMessage;
        this.markForCheck();
      });

    this.store
      .pipe(
        select(fromConfig.getDefaultBandSettings),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(defaultBandSettings => {
        this.defaultBandSettings = defaultBandSettings;
        this.markForCheck();
      });

    this.store
      .pipe(
        select(fromConfig.getUrls),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(({ baseUrl }) => {
        this.baseUrl = baseUrl;
      });

    // Epoch state.
    this.store
      .pipe(
        select(fromEpochs.getEpochsState),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(state => {
        this.dayCode = state.dayCode;
        this.earthSecToEpochSec = state.earthSecToEpochSec;
        this.epochs = state.epochs;
        this.inUseEpoch = state.inUseEpoch;
        this.markForCheck();
      });

    // Layout state.
    this.store
      .pipe(
        select(fromLayout.getLayoutState),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(state => {
        this.rightPanelSelectedTabIndex = state.rightPanelSelectedTabIndex;
        this.showActivityPointMetadata = state.showActivityPointMetadata;
        this.showActivityPointParameters = state.showActivityPointParameters;
        this.showApplyLayoutDrawer = state.showApplyLayoutDrawer;
        this.showDetailsPanel = state.showDetailsPanel;
        this.showEpochsDrawer = state.showEpochsDrawer;
        this.showGlobalSettingsDrawer = state.showGlobalSettingsDrawer;
        this.showLeftPanel = state.showLeftPanel;
        this.showOutputDrawer = state.showOutputDrawer;
        this.showRightPanel = state.showRightPanel;
        this.showSituationalAwarenessDrawer =
          state.showSituationalAwarenessDrawer;
        this.showSouthBandsPanel = state.showSouthBandsPanel;
        this.showTimeCursorDrawer = state.showTimeCursorDrawer;
        this.timelinePanelSize = state.timelinePanelSize;
        this.markForCheck();
        this.resize();
      });

    // Output state.
    this.store
      .pipe(
        select(fromOutput.getOutputState),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(state => {
        this.allInOneFile = state.allInOneFile;
        this.allInOneFilename = state.allInOneFilename;
        this.decimateOutputData = state.decimateOutputData;
        this.outputFormat = state.outputFormat;
        this.outputSourceIdsByLabel = state.outputSourceIdsByLabel;
        this.markForCheck();
      });

    // Source Explorer state.
    this.store
      .pipe(
        select(fromSourceExplorer.getSourceExplorerState),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(state => {
        this.customFiltersBySourceId = state.customFiltersBySourceId;
        this.filtersByTarget = state.filtersByTarget;
        this.currentState = state.currentState;
        this.currentStateId = state.currentStateId;
        this.treeBySourceId = state.treeBySourceId;
        this.markForCheck();
      });

    // Situational awareness state.
    this.store
      .pipe(
        select(fromSituationalAwareness.getSituationalAwarenessState),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(state => {
        this.nowMinus = state.nowMinus;
        this.nowPlus = state.nowPlus;
        this.pageDuration = state.pageDuration;
        this.pefEntries = state.pefEntries;
        this.situationalAware = state.situationalAware;
        this.startTime = state.startTime;
        this.useNow = state.useNow;
        this.markForCheck();
      });

    // Time cursor state.
    this.store
      .pipe(
        select(fromTimeCursor.getTimeCursorState),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(state => {
        this.autoPage = state.autoPage;
        this.clockRate = state.clockRate;
        this.currentTimeDelta = state.currentTimeDelta;
        this.cursorColor = state.cursorColor;
        this.cursorTime = state.cursorTime;
        this.cursorWidth = state.cursorWidth;
        this.showTimeCursor = state.showTimeCursor;
        this.setCursorTime = state.setCursorTime;
        this.markForCheck();
      });

    // Timeline state.
    this.store
      .pipe(
        select(fromTimeline.getTimelineState),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe(state => {
        this.bands = state.bands;
        this.guides = state.guides;
        this.maxTimeRange = state.maxTimeRange;
        this.lastClickTime = state.lastClickTime;
        this.selectedBandId = state.selectedBandId;
        this.selectedPoint = state.selectedPoint;
        this.selectedSubBandId = state.selectedSubBandId;
        this.viewTimeRange = state.viewTimeRange;
        this.setSelectedSubBand();
        this.subBandSourceIdsByLabel = getSourceIdsByLabelInBands(
          this.bands,
          this.customFiltersBySourceId,
          this.filtersByTarget,
          this.treeBySourceId,
        );
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
   * TODO: Find out how we can remove this.
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
   * Event. Called when an `apply-layout` event is fired from the raven-layout-apply component.
   */
  onApplyLayout(update: RavenApplyLayoutUpdate): void {
    this.store.dispatch(new sourceExplorerActions.ApplyLayout(update));
  }

  /**
   * Event. Called when an `apply-layout-with-pins` event is fired from the raven-layout-apply component.
   */
  onApplyLayoutWithPins(update: RavenApplyLayoutUpdate): void {
    this.store.dispatch(new sourceExplorerActions.ApplyLayoutWithPins(update));
  }

  /**
   * Event. Called when an `apply-state` event is fired from the raven-layout-apply component.
   */
  onApplyState(source: RavenSource): void {
    this.store.dispatch(
      new sourceExplorerActions.ApplyState(source.url, source.id),
    );
  }

  /**
   * Event. Called when a band is left clicked in a raven-bands component.
   */
  onBandLeftClick(e: RavenBandLeftClick): void {
    this.store.dispatch(new timelineActions.SelectBand(e.bandId));
    this.store.dispatch(new timelineActions.UpdateLastClickTime(e.time));

    if (e.subBandId && e.pointId) {
      this.store.dispatch(
        new timelineActions.SelectPoint(e.bandId, e.subBandId, e.pointId),
      );
    }
  }

  /**
   * Event. Called when an activity expansion selecttion is made.
   */
  onChangeActivityExpansion(e: RavenActivityPointExpansion) {
    this.store.dispatch(
      new timelineActions.RemoveChildrenOrDescendants(
        this.selectedBandId,
        this.selectedSubBandId,
        e.activityPoint,
      ),
    );
    if (e.expansion !== 'noExpansion') {
      this.store.dispatch(
        new timelineActions.FetchChildrenOrDescendants(
          this.selectedBandId,
          this.selectedSubBandId,
          e.activityPoint,
          e.expansion,
        ),
      );
    }
  }

  /**
   * Event. Called when an `change-situational-awareness` event is fired
   * from the raven-situational-awareness component.
   */
  onChangeSituationalAwareness(situAware: boolean): void {
    this.store.dispatch(
      new situationalAwarenessActions.ChangeSituationalAwareness(
        `${this.baseUrl}/mpsserver/api/v2/situational_awareness?`,
        situAware,
      ),
    );
  }

  /**
   * Event. Called when a `create-output` event is fired from the raven-output component.
   */
  onCreateOutput(): void {
    this.store.dispatch(new outputActions.CreateOutput());
  }

  /**
   * Event. Called when a `delete-sub-band` event is fired from the raven-settings component.
   */
  onDeleteSubBand(subBand: RavenSubBand): void {
    this.store.dispatch(
      new dialogActions.OpenDeleteSubBandDialog(subBand, '300px'),
    );
  }

  /**
   * Event. Called when a `change-time-cursor` event is fired from the raven-time-cursor component.
   */
  onDisplayTimeCursor(show: boolean): void {
    if (show) {
      this.store.dispatch(new timeCursorActions.ShowTimeCursor());
    } else {
      this.store.dispatch(new timeCursorActions.HideTimeCursor());
    }
  }

  /**
   * Event. Called when a `import-epochs` event is fired from the raven-epochs component.
   */
  onImportEpochs(epochs: RavenEpoch[]) {
    this.store.dispatch(new epochsActions.AddEpochs(epochs));
  }

  /**
   * Event. Called when a tab is changed.
   */
  onSelectedTabChange(e: MatTabChangeEvent) {
    this.store.dispatch(
      new layoutActions.UpdateLayout({ rightPanelSelectedTabIndex: e.index }),
    );
  }

  /**
   * Event. Called when a `new-sort` event is fired from raven-bands.
   */
  onSort(sort: StringTMap<RavenSortMessage>): void {
    this.store.dispatch(new timelineActions.SortBands(sort));
    this.store.dispatch(new layoutActions.Resize());
  }

  /**
   * Event. Called when a toggle event is fired from the apply layout drawer.
   */
  onToggleApplyLayoutDrawer(opened?: boolean) {
    this.store.dispatch(new layoutActions.ToggleApplyLayoutDrawer(opened));
  }

  /**
   * Event. Called when a toggle event is fired from the epochs drawer.
   */
  onToggleEpochsDrawer(opened?: boolean) {
    this.store.dispatch(new layoutActions.ToggleEpochsDrawer(opened));
  }

  /**
   * Event. Called when a toggle event is fired from the epochs drawer.
   */
  onToggleTimeCursorDrawer(opened?: boolean) {
    this.store.dispatch(new layoutActions.ToggleTimeCursorDrawer(opened));
  }

  /**
   * Event. Called when a toggle event is fired from the global settings drawer.
   */
  onToggleGlobalSettingsDrawer(opened?: boolean) {
    this.store.dispatch(new layoutActions.ToggleGlobalSettingsDrawer(opened));
  }

  /**
   * Event. Called when a toggle event is fired from the output drawer.
   */
  onToggleOutputDrawer(opened?: boolean) {
    this.store.dispatch(new layoutActions.ToggleOutputDrawer(opened));
  }

  /**
   * Event. Called when a `toggle-show-activity-point-metadata` event is fired from a raven-activity-point.
   */
  onToggleShowActivityPointMetadata(show: boolean) {
    this.store.dispatch(
      new layoutActions.UpdateLayout({ showActivityPointMetadata: show }),
    );
  }

  /**
   * Event. Called when a `toggle-show-activity-point-parameters` event is fired from a raven-activity-point.
   */
  onToggleShowActivityPointParameters(show: boolean) {
    this.store.dispatch(
      new layoutActions.UpdateLayout({ showActivityPointParameters: show }),
    );
  }

  /**
   * Event. Called when a toggle event is fired from the situational awareness drawer.
   */
  onToggleSituationalAwarenessDrawer(opened?: boolean) {
    this.store.dispatch(
      new layoutActions.ToggleSituationalAwarenessDrawer(opened),
    );
  }

  /**
   * Event. Called when an `update-band` event is fired from the raven-settings component.
   */
  onUpdateBand(e: RavenUpdate): void {
    if (e.bandId) {
      this.store.dispatch(new timelineActions.UpdateBand(e.bandId, e.update));
    }
  }

  /**
   * Event. Called when an `update-band-and-sub-band` event is fired from the raven-settings component.
   */
  onUpdateBandAndSubBand(e: RavenUpdate): void {
    if (e.bandId && e.subBandId) {
      this.store.dispatch(new timelineActions.UpdateBand(e.bandId, e.update));
      this.store.dispatch(
        new timelineActions.UpdateSubBand(e.bandId, e.subBandId, e.update),
      );
    }
  }

  /**
   * Event. Called when an `update-default-band-settings` event is fired from the raven-settings-global component.
   */
  onUpdateDefaultBandSettings(e: RavenUpdate): void {
    this.store.dispatch(new configActions.UpdateDefaultBandSettings(e.update));
  }

  /**
   * Event. Called when an `update-epochs` event is fired from the raven-epochs component.
   */
  onUpdateEpochs(e: RavenUpdate): void {
    this.store.dispatch(new epochsActions.UpdateEpochs(e.update));
  }

  /**
   * Event. Called when an `update-output-settings` event is fired from the raven-output component.
   */
  onUpdateOutputSettings(e: RavenUpdate): void {
    this.store.dispatch(new outputActions.UpdateOutputSettings(e.update));
  }

  /**
   * Event. Called when an `update-situational-awareness-settings` event is fired from the raven-situational-awareness component.
   */
  onUpdateSituationalAwarenessSettings(e: RavenUpdate): void {
    this.store.dispatch(
      new situationalAwarenessActions.UpdateSituationalAwarenessSettings(
        e.update,
      ),
    );
  }

  /**
   * Event. Called when an `update-sub-band` event is fired from the raven-settings component.
   */
  onUpdateSubBand(e: RavenUpdate): void {
    if (e.bandId && e.subBandId) {
      this.store.dispatch(
        new timelineActions.UpdateSubBand(e.bandId, e.subBandId, e.update),
      );
    }
  }

  /**
   * Event. Called when an `update-time-cursor-settings` event is fired from the raven-time-cursor component.
   */
  onUpdateTimeCursorSettings(e: RavenUpdate): void {
    this.store.dispatch(
      new timeCursorActions.UpdateTimeCursorSettings(e.update),
    );
  }

  /**
   * Event. Called when an `update-timeline` event is fired from the raven-settings component.
   */
  onUpdateTimeline(e: RavenUpdate): void {
    this.store.dispatch(new timelineActions.UpdateTimeline(e.update));
  }

  /**
   * Event. Called when catching an `update-view-time-range` event.
   */
  onUpdateViewTimeRange(viewTimeRange: RavenTimeRange): void {
    this.store.dispatch(new timelineActions.UpdateViewTimeRange(viewTimeRange));
  }

  /**
   * Helper that dispatches a resize event.
   */
  resize() {
    this.store.dispatch(new layoutActions.Resize());
  }

  /**
   * Helper that sets the selected sub-band and it's points array for use in the `raven-table`.
   */
  setSelectedSubBand() {
    this.selectedSubBand = subBandById(
      this.bands,
      this.selectedBandId,
      this.selectedSubBandId,
    );

    if (this.selectedSubBand) {
      this.selectedSubBandPoints = this.selectedSubBand.points;
    } else {
      this.selectedSubBandPoints = [];
    }
  }
}
