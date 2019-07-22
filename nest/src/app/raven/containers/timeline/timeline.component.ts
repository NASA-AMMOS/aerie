/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { combineLatest, Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { ConfigState } from '../../../../config';
import * as configActions from '../../../shared/actions/config.actions';
import * as toastActions from '../../../shared/actions/toast.actions';
import { StringTMap, TimeRange } from '../../../shared/models';
import * as configSelectors from '../../../shared/selectors/config.selectors';
import * as dialogActions from '../../actions/dialog.actions';
import * as epochsActions from '../../actions/epochs.actions';
import * as layoutActions from '../../actions/layout.actions';
import * as outputActions from '../../actions/output.actions';
import * as situationalAwarenessActions from '../../actions/situational-awareness.actions';
import * as sourceExplorerActions from '../../actions/source-explorer.actions';
import * as timeCursorActions from '../../actions/time-cursor.actions';
import * as timelineActions from '../../actions/timeline.actions';
import { TimeCursorState } from '../../reducers/time-cursor.reducer';
import { TimelineState } from '../../reducers/timeline.reducer';
import * as epochsSelectors from '../../selectors/epochs.selectors';
import * as layoutSelectors from '../../selectors/layout.selectors';
import * as outputSelectors from '../../selectors/output.selectors';
import * as situAwareSelectors from '../../selectors/situational-awareness.selectors';
import * as sourceExplorerSelectors from '../../selectors/source-explorer.selectors';
import * as timeCursorSelectors from '../../selectors/time-cursor.selectors';
import * as timelineSelectors from '../../selectors/timeline.selectors';
import {
  bandById,
  getSourceIdsByLabelInBands,
  subBandById,
  toCompositeBand,
  toDividerBand,
} from '../../util';
import {
  RavenActivityBand,
  RavenActivityPoint,
  RavenActivityPointExpansion,
  RavenApplyLayoutUpdate,
  RavenBandLeftClick,
  RavenCompositeBand,
  RavenCustomFilter,
  RavenDefaultBandSettings,
  RavenEpoch,
  RavenGuidePoint,
  RavenPoint,
  RavenPointIndex,
  RavenPointUpdate,
  RavenSituationalAwarenessPefEntry,
  RavenSortMessage,
  RavenSource,
  RavenState,
  RavenSubBand,
  RavenUpdate,
} from './../../models';

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
  epochsModified$: Observable<boolean>;

  // Layout state.
  mode$: Observable<string>;
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
  currentState$: Observable<RavenState | null>;
  currentStateChanged$: Observable<boolean>;
  currentStateId$: Observable<string>;
  guides$: Observable<number[]>;
  hoveredBandId$: Observable<string>;
  lastClickTime$: Observable<number | null>;
  maxTimeRange$: Observable<TimeRange>;
  projectEpochsUrl$: Observable<string>;
  selectedBandId$: Observable<string>;
  selectedPoint$: Observable<RavenPoint | null>;
  selectedSubBand$: Observable<RavenSubBand | null>;
  selectedSubBandId$: Observable<string>;
  selectedSubBandPoints$: Observable<RavenPoint[]>;
  subBandSourceIdsByLabel$: Observable<StringTMap<string[]>>;
  viewTimeRange$: Observable<TimeRange>;

  // Local (non-Observable) state. Derived from store state.
  activityInitiallyHidden: boolean;
  bands: RavenCompositeBand[];
  baseUrl: string;
  detailsPanelHeight = 20;
  hoveredBandId: string;
  selectedBandId: string;
  selectedSubBand: RavenSubBand | null;
  selectedSubBandId: string;
  southPanelHeight = 20;
  heightChangeDelta = 0.1;
  treeBySourceId: StringTMap<RavenSource>;

  private subscriptions = new Subscription();

  constructor(
    private store: Store<TimelineState | ConfigState | TimeCursorState>,
  ) {
    // Config state.
    this.baseUrl$ = this.store.pipe(select(configSelectors.getBaseUrl));
    this.excludeActivityTypes$ = this.store.pipe(
      select(configSelectors.getExcludeActivityTypes),
    );
    this.itarMessage$ = this.store.pipe(select(configSelectors.getItarMessage));
    this.projectEpochsUrl$ = this.store.pipe(
      select(configSelectors.getProjectEpochsUrl),
    );
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
    this.epochsModified$ = this.store.pipe(
      select(epochsSelectors.getEpochsModified),
    );

    // Layout state.
    this.mode$ = this.store.pipe(select(layoutSelectors.getMode));
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
    this.currentState$ = this.store.pipe(
      select(timelineSelectors.getCurrentState),
    );
    this.currentStateChanged$ = this.store.pipe(
      select(timelineSelectors.getCurrentStateChanged),
    );
    this.currentStateId$ = this.store.pipe(
      select(timelineSelectors.getCurrentStateId),
    );
    this.guides$ = this.store.pipe(select(timelineSelectors.getGuides));
    this.hoveredBandId$ = this.store.pipe(
      select(timelineSelectors.getHoveredBandId),
    );
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
    this.selectedSubBandPoints$ = combineLatest([
      this.selectedSubBand$,
      this.excludeActivityTypes$,
    ]).pipe(
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
    this.subBandSourceIdsByLabel$ = combineLatest([
      this.bands$,
      this.customFiltersBySourceId$,
      this.filtersByTarget$,
      this.treeBySourceId$,
    ]).pipe(
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
    this.subscriptions.add(
      this.bands$.subscribe(bands => (this.bands = bands)),
    );
    this.subscriptions.add(
      this.baseUrl$.subscribe(baseUrl => (this.baseUrl = baseUrl)),
    );
    this.subscriptions.add(
      this.defaultBandSettings$.subscribe(
        defaultBandSettings =>
          (this.activityInitiallyHidden =
            defaultBandSettings.activityInitiallyHidden),
      ),
    );
    this.subscriptions.add(
      this.hoveredBandId$.subscribe(
        hoveredBandId => (this.hoveredBandId = hoveredBandId),
      ),
    );
    this.subscriptions.add(
      this.selectedBandId$.subscribe(
        selectedBandId => (this.selectedBandId = selectedBandId),
      ),
    );
    this.subscriptions.add(
      this.selectedSubBandId$.subscribe(
        selectedSubBandId => (this.selectedSubBandId = selectedSubBandId),
      ),
    );
    this.subscriptions.add(
      this.showDetailsPanel$.subscribe(
        showDetailsPanel =>
          (this.detailsPanelHeight = showDetailsPanel ? 20 : 0),
      ),
    );
    this.subscriptions.add(
      this.selectedSubBand$.subscribe(
        selectedSubBand => (this.selectedSubBand = selectedSubBand),
      ),
    );
    this.subscriptions.add(
      this.selectedSubBandId$.subscribe(
        selectedSubBandId => (this.selectedSubBandId = selectedSubBandId),
      ),
    );

    this.subscriptions.add(
      this.showSouthBandsPanel$.subscribe(
        showSouthBandsPanel =>
          (this.southPanelHeight = showSouthBandsPanel ? 20 : 0),
      ),
    );
    this.subscriptions.add(
      this.treeBySourceId$.subscribe(
        treeBySourceId => (this.treeBySourceId = treeBySourceId),
      ),
    );
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  getActivityFilter() {
    const band = subBandById(
      this.bands,
      this.selectedBandId,
      this.selectedSubBandId,
    );
    if (band && band.type === 'activity') {
      return (band as RavenActivityBand).activityFilter;
    } else {
      return '';
    }
  }

  /**
   * Helper. Calculate the main chart height.
   */
  getMainChartHeightPercent() {
    return 100 - this.southPanelHeight - this.detailsPanelHeight;
  }

  /**
   * Event. Called when filter activities is clicked.
   */
  onFilterActivityInSubBand(e: any) {
    if (e.bandId && e.subBandId) {
      this.store.dispatch(
        new timelineActions.UpdateSubBand(e.bandId, e.subBandId, {
          activityFilter: e.filter,
        }),
      );
      this.store.dispatch(
        new timelineActions.FilterActivityInSubBand(
          e.bandId,
          e.subBandId,
          e.filter,
          this.activityInitiallyHidden,
        ),
      );
    }
  }

  /**
   * Event. Called when a point in the Guide band is clicked.
   */
  onToggleGuide(e: RavenGuidePoint): void {
    this.store.dispatch(new timelineActions.ToggleGuide(e));
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

  onDeleteBand(bandId: string): void {
    const band = bandById(this.bands, bandId) as RavenCompositeBand;
    this.store.dispatch(new dialogActions.OpenDeleteBandDialog(band, '300px'));
  }

  onDecreaseBandHeight(bandId: string): void {
    const band = bandById(this.bands, bandId) as RavenCompositeBand;
    this.store.dispatch(
      new timelineActions.UpdateBand(bandId, {
        height: band.height * (1 - this.heightChangeDelta),
      }),
    );
  }

  onIncreaseBandHeight(bandId: string): void {
    const band = bandById(this.bands, bandId) as RavenCompositeBand;
    this.store.dispatch(
      new timelineActions.UpdateBand(bandId, {
        height: band.height * (1 + this.heightChangeDelta),
      }),
    );
  }

  /**
   * Event. Called when a `add-divider-band` event is fired.
   */
  onAddDividerBand(selectedBandId: string): void {
    this.store.dispatch(
      new timelineActions.AddBand(null, toCompositeBand(toDividerBand()), {
        afterBandId: selectedBandId,
      }),
    );
  }

  /**
   * Event. Called when a `epochError` event is fired.
   */
  onEpochError(message: string): void {
    this.store.dispatch(new toastActions.ShowToast('warning', message, ''));
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
   * Event. Called when a 'hover' event is fired from ctl.
   */
  onHoverBand(bandId: string) {
    this.store.dispatch(new timelineActions.HoverBand(bandId));
  }

  /**
   * Event. Called when a `import-epochs` event is fired from the raven-epochs component.
   */
  onImportEpochs(epochs: RavenEpoch[]) {
    this.store.dispatch(new epochsActions.AppendAndReplaceEpochs(epochs));
  }

  /**
   * Event. Called when a `AddPointToSubBand' event is fired from the raven-table component.
   */
  onAddPointToSubBand(e: RavenPointIndex) {
    this.store.dispatch(
      new timelineActions.AddPointAtIndex(
        this.selectedBandId,
        this.selectedSubBandId,
        e.point,
        e.index,
      ),
    );
  }

  /**
   * Event. Called when a `RemovePointsInSubBand' event is fired from the raven-table component.
   */
  onRemovePointsInSubBand(removePoints: RavenPoint[]) {
    this.store.dispatch(
      new timelineActions.MarkRemovePointsInSubBand(
        this.selectedBandId,
        this.selectedSubBandId,
        removePoints,
      ),
    );
  }

  /**
   * Event. Called when a `Save' event is fired from the raven-table component.
   */
  onSave() {
    if (this.selectedSubBand) {
      const source = this.treeBySourceId[this.selectedSubBand.sourceIds[0]];
      const csvHeaderMap = {};

      // Get the mapping data from the sourceUrl.
      let url = decodeURIComponent(source.url);
      url = url.replace(/[+]/g, ' ');
      const mapString = url.match(new RegExp('.*map=\\((.*)\\)&'));
      if (mapString) {
        const [, mapStr] = mapString;
        mapStr.split(',').forEach(keyValue => {
          const kv = keyValue.split('=');
          csvHeaderMap[kv[0]] = kv[1];
        });
      }
      this.store.dispatch(
        new timelineActions.UpdateCsvFile(
          this.selectedBandId,
          this.selectedSubBandId,
          this.selectedSubBand.sourceIds[0],
          this.selectedSubBand.points,
          csvHeaderMap,
        ),
      );
    }
  }

  /**
   * Event. Called when settings icon is clicked.
   */
  onSettingsBand(bandId: string): void {
    this.store.dispatch(new timelineActions.SelectBand(bandId));
    const band = bandById(this.bands, bandId) as RavenCompositeBand;
    this.store.dispatch(
      new dialogActions.OpenSettingsBandDialog(
        bandId,
        band.subBands[0].id,
        '300px',
      ),
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

  onUpdateAddTo(e: RavenUpdate): void {
    if (e.bandId && e.subBandId) {
      this.store.dispatch(new timelineActions.SelectBand(e.bandId));
      this.store.dispatch(
        new timelineActions.UpdateSubBand(e.bandId, e.subBandId, e.update),
      );
    }
  }

  onUpdateOverlay(e: RavenUpdate): void {
    if (e.bandId) {
      this.store.dispatch(new timelineActions.SelectBand(e.bandId));
      this.store.dispatch(new timelineActions.UpdateBand(e.bandId, e.update));
    }
  }

  /**
   * Event. Called when an `updateBandsActivityFilter` event is fired from the raven-settings-global component.
   */
  onUpdateBandsActivityFilter(filter: string) {
    this.store.dispatch(
      new timelineActions.UpdateAllActivityBandFilter(filter),
    );
  }

  /**
   * Event. Called when an `update-default-band-settings` event is fired from the raven-settings-global component.
   */
  onUpdateDefaultBandSettings(e: RavenUpdate): void {
    this.store.dispatch(new configActions.UpdateDefaultBandSettings(e.update));
  }

  onAddEpoch(epoch: RavenEpoch): void {
    this.store.dispatch(new epochsActions.AppendAndReplaceEpochs([epoch]));
  }

  onRemoveEpochs(epochs: RavenEpoch[]): void {
    this.store.dispatch(new epochsActions.RemoveEpochs(epochs));
  }

  onSaveNewEpochFile(): void {
    this.store.dispatch(new dialogActions.OpenSaveNewEpochFileDialog());
  }

  onSelectPoint(point: RavenPoint) {
    this.store.dispatch(
      new timelineActions.SelectPoint(
        this.selectedBandId,
        this.selectedSubBandId,
        point.uniqueId,
      ),
    );
  }

  onUpdateEpochData(e: any): void {
    this.store.dispatch(
      new epochsActions.UpdateEpochData(e.rowIndex, {
        name: e.name,
        selected: e.selected,
        value: e.value,
      }),
    );
  }

  /**
   * Event. Called when a `UpdatePoint' event is fired from the raven-table component.
   */
  onUpdatePoint(e: RavenPointUpdate) {
    this.store.dispatch(
      new timelineActions.UpdatePointInSubBand(
        e.bandId,
        e.subBandId,
        e.pointId,
        e.update,
      ),
    );
  }

  /**
   * Event. Called when an `update-epochs` event is fired from the raven-epochs component.
   */
  onUpdateEpochSetting(e: RavenUpdate): void {
    this.store.dispatch(new epochsActions.UpdateEpochSetting(e.update));
  }

  onUpdateProjectEpochs(): void {
    this.store.dispatch(new dialogActions.OpenUpdateProjectEpochsDialog());
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
  onUpdateViewTimeRange(viewTimeRange: TimeRange): void {
    this.store.dispatch(new timelineActions.UpdateViewTimeRange(viewTimeRange));
  }

  /**
   * Helper that dispatches a resize event.
   */
  resize() {
    this.store.dispatch(new layoutActions.Resize());
  }

  onPanLeft() {
    this.store.dispatch(new timelineActions.PanLeftViewTimeRange());
  }

  onPanRight() {
    this.store.dispatch(new timelineActions.PanRightViewTimeRange());
  }

  onPanTo(viewTimeRange: TimeRange) {
    this.store.dispatch(new timelineActions.UpdateViewTimeRange(viewTimeRange));
  }

  onResetView() {
    this.store.dispatch(new timelineActions.ResetViewTimeRange());
  }

  onZoomIn() {
    this.store.dispatch(new timelineActions.ZoomInViewTimeRange());
  }

  onZoomOut() {
    this.store.dispatch(new timelineActions.ZoomOutViewTimeRange());
  }

  onRemoveAllBands() {
    this.store.dispatch(new dialogActions.OpenRemoveAllBandsDialog('400px'));
  }

  onRemoveAllGuides() {
    this.store.dispatch(new dialogActions.OpenRemoveAllGuidesDialog('400px'));
  }

  onShareableLink() {
    this.store.dispatch(new dialogActions.OpenShareableLinkDialog('600px'));
  }

  onApplyCurrentState() {
    this.store.dispatch(new dialogActions.OpenApplyCurrentStateDialog());
  }

  onApplyCurrentLayout() {
    this.store.dispatch(new layoutActions.ToggleApplyLayoutDrawerEvent(true));
  }

  onUpdateCurrentState() {
    this.store.dispatch(new dialogActions.OpenUpdateCurrentStateDialog());
  }
}
