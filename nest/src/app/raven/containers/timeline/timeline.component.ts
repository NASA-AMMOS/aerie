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
import { ConfigActions, ToastActions } from '../../../shared/actions';
import { StringTMap, TimeRange } from '../../../shared/models';
import * as configSelectors from '../../../shared/selectors/config.selectors';
import {
  DialogActions,
  EpochsActions,
  LayoutActions,
  OutputActions,
  SituationalAwarenessActions,
  SourceExplorerActions,
  TimeCursorActions,
  TimelineActions,
} from '../../actions';
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
  defaultDividerHeight: number;
  defaultDividerColor: string;
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
      this.defaultBandSettings$.subscribe(defaultBandSettings => {
        this.activityInitiallyHidden =
          defaultBandSettings.activityInitiallyHidden;
        this.defaultDividerColor = defaultBandSettings.dividerColor;
        this.defaultDividerHeight = defaultBandSettings.dividerHeight;
      }),
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
        TimelineActions.updateSubBand({
          bandId: e.bandId,
          subBandId: e.subBandId,
          update: { activityFilter: e.filter },
        }),
      );
      this.store.dispatch(
        TimelineActions.filterActivityInSubBand({
          activityInitiallyHidden: this.activityInitiallyHidden,
          bandId: e.bandId,
          filter: e.filter,
          subBandId: e.subBandId,
        }),
      );
    }
  }

  /**
   * Event. Called when a point in the Guide band is clicked.
   */
  onToggleGuide(guide: RavenGuidePoint): void {
    this.store.dispatch(TimelineActions.toggleGuide({ guide }));
  }

  /**
   * Event. Called when an `apply-layout` event is fired from the raven-layout-apply component.
   */
  onApplyLayout(update: RavenApplyLayoutUpdate): void {
    this.store.dispatch(SourceExplorerActions.applyLayout({ update }));
  }

  /**
   * Event. Called when an `apply-state` event is fired from the raven-layout-apply component.
   */
  onApplyState(source: RavenSource): void {
    this.store.dispatch(
      SourceExplorerActions.applyState({
        sourceId: source.id,
        sourceUrl: source.url,
      }),
    );
  }

  /**
   * Event. Called when a band is left clicked in a raven-bands component.
   */
  onBandLeftClick(e: RavenBandLeftClick): void {
    this.store.dispatch(TimelineActions.selectBand({ bandId: e.bandId }));
    this.store.dispatch(TimelineActions.updateLastClickTime({ time: e.time }));

    if (e.subBandId && e.pointId) {
      this.store.dispatch(
        TimelineActions.selectPoint({
          bandId: e.bandId,
          pointId: e.pointId,
          subBandId: e.subBandId,
        }),
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
      TimelineActions.removeChildrenOrDescendants({
        activityPoint: e.activityPoint,
        bandId: this.selectedBandId,
        subBandId: this.selectedSubBandId,
      }),
    );
    if (e.expansion !== 'noExpansion') {
      this.store.dispatch(
        TimelineActions.expandChildrenOrDescendants({
          activityPoint: e.activityPoint,
          bandId: this.selectedBandId,
          expandType: e.expansion,
          subBandId: this.selectedSubBandId,
        }),
      );
      this.store.dispatch(
        TimelineActions.fetchChildrenOrDescendants({
          activityPoint: e.activityPoint,
          bandId: this.selectedBandId,
          expandType: e.expansion,
          subBandId: this.selectedSubBandId,
        }),
      );
    }
  }

  /**
   * Event. Called when an `change-situational-awareness` event is fired
   * from the raven-situational-awareness component.
   */
  onChangeSituationalAwareness(situAware: boolean): void {
    this.store.dispatch(
      SituationalAwarenessActions.changeSituationalAwareness({
        situAware,
        url: `${this.baseUrl}/mpsserver/api/v2/situational_awareness?`,
      }),
    );
  }

  /**
   * Event. Called when a `create-output` event is fired from the raven-output component.
   */
  onCreateOutput(): void {
    this.store.dispatch(OutputActions.createOutput());
  }

  onDeleteBand(bandId: string): void {
    const band = bandById(this.bands, bandId) as RavenCompositeBand;
    this.store.dispatch(
      DialogActions.openDeleteBandDialog({ band, width: '300px' }),
    );
  }

  onDecreaseBandHeight(bandId: string): void {
    const band = bandById(this.bands, bandId) as RavenCompositeBand;
    const newHeight = band.height * (1 - this.heightChangeDelta);
    this.store.dispatch(
      TimelineActions.updateBand({
        bandId,
        update: {
          height: newHeight,
        },
      }),
    );

    // Decrease all subBand height.
    for (let i = 0, l = band.subBands.length; i < l; ++i) {
      this.store.dispatch(
        TimelineActions.updateSubBand({
          bandId,
          subBandId: band.subBands[i].id,
          update: {
            height: newHeight,
          },
        }),
      );
    }
  }

  onIncreaseBandHeight(bandId: string): void {
    const band = bandById(this.bands, bandId) as RavenCompositeBand;
    const newHeight = band.height * (1 + this.heightChangeDelta);
    this.store.dispatch(
      TimelineActions.updateBand({
        bandId,
        update: {
          height: newHeight,
        },
      }),
    );

    // Increase all subBand height.
    for (let i = 0, l = band.subBands.length; i < l; ++i) {
      this.store.dispatch(
        TimelineActions.updateSubBand({
          bandId,
          subBandId: band.subBands[i].id,
          update: {
            height: newHeight,
          },
        }),
      );
    }
  }

  /**
   * Event. Called when a `add-divider-band` event is fired.
   */
  onAddDividerBand(selectedBandId: string): void {
    this.store.dispatch(
      TimelineActions.addBand({
        band: toCompositeBand(
          toDividerBand(this.defaultDividerHeight, this.defaultDividerColor),
          undefined,
          undefined,
          this.defaultDividerColor,
        ),
        modifiers: {
          afterBandId: selectedBandId,
        },
        sourceId: null,
      }),
    );
  }

  /**
   * Event. Called when a `epochError` event is fired.
   */
  onEpochError(message: string): void {
    this.store.dispatch(
      ToastActions.showToast({ toastType: 'warning', message, title: '' }),
    );
  }

  /**
   * Event. Called when a `change-time-cursor` event is fired from the raven-time-cursor component.
   */
  onDisplayTimeCursor(show: boolean): void {
    if (show) {
      this.store.dispatch(TimeCursorActions.showTimeCursor());
    } else {
      this.store.dispatch(TimeCursorActions.hideTimeCursor());
    }
  }

  /**
   * Event. Called when a 'hover' event is fired from ctl.
   */
  onHoverBand(bandId: string) {
    this.store.dispatch(TimelineActions.hoverBand({ bandId }));
  }

  /**
   * Event. Called when a `import-epochs` event is fired from the raven-epochs component.
   */
  onImportEpochs(epochs: RavenEpoch[]) {
    this.store.dispatch(EpochsActions.appendAndReplaceEpochs({ epochs }));
  }

  /**
   * Event. Called when a `AddPointToSubBand' event is fired from the raven-table component.
   */
  onAddPointToSubBand(e: RavenPointIndex) {
    this.store.dispatch(
      TimelineActions.addPointAtIndex({
        bandId: this.selectedBandId,
        index: e.index,
        point: e.point,
        subBandId: this.selectedSubBandId,
      }),
    );
  }

  /**
   * Event. Called when a `RemovePointsInSubBand' event is fired from the raven-table component.
   */
  onRemovePointsInSubBand(removePoints: RavenPoint[]) {
    this.store.dispatch(
      TimelineActions.markRemovePointsInSubBand({
        bandId: this.selectedBandId,
        points: removePoints,
        subBandId: this.selectedSubBandId,
      }),
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
        TimelineActions.updateCsvFile({
          bandId: this.selectedBandId,
          csvHeaderMap,
          points: this.selectedSubBand.points,
          sourceId: this.selectedSubBand.sourceIds[0],
          subBandId: this.selectedSubBandId,
        }),
      );
    }
  }

  /**
   * Event. Called when settings icon is clicked.
   */
  onSettingsBand(bandId: string): void {
    this.store.dispatch(TimelineActions.selectBand({ bandId }));
    const band = bandById(this.bands, bandId) as RavenCompositeBand;
    this.store.dispatch(
      DialogActions.openSettingsBandDialog({
        bandId,
        subBandId: band.subBands[0].id,
        width: '300px',
      }),
    );
  }

  /**
   * Event. Called when a `new-sort` event is fired from raven-bands.
   */
  onSort(sort: StringTMap<RavenSortMessage>): void {
    this.store.dispatch(TimelineActions.sortBands({ sort }));
    this.store.dispatch(LayoutActions.resize());
  }

  /**
   * Event. Called when a toggle event is fired from the apply layout drawer.
   */
  onToggleApplyLayoutDrawer(opened?: boolean) {
    this.store.dispatch(LayoutActions.toggleApplyLayoutDrawer({ opened }));
  }

  /**
   * Event. Called when a toggle event is fired from the epochs drawer.
   */
  onToggleEpochsDrawer(opened?: boolean) {
    this.store.dispatch(LayoutActions.toggleEpochsDrawer({ opened }));
  }

  /**
   * Event. Called when a toggle event is fired from the epochs drawer.
   */
  onToggleTimeCursorDrawer(opened?: boolean) {
    this.store.dispatch(LayoutActions.toggleTimeCursorDrawer({ opened }));
  }

  /**
   * Event. Called when a toggle event is fired from the global settings drawer.
   */
  onToggleGlobalSettingsDrawer(opened?: boolean) {
    this.store.dispatch(LayoutActions.toggleGlobalSettingsDrawer({ opened }));
  }

  /**
   * Event. Called when a toggle event is fired from the output drawer.
   */
  onToggleOutputDrawer(opened?: boolean) {
    this.store.dispatch(LayoutActions.toggleOutputDrawer({ opened }));
  }

  /**
   * Event. Called when a `toggle-show-activity-point-metadata` event is fired from a raven-activity-point.
   */
  onToggleShowActivityPointMetadata(show: boolean) {
    this.store.dispatch(
      LayoutActions.updateLayout({
        update: { showActivityPointMetadata: show },
      }),
    );
  }

  /**
   * Event. Called when a `toggle-show-activity-point-parameters` event is fired from a raven-activity-point.
   */
  onToggleShowActivityPointParameters(show: boolean) {
    this.store.dispatch(
      LayoutActions.updateLayout({
        update: { showActivityPointParameters: show },
      }),
    );
  }

  /**
   * Event. Called when a toggle event is fired from the situational awareness drawer.
   */
  onToggleSituationalAwarenessDrawer(opened?: boolean) {
    this.store.dispatch(
      LayoutActions.toggleSituationalAwarenessDrawer({ opened }),
    );
  }

  onUpdateAddTo(e: RavenUpdate): void {
    if (e.bandId && e.subBandId) {
      this.store.dispatch(TimelineActions.selectBand({ bandId: e.bandId }));
      this.store.dispatch(
        TimelineActions.updateSubBand({
          bandId: e.bandId,
          subBandId: e.subBandId,
          update: e.update,
        }),
      );
    }
  }

  onUpdateOverlay(e: RavenUpdate): void {
    if (e.bandId) {
      this.store.dispatch(TimelineActions.selectBand({ bandId: e.bandId }));
      this.store.dispatch(
        TimelineActions.updateBand({ bandId: e.bandId, update: e.update }),
      );
    }
  }

  /**
   * Event. Called when an `updateBandsActivityFilter` event is fired from the raven-settings-global component.
   */
  onUpdateBandsActivityFilter(filter: string) {
    this.store.dispatch(
      TimelineActions.updateAllActivityBandFilter({ filter }),
    );
  }

  /**
   * Event. Called when an `update-default-band-settings` event is fired from the raven-settings-global component.
   */
  onUpdateDefaultBandSettings(e: RavenUpdate): void {
    this.store.dispatch(
      ConfigActions.updateDefaultBandSettings({ update: e.update }),
    );
  }

  onAddEpoch(epoch: RavenEpoch): void {
    this.store.dispatch(
      EpochsActions.appendAndReplaceEpochs({ epochs: [epoch] }),
    );
  }

  onRemoveEpochs(epochs: RavenEpoch[]): void {
    this.store.dispatch(EpochsActions.removeEpochs({ epochs }));
  }

  onSaveNewEpochFile(): void {
    this.store.dispatch(DialogActions.openSaveNewEpochFileDialog());
  }

  /**
   * Event. Called when an `select-point` event is fired from the raven-table component.
   */
  onSelectPoint(point: RavenPoint) {
    this.store.dispatch(
      TimelineActions.selectPoint({
        bandId: this.selectedBandId,
        pointId: point.uniqueId,
        subBandId: this.selectedSubBandId,
      }),
    );
  }

  onUpdateEpochData(e: any): void {
    this.store.dispatch(
      EpochsActions.updateEpochData({
        data: {
          name: e.name,
          selected: e.selected,
          value: e.value,
        },
        index: e.rowIndex,
      }),
    );
  }

  /**
   * Event. Called when a `UpdatePoint' event is fired from the raven-table component.
   */
  onUpdatePoint(e: RavenPointUpdate) {
    this.store.dispatch(
      TimelineActions.updatePointInSubBand({
        bandId: e.bandId,
        pointId: e.pointId,
        subBandId: e.subBandId,
        update: e.update,
      }),
    );
  }

  /**
   * Event. Called when an `update-epochs` event is fired from the raven-epochs component.
   */
  onUpdateEpochSetting(e: RavenUpdate): void {
    this.store.dispatch(EpochsActions.updateEpochSetting({ update: e.update }));
  }

  onUpdateProjectEpochs(): void {
    this.store.dispatch(DialogActions.openUpdateProjectEpochsDialog());
  }

  /**
   * Event. Called when an `update-output-settings` event is fired from the raven-output component.
   */
  onUpdateOutputSettings(e: RavenUpdate): void {
    this.store.dispatch(
      OutputActions.updateOutputSettings({ update: e.update }),
    );
  }

  /**
   * Event. Called when an `update-situational-awareness-settings` event is fired from the raven-situational-awareness component.
   */
  onUpdateSituationalAwarenessSettings(e: RavenUpdate): void {
    this.store.dispatch(
      SituationalAwarenessActions.updateSituationalAwarenessSettings({
        update: e.update,
      }),
    );
  }

  /**
   * Event. Called when an `update-sub-band` event is fired from the raven-settings component.
   */
  onUpdateSubBand(e: RavenUpdate): void {
    if (e.bandId && e.subBandId) {
      this.store.dispatch(
        TimelineActions.updateSubBand({
          bandId: e.bandId,
          subBandId: e.subBandId,
          update: e.update,
        }),
      );
    }
  }

  /**
   * Event. Called when an `update-time-cursor-settings` event is fired from the raven-time-cursor component.
   */
  onUpdateTimeCursorSettings(e: RavenUpdate): void {
    this.store.dispatch(
      TimeCursorActions.updateTimeCursorSettings({ update: e.update }),
    );
  }

  /**
   * Event. Called when an `update-timeline` event is fired from the raven-settings component.
   */
  onUpdateTimeline(e: RavenUpdate): void {
    this.store.dispatch(TimelineActions.updateTimeline({ update: e.update }));
  }

  /**
   * Event. Called when catching an `update-view-time-range` event.
   */
  onUpdateViewTimeRange(viewTimeRange: TimeRange): void {
    this.store.dispatch(TimelineActions.updateViewTimeRange({ viewTimeRange }));
  }

  /**
   * Helper that dispatches a resize event.
   */
  resize() {
    this.store.dispatch(LayoutActions.resize());
  }

  onPanLeft() {
    this.store.dispatch(TimelineActions.panLeftViewTimeRange());
  }

  onPanRight() {
    this.store.dispatch(TimelineActions.panRightViewTimeRange());
  }

  onPanTo(viewTimeRange: TimeRange) {
    this.store.dispatch(TimelineActions.updateViewTimeRange({ viewTimeRange }));
  }

  onResetView() {
    this.store.dispatch(TimelineActions.resetViewTimeRange());
  }

  onZoomIn() {
    this.store.dispatch(TimelineActions.zoomInViewTimeRange());
  }

  onZoomOut() {
    this.store.dispatch(TimelineActions.zoomOutViewTimeRange());
  }

  onRemoveAllBands() {
    this.store.dispatch(
      DialogActions.openRemoveAllBandsDialog({ width: '400px' }),
    );
  }

  onRemoveAllGuides() {
    this.store.dispatch(
      DialogActions.openRemoveAllGuidesDialog({ width: '400px' }),
    );
  }

  onShareableLink() {
    this.store.dispatch(
      DialogActions.openShareableLinkDialog({ width: '600px' }),
    );
  }

  onApplyCurrentState() {
    this.store.dispatch(DialogActions.openApplyCurrentStateDialog());
  }

  onApplyCurrentLayout() {
    this.store.dispatch(
      LayoutActions.toggleApplyLayoutDrawerEvent({ opened: true }),
    );
  }

  onUpdateCurrentState() {
    this.store.dispatch(DialogActions.openUpdateCurrentStateDialog());
  }

  onContextMenuSelect(item: string) {
    console.log(`selected ${item}`);
  }

  zoomTo() {

  }
}
