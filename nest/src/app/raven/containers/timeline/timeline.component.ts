/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { MatTabChangeEvent } from '@angular/material';
import { select, Store } from '@ngrx/store';
import { combineLatest, Observable, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { ConfigState } from '../../../../config';
import { getSourceIdsByLabelInBands } from '../../../shared/util';
import { TimeCursorState } from '../../reducers/time-cursor.reducer';
import { TimelineState } from '../../reducers/timeline.reducer';

import {
  RavenActivityPoint,
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

import * as configSelectors from '../../../shared/selectors/config.selectors';
import * as epochsSelectors from '../../selectors/epochs.selectors';
import * as layoutSelectors from '../../selectors/layout.selectors';
import * as outputSelectors from '../../selectors/output.selectors';
import * as situAwareSelectors from '../../selectors/situational-awareness.selectors';
import * as sourceExplorerSelectors from '../../selectors/source-explorer.selectors';
import * as timeCursorSelectors from '../../selectors/time-cursor.selectors';
import * as timelineSelectors from '../../selectors/timeline.selectors';

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
  baseUrl$: Observable<string>;
  defaultBandSettings$: Observable<RavenDefaultBandSettings>;
  excludeActivityTypes$: Observable<string[]>;
  itarMessage$: Observable<string>;

  // Epoch state.
  dayCode$: Observable<string>;
  earthSecToEpochSec$: Observable<number>;
  epochs$: Observable<RavenEpoch[]>;
  inUseEpoch$: Observable<RavenEpoch | null>;

  // Layout state.
  rightPanelSelectedTabIndex$: Observable<number | null>;
  showActivityPointMetadata$: Observable<boolean>;
  showActivityPointParameters$: Observable<boolean>;
  showApplyLayoutDrawer$: Observable<boolean>;
  showDetailsPanel$: Observable<boolean>;
  showEpochsDrawer$: Observable<boolean>;
  showGlobalSettingsDrawer$: Observable<boolean>;
  showLeftPanel$: Observable<boolean>;
  showOutputDrawer$: Observable<boolean>;
  showRightPanel$: Observable<boolean>;
  showSituationalAwarenessDrawer$: Observable<boolean>;
  showSouthBandsPanel$: Observable<boolean>;
  showTimeCursorDrawer$: Observable<boolean>;
  timelinePanelSize$: Observable<number>;

  // Output state.
  allInOneFile$: Observable<boolean>;
  allInOneFilename$: Observable<string>;
  decimateOutputData$: Observable<boolean>;
  outputFormat$: Observable<string>;
  outputSourceIdsByLabel$: Observable<StringTMap<string[]>>;

  // Source Explorer state.
  currentState$: Observable<RavenState | null>;
  currentStateId$: Observable<string>;
  customFiltersBySourceId$: Observable<StringTMap<RavenCustomFilter[]>>;
  filtersByTarget$: Observable<StringTMap<StringTMap<string[]>>>;
  treeBySourceId$: Observable<StringTMap<RavenSource>>;

  // SituationalAwareness state.
  nowMinus$: Observable<number | null>;
  nowPlus$: Observable<number | null>;
  pageDuration$: Observable<number | null>;
  pefEntries$: Observable<RavenSituationalAwarenessPefEntry[] | null>;
  situationalAware$: Observable<boolean>;
  startTime$: Observable<number | null>;
  useNow$: Observable<boolean>;

  // Time cursor state.
  autoPage$: Observable<boolean>;
  clockRate$: Observable<number>;
  cursorTime$: Observable<number | null>;
  currentTimeDelta$: Observable<number | null>;
  cursorColor$: Observable<string>;
  cursorWidth$: Observable<number>;
  showTimeCursor$: Observable<boolean>;
  setCursorTime$: Observable<number | null>;

  // Timeline state.
  bands$: Observable<RavenCompositeBand[]>;
  guides$: Observable<number[]>;
  lastClickTime$: Observable<number | null>;
  maxTimeRange$: Observable<RavenTimeRange>;
  selectedBandId$: Observable<string>;
  selectedPoint$: Observable<RavenPoint | null>;
  selectedSubBand$: Observable<RavenSubBand | null>;
  selectedSubBandId$: Observable<string>;
  selectedSubBandPoints$: Observable<RavenPoint[]>;
  subBandSourceIdsByLabel$: Observable<StringTMap<string[]>>;
  viewTimeRange$: Observable<RavenTimeRange>;

  // Local (non-Observable) state. Derived from store state.
  bands: RavenCompositeBand[];
  baseUrl: string;
  selectedBandId: string;
  selectedSubBandId: string;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private store: Store<TimelineState | ConfigState | TimeCursorState>,
  ) {
    // Config state.
    this.baseUrl$ = this.store.pipe(select(configSelectors.getBaseUrl));
    this.excludeActivityTypes$ = this.store.pipe(
      select(configSelectors.getExcludeActivityTypes),
    );
    this.itarMessage$ = this.store.pipe(select(configSelectors.getItarMessage));
    this.defaultBandSettings$ = this.store.pipe(
      select(configSelectors.getDefaultBandSettings),
    );

    // Epoch state.
    this.dayCode$ = this.store.pipe(select(epochsSelectors.getDayCode));
    this.earthSecToEpochSec$ = this.store.pipe(
      select(epochsSelectors.getEarthSecToEpochSec),
    );
    this.epochs$ = this.store.pipe(select(epochsSelectors.getEpochs));
    this.inUseEpoch$ = this.store.pipe(select(epochsSelectors.getInUseEpochs));

    // Layout state.
    this.rightPanelSelectedTabIndex$ = this.store.pipe(
      select(layoutSelectors.getRightPanelSelectedTabIndex),
    );
    this.showActivityPointMetadata$ = this.store.pipe(
      select(layoutSelectors.getShowActivityPointMetadata),
    );
    this.showActivityPointParameters$ = this.store.pipe(
      select(layoutSelectors.getShowActivityPointParameters),
    );
    this.showApplyLayoutDrawer$ = this.store.pipe(
      select(layoutSelectors.getShowApplyLayoutDrawer),
    );
    this.showDetailsPanel$ = this.store.pipe(
      select(layoutSelectors.getShowDetailsPanel),
    );
    this.showEpochsDrawer$ = this.store.pipe(
      select(layoutSelectors.getShowEpochsDrawer),
    );
    this.showGlobalSettingsDrawer$ = this.store.pipe(
      select(layoutSelectors.getShowGlobalSettingsDrawer),
    );
    this.showLeftPanel$ = this.store.pipe(
      select(layoutSelectors.getShowLeftPanel),
    );
    this.showOutputDrawer$ = this.store.pipe(
      select(layoutSelectors.getShowOutputDrawer),
    );
    this.showRightPanel$ = this.store.pipe(
      select(layoutSelectors.getShowRightPanel),
    );
    this.showSituationalAwarenessDrawer$ = this.store.pipe(
      select(layoutSelectors.getShowSituationalAwarenessDrawer),
    );
    this.showSouthBandsPanel$ = this.store.pipe(
      select(layoutSelectors.getShowSouthBandsPanel),
    );
    this.showTimeCursorDrawer$ = this.store.pipe(
      select(layoutSelectors.getShowTimeCursorDrawer),
    );
    this.timelinePanelSize$ = this.store.pipe(
      select(layoutSelectors.getTimelinePanelSize),
    );

    // Output state.
    this.allInOneFile$ = this.store.pipe(
      select(outputSelectors.getAllInOneFile),
    );
    this.allInOneFilename$ = this.store.pipe(
      select(outputSelectors.getAllInOneFilename),
    );
    this.decimateOutputData$ = this.store.pipe(
      select(outputSelectors.getDecimateOutputData),
    );
    this.outputFormat$ = this.store.pipe(
      select(outputSelectors.getOutputFormat),
    );
    this.outputSourceIdsByLabel$ = this.store.pipe(
      select(outputSelectors.getOutputSourceIdsByLabel),
    );

    // Situational awareness state.
    this.nowMinus$ = this.store.pipe(select(situAwareSelectors.getNowMinus));
    this.nowPlus$ = this.store.pipe(select(situAwareSelectors.getNowPlus));
    this.pageDuration$ = this.store.pipe(
      select(situAwareSelectors.getPageDuration),
    );
    this.pefEntries$ = this.store.pipe(
      select(situAwareSelectors.getPefEntries),
    );
    this.situationalAware$ = this.store.pipe(
      select(situAwareSelectors.getSituationalAware),
    );
    this.startTime$ = this.store.pipe(select(situAwareSelectors.getStartTime));
    this.useNow$ = this.store.pipe(select(situAwareSelectors.getUseNow));

    // Source Explorer state.
    this.currentState$ = this.store.pipe(
      select(sourceExplorerSelectors.getCurrentState),
    );
    this.currentStateId$ = this.store.pipe(
      select(sourceExplorerSelectors.getCurrentStateId),
    );
    this.customFiltersBySourceId$ = this.store.pipe(
      select(sourceExplorerSelectors.getCustomFiltersBySourceId),
    );
    this.filtersByTarget$ = this.store.pipe(
      select(sourceExplorerSelectors.getFiltersByTarget),
    );
    this.treeBySourceId$ = this.store.pipe(
      select(sourceExplorerSelectors.getTreeBySourceId),
    );

    // Time cursor state.
    this.autoPage$ = this.store.pipe(select(timeCursorSelectors.getAutoPage));
    this.clockRate$ = this.store.pipe(select(timeCursorSelectors.getClockRate));
    this.currentTimeDelta$ = this.store.pipe(
      select(timeCursorSelectors.getCurrentTimeDelta),
    );
    this.cursorColor$ = this.store.pipe(
      select(timeCursorSelectors.getCursorColor),
    );
    this.cursorTime$ = this.store.pipe(
      select(timeCursorSelectors.getCursorTime),
    );
    this.cursorWidth$ = this.store.pipe(
      select(timeCursorSelectors.getCursorWidth),
    );
    this.showTimeCursor$ = this.store.pipe(
      select(timeCursorSelectors.getShowTimeCursor),
    );
    this.setCursorTime$ = this.store.pipe(
      select(timeCursorSelectors.getSetCursorTime),
    );

    // Timeline state.
    this.bands$ = this.store.pipe(select(timelineSelectors.getBands));
    this.guides$ = this.store.pipe(select(timelineSelectors.getGuides));
    this.lastClickTime$ = this.store.pipe(
      select(timelineSelectors.getLastClickTime),
    );
    this.maxTimeRange$ = this.store.pipe(
      select(timelineSelectors.getMaxTimeRange),
    );
    this.selectedBandId$ = this.store.pipe(
      select(timelineSelectors.getSelectedBandId),
    );
    this.selectedPoint$ = this.store.pipe(
      select(timelineSelectors.getSelectedPoint),
    );
    this.selectedSubBand$ = this.store.pipe(
      select(timelineSelectors.getSelectedSubBand),
    );
    this.selectedSubBandId$ = this.store.pipe(
      select(timelineSelectors.getSelectedSubBandId),
    );
    this.selectedSubBandPoints$ = combineLatest(
      this.selectedSubBand$,
      this.excludeActivityTypes$,
    ).pipe(
      map(([selectedSubBand, excludeActivityTypes]) => {
        if (selectedSubBand) {
          // Filter points in excludeActivityTypes.
          if (selectedSubBand.type === 'activity') {
            const points = selectedSubBand.points as RavenActivityPoint[];
            return points.filter(
              (point: RavenActivityPoint) =>
                !excludeActivityTypes.includes(point.activityType),
            );
          }
          return selectedSubBand.points;
        }
        return [];
      }),
    );
    this.subBandSourceIdsByLabel$ = combineLatest(
      this.bands$,
      this.customFiltersBySourceId$,
      this.filtersByTarget$,
      this.treeBySourceId$,
    ).pipe(
      map(([bands, customFiltersBySourceId, filtersByTarget, treeBySourceId]) =>
        getSourceIdsByLabelInBands(
          bands,
          customFiltersBySourceId,
          filtersByTarget,
          treeBySourceId,
        ),
      ),
    );
    this.viewTimeRange$ = this.store.pipe(
      select(timelineSelectors.getViewTimeRange),
    );

    // Subscribed state. For local use here in the component.
    this.bands$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(bands => (this.bands = bands));
    this.baseUrl$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(baseUrl => (this.baseUrl = baseUrl));
    this.selectedBandId$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(selectedBandId => (this.selectedBandId = selectedBandId));
    this.selectedSubBandId$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(
        selectedSubBandId => (this.selectedSubBandId = selectedSubBandId),
      );
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Event. Called when an `apply-layout` event is fired from the raven-layout-apply component.
   */
  onApplyLayout(update: RavenApplyLayoutUpdate): void {
    this.store.dispatch(new sourceExplorerActions.ApplyLayout(update));
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
   * Event. Called when an activity expansion selection is made.
   *
   * TODO: RemoveChildrenOrDescendants and FetchChildrenOrDescendants
   * should not need selectedBandId and selectedSubBandId here as they can get
   * that from the Effect or Reducer.
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
        new timelineActions.ExpandChildrenOrDescendants(
          this.selectedBandId,
          this.selectedSubBandId,
          e.activityPoint,
          e.expansion,
        ),
      ),
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
}
