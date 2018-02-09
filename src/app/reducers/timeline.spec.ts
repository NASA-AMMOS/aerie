/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { initialState, reducer, TimelineState } from './timeline';

import {
  RavenActivityBand,
  RavenBand,
  RavenSource,
  RavenStateBand,
} from './../shared/models';

import {
  FetchGraphDataSuccess,
  RemoveBands,
} from './../actions/source-explorer';

import {
  SelectBand,
  SettingsUpdateAllBands,
  SettingsUpdateBand,
  SortBands,
  UpdateViewTimeRange,
} from './../actions/timeline';

import {
  activityBand,
  activityPoint,
  childSource,
  rootSource,
  stateBand,
} from './../shared/mocks';

describe('timeline reducer', () => {
  let timelineState: TimelineState;

  beforeEach(() => {
    timelineState = initialState;
  });

  it('handle default', () => {
    expect(timelineState).toEqual(initialState);
  });

  it('handle FetchGraphDataSuccess (no existing bands)', () => {
    const source: RavenSource = rootSource;
    const bands: RavenBand[] = [stateBand];
    const bandIdToName = {};
    const bandIdToPoints = {};

    timelineState = reducer(timelineState, new FetchGraphDataSuccess(source, bands, bandIdToName, bandIdToPoints));
    expect(timelineState).toEqual({
      ...initialState,
      bands: [{
        ...stateBand,
        containerId: '0',
        sortOrder: 0,
        sourceIds: {
          ...stateBand.sourceIds,
          [source.id]: source.name,
        },
      } as RavenStateBand],
      maxTimeRange: { end: 100, start: 0 },
      viewTimeRange: { end: 100, start: 0 },
    });
  });

  it('handle FetchGraphDataSuccess (to existing band)', () => {
    const source: RavenSource = rootSource;
    const child: RavenSource = childSource;
    const bands: RavenBand[] = [activityBand];
    const bandIdToName = { '100': 'test-activity-band' };
    const bandIdToPoints = { '100': [activityPoint] };

    // First add a band that we can add points to it in the next reducer call.
    timelineState = reducer(timelineState, new FetchGraphDataSuccess(source, bands, bandIdToName, bandIdToPoints));

    timelineState = reducer(timelineState, new FetchGraphDataSuccess(child, [], bandIdToName, bandIdToPoints));
    expect(timelineState).toEqual({
      ...initialState,
      bands: [{
        ...activityBand,
        points: [activityPoint],
        sourceIds: {
          ...activityBand.sourceIds,
          [source.id]: source.name,
          [child.id]: child.name,
        },
      } as RavenActivityBand],
      maxTimeRange: { end: 200, start: 50 },
      viewTimeRange: { end: 200, start: 50 },
    });
  });

  it('handle RemoveBands (remove entire band)', () => {
    const source: RavenSource = rootSource;
    const bands: RavenBand[] = [activityBand];
    const bandIdToName = {};
    const bandIdToPoints = {};

    // First add a band so we can remove it.
    timelineState = reducer(timelineState, new FetchGraphDataSuccess(source, bands, bandIdToName, bandIdToPoints));

    timelineState = reducer(timelineState, new RemoveBands(source, ['100'], []));
    expect(timelineState).toEqual(initialState);
  });

  it('handle RemoveBands (remove points from band)', () => {
    const source: RavenSource = rootSource;
    const child: RavenSource = childSource;
    const bands: RavenBand[] = [activityBand];
    const bandIdToName = { '100': 'test-activity-band' };

    activityPoint.sourceId = child.id;
    const bandIdToPoints = { '100': [activityPoint] };

    // First add a band with extra points that we can remove.
    timelineState = reducer(timelineState, new FetchGraphDataSuccess(source, bands, bandIdToName, bandIdToPoints));
    timelineState = reducer(timelineState, new FetchGraphDataSuccess(child, [], bandIdToName, bandIdToPoints));

    timelineState = reducer(timelineState, new RemoveBands(child, [], ['100']));

    expect(timelineState).toEqual({
      ...initialState,
      bands: [{
        ...activityBand,
        points: [],
        sortOrder: 0,
        sourceIds: {
          ...activityBand.sourceIds,
          [source.id]: source.name,
        },
      } as RavenActivityBand],
      maxTimeRange: { end: 200, start: 50 },
      viewTimeRange: { end: 200, start: 50 },
    });
  });

  it('handle SelectBand', () => {
    const source: RavenSource = rootSource;
    const bands: RavenBand[] = [stateBand];
    const bandIdToName = {};
    const bandIdToPoints = {};

    // First add a band we can select.
    const timelineStateWithBand = reducer(timelineState, new FetchGraphDataSuccess(source, bands, bandIdToName, bandIdToPoints));

    timelineState = reducer(timelineStateWithBand, new SelectBand(stateBand.id));
    expect(timelineState).toEqual({
      ...timelineStateWithBand,
      selectedBandId: stateBand.id,
    });
  });

  it('handle SettingsUpdateAllBands', () => {
    timelineState = reducer(timelineState, new SettingsUpdateAllBands('labelWidth', 200));
    expect(timelineState).toEqual({
      ...initialState,
      labelWidth: 200,
    });
  });

  it('handle SettingsUpdateBand (with no selected band)', () => {
    timelineState = reducer(timelineState, new SettingsUpdateBand('label', '42'));
    expect(timelineState).toEqual({ ...initialState });
  });

  it('handle SettingsUpdateBand', () => {
    const source: RavenSource = rootSource;
    const bands: RavenBand[] = [stateBand];
    const bandIdToName = {};
    const bandIdToPoints = {};

    // First add a band and select it so we can update it.
    let timelineStateWithBand = reducer(timelineState, new FetchGraphDataSuccess(source, bands, bandIdToName, bandIdToPoints));
    timelineStateWithBand = reducer(timelineStateWithBand, new SelectBand(stateBand.id));

    timelineState = reducer(timelineStateWithBand, new SettingsUpdateBand('label', '42'));
    expect(timelineState).toEqual({
      ...timelineStateWithBand,
      bands: [{
        ...timelineStateWithBand.bands[0],
        label: '42',
      }],
    });
  });

  it('handle SortBands', () => {
    const source: RavenSource = rootSource;
    const bands: RavenBand[] = [activityBand, stateBand];
    const sort = {
      '100': { containerId: '0', sortOrder: 1 },
      '102': { containerId: '0', sortOrder: 0 },
    };

    const timelineStateWithBands = reducer(timelineState, new FetchGraphDataSuccess(source, bands, {}, {}));

    timelineState = reducer(timelineStateWithBands, new SortBands(sort));
    expect(timelineState).toEqual({
      ...timelineStateWithBands,
      bands: [
        {
          ...timelineStateWithBands.bands[0],
          containerId: '0',
          sortOrder: 1,
        },
        {
          ...timelineStateWithBands.bands[1],
          containerId: '0',
          sortOrder: 0,
        },
      ],
    });
  });

  it('handle UpdateViewTimeRange', () => {
    timelineState = reducer(timelineState, new UpdateViewTimeRange({ end: 314, start: 272 }));
    expect(timelineState).toEqual({
      ...initialState,
      viewTimeRange: { end: 314, start: 272 },
    });
  });
});
