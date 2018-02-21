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
  RavenSource,
} from './../shared/models';

import {
  FetchGraphDataSuccess,
  RemoveBands,
} from './../actions/source-explorer';

import {
  SelectBand,
  SettingsUpdateAllBands,
  SettingsUpdateBand,
  SettingsUpdateSubBand,
  SortBands,
  UpdateViewTimeRange,
} from './../actions/timeline';

import {
  activityBand,
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

    timelineState = reducer(timelineState, new FetchGraphDataSuccess(source, [stateBand]));

    const band = { ...timelineState.bands[0] };
    band.bands = [{
      ...stateBand,
      parentUniqueId: band.id,
      sourceId: source.id,
      sourceName: source.name,
    }];

    expect(timelineState).toEqual({
      ...initialState,
      bands: [band],
      maxTimeRange: { end: 100, start: 0 },
      viewTimeRange: { end: 100, start: 0 },
    });
  });

  it('handle FetchGraphDataSuccess (to existing band)', () => {
    const source: RavenSource = rootSource;
    const child: RavenSource = childSource;

    const activityBand1 = {
      ...activityBand,
      id: '400',
      legend: 'a',
    };

    const activityBand2 = {
      ...activityBand,
      id: '500',
      legend: 'a',
    };

    timelineState = reducer(timelineState, new FetchGraphDataSuccess(source, [activityBand1]));
    timelineState = reducer(timelineState, new FetchGraphDataSuccess(child, [activityBand2]));

    const band = { ...timelineState.bands[0] };
    band.bands = [
      {
        ...activityBand1,
        id: '400',
        parentUniqueId: band.id,
        sourceId: source.id,
        sourceName: source.name,
      },
      {
        ...activityBand2,
        id: '500',
        parentUniqueId: band.id,
        sourceId: child.id,
        sourceName: child.name,
      }
    ];

    expect(timelineState).toEqual({
      ...initialState,
      bands: [band],
      maxTimeRange: { end: 200, start: 50 },
      viewTimeRange: { end: 200, start: 50 },
    });
  });

  it('handle RemoveBands (remove entire band)', () => {
    const source: RavenSource = rootSource;

    // First add a band so we can remove it.
    timelineState = reducer(timelineState, new FetchGraphDataSuccess(source, [activityBand]));

    timelineState = reducer(timelineState, new RemoveBands(source, ['100']));
    expect(timelineState).toEqual(initialState);
  });

  it('handle RemoveBands (remove points from band)', () => {
    const source: RavenSource = rootSource;
    const child: RavenSource = childSource;

    const activityBand1 = { ...activityBand };
    const activityBand2 = { ...activityBand };

    activityBand1.id = '400';
    activityBand2.id = '500';

    timelineState = reducer(timelineState, new FetchGraphDataSuccess(source, [activityBand1]));
    timelineState = reducer(timelineState, new FetchGraphDataSuccess(child, [activityBand2]));
    timelineState = reducer(timelineState, new RemoveBands(child, ['400']));

    const band = { ...timelineState.bands[0] };
    band.bands = [
      {
        ...activityBand2,
        id: '500',
        parentUniqueId: band.id,
        sourceId: child.id,
        sourceName: child.name,
      }
    ];

    expect(timelineState).toEqual({
      ...initialState,
      bands: [band],
      maxTimeRange: { end: 200, start: 50 },
      viewTimeRange: { end: 200, start: 50 },
    });
  });

  it('handle SelectBand', () => {
    const source: RavenSource = rootSource;

    // First add a band we can select.
    const timelineStateWithBand = reducer(timelineState, new FetchGraphDataSuccess(source, [stateBand]));

    const band = { ...timelineState.bands[0] };

    timelineState = reducer(timelineStateWithBand, new SelectBand(band.id));
    expect(timelineState).toEqual({
      ...timelineStateWithBand,
      selectedBandId: band.id,
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
    timelineState = reducer(timelineState, new SettingsUpdateBand('', 'label', '42'));
    expect(timelineState).toEqual({ ...initialState });
  });

  it('handle SettingsUpdateBand', () => {
    const source: RavenSource = rootSource;

    // First add a band and select it so we can update it.
    let timelineStateWithBand = reducer(timelineState, new FetchGraphDataSuccess(source, [stateBand]));
    const band = { ...timelineStateWithBand.bands[0] };

    timelineStateWithBand = reducer(timelineStateWithBand, new SelectBand(band.id));
    timelineState = reducer(timelineStateWithBand, new SettingsUpdateBand(band.id, 'height', 42));

    expect(timelineState).toEqual({
      ...timelineStateWithBand,
      bands: [{
        ...timelineStateWithBand.bands[0],
        height: 42
      }],
    });
  });

  it('handle SettingsUpdateSubBand', () => {
    const source: RavenSource = rootSource;

    // First add a band and select it so we can update it.
    let timelineStateWithBand = reducer(timelineState, new FetchGraphDataSuccess(source, [stateBand]));
    const band = { ...timelineStateWithBand.bands[0] };
    const subBand = { ...band.bands[0] };

    timelineStateWithBand = reducer(timelineStateWithBand, new SelectBand(band.id));
    timelineState = reducer(timelineStateWithBand, new SettingsUpdateSubBand(band.id, subBand.id, 'label', '42'));

    expect(timelineState).toEqual({
      ...timelineStateWithBand,
      bands: [{
        ...timelineStateWithBand.bands[0],
        bands: [{
          ...timelineStateWithBand.bands[0].bands[0],
          label: '42',
        }],
      }],
    });
  });

  it('handle SortBands', () => {
    const source: RavenSource = rootSource;
    const stateBand1 = { ...stateBand };
    const stateBand2 = { ...stateBand };

    stateBand1.id = '400';
    stateBand2.id = '500';

    const timelineStateWithBands = reducer(timelineState, new FetchGraphDataSuccess(source, [stateBand1, stateBand2]));

    const band0 = { ...timelineStateWithBands.bands[0] };
    const band1 = { ...timelineStateWithBands.bands[1] };

    const sort = {
      [band0.id]: { containerId: '0', sortOrder: 1 },
      [band1.id]: { containerId: '0', sortOrder: 0 },
    };

    timelineState = reducer(timelineStateWithBands, new SortBands(sort));
    expect(timelineState).toEqual({
      ...timelineStateWithBands,
      bands: [
        {
          ...band0,
          containerId: '0',
          sortOrder: 1,
        },
        {
          ...band1,
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
