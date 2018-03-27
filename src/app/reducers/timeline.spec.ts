/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  initialState,
  reducer,
  TimelineState,
} from './timeline';

import {
  RavenSource,
} from './../shared/models';

import {
  AddBand,
  AddPointsToSubBand,
  AddSubBand,
  RemoveBandsOrPointsForSource,
  RemoveSubBand,
  SelectBand,
  SortBands,
  UpdateBand,
  UpdateSubBand,
  UpdateTimeline,
  UpdateViewTimeRange,
} from './../actions/timeline';

import {
  activityBand,
  activityPoint,
  compositeBand,
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

  it('handle AddBand', () => {
    const source: RavenSource = rootSource;
    const newBand = {
      ...compositeBand,
      subBands: [{
        ...stateBand,
      }],
    };
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));

    expect(timelineState).toEqual({
      ...initialState,
      bands: [{
        ...compositeBand,
        containerId: '0',
        sortOrder: 0,
        subBands: [{
          ...stateBand,
          parentUniqueId: compositeBand.id,
          sourceIds: {
            [source.id]: source.id,
          },
        }],
      }],
      maxTimeRange: { end: 100, start: 0 },
      viewTimeRange: { end: 100, start: 0 },
    });
  });

  it('handle AddPointsToSubBand', () => {
    const source: RavenSource = rootSource;
    const newBand = {
      ...compositeBand,
      subBands: [{
        ...activityBand,
      }],
    };

    // First add band to state so we have something to add points to.
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));

    expect(timelineState.bands[0].subBands[0].points).toEqual([]);
    timelineState = reducer(timelineState, new AddPointsToSubBand(source.id, newBand.id, activityBand.id, [activityPoint]));
    expect(timelineState.bands[0].subBands[0].points).toEqual([activityPoint]);
  });

  it('handle AddSubBand', () => {
    const source: RavenSource = rootSource;
    const newBand = {
      ...compositeBand,
      subBands: [{
        ...stateBand,
      }],
    };

    // First add band to state so we have something to add a sub-band to.
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));

    expect(timelineState.bands[0].subBands.length).toEqual(1);
    timelineState = reducer(timelineState, new AddSubBand(source.id, newBand.id, activityBand));
    expect(timelineState.bands[0].subBands.length).toEqual(2);
    expect(timelineState).toEqual({
      ...initialState,
      bands: [{
        ...compositeBand,
        containerId: '0',
        sortOrder: 0,
        subBands: [
          {
            ...stateBand,
            parentUniqueId: compositeBand.id,
            sourceIds: {
              [source.id]: source.id,
            },
          },
          {
            ...activityBand,
            parentUniqueId: compositeBand.id,
            sourceIds: {
              [source.id]: source.id,
            },
          },
        ],
      }],
      maxTimeRange: { end: 200, start: 0 },
      viewTimeRange: { end: 100, start: 0 },
    });
  });

  it('handle RemoveBandsOrPointsForSource', () => {
    const source: RavenSource = rootSource;

    const band = {
      ...compositeBand,
      id: '0',
      subBands: [{
        ...activityBand,
        id: '1',
        parentUniqueId: '0',
        points: [
          {
            ...activityPoint,
            id: '100',
            sourceId: '/',
          },
          {
            ...activityPoint,
            id: '101',
            sourceId: '/child',
          },
        ],
        sourceIds: {
          '/': '/',
          '/child': '/child',
        },
      }],
    };

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    expect(timelineState.bands[0].subBands[0].points.length).toEqual(2);

    timelineState = reducer(timelineState, new RemoveBandsOrPointsForSource('/'));
    expect(timelineState.bands[0].subBands[0].points[0].id).toEqual('101');
    expect(timelineState.bands[0].subBands[0].points.length).toEqual(1);

    timelineState = reducer(timelineState, new RemoveBandsOrPointsForSource('/child'));
    expect(timelineState.bands.length).toEqual(0);
  });

  it('handle RemoveSubBand', () => {
    const source: RavenSource = rootSource;
    const band = {
      ...compositeBand,
      id: '0',
      subBands: [
        {
          ...stateBand,
          id: '1',
          parentUniqueId: '0',
          sourceIds: {
            '/': '/',
          },
        },
        {
          ...activityBand,
          id: '2',
          parentUniqueId: '0',
          sourceIds: {
            '/': '/',
          },
        },
      ],
    };

    // First add a band with some sub-bands so we can remove them.
    timelineState = reducer(timelineState, new AddBand(source.id, band));
    expect(timelineState.bands[0].subBands.length).toEqual(2);
    expect(timelineState.bands[0].subBands[0].id).toEqual('1');
    expect(timelineState.bands[0].subBands[1].id).toEqual('2');

    timelineState = reducer(timelineState, new RemoveSubBand('1'));
    expect(timelineState.bands[0].subBands.length).toEqual(1);
    expect(timelineState.bands[0].subBands[0].id).toEqual('2');

    timelineState = reducer(timelineState, new RemoveSubBand('2'));
    expect(timelineState.bands.length).toEqual(0);
  });

  it('handle SelectBand', () => {
    const source: RavenSource = rootSource;

    const newBand = {
      ...compositeBand,
      id: '0',
      subBands: [{
        ...stateBand,
        id: '1',
        parentUniqueId: '0',
        sourceIds: {
          '/': '/',
        },
      }],
    };

    expect(timelineState.selectedBandId).toEqual('');
    expect(timelineState.selectedSubBandId).toEqual('');

    // Add a band and select it.
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));
    timelineState = reducer(timelineState, new SelectBand('0'));

    expect(timelineState.selectedBandId).toEqual('0');
    expect(timelineState.selectedSubBandId).toEqual('1');
  });

  it('handle SortBands', () => {
    const source: RavenSource = rootSource;

    const stateBand1 = {
      ...compositeBand,
      id: '0',
      subBands: [{
        ...stateBand,
        id: '400',
        parentUniqueId: '0',
        sourceIds: {
          '/': '/',
        },
      }],
    };

    const stateBand2 = {
      ...compositeBand,
      id: '1',
      subBands: [{
        ...stateBand,
        id: '500',
        parentUniqueId: '1',
        sourceIds: {
          '/': '/',
        },
      }],
    };

    // First add some bands we can sort.
    timelineState = reducer(timelineState, new AddBand(source.id, stateBand1));
    timelineState = reducer(timelineState, new AddBand(source.id, stateBand2));

    const sort = {
      [stateBand1.id]: { containerId: '0', sortOrder: 1 },
      [stateBand2.id]: { containerId: '1', sortOrder: 0 },
    };

    timelineState = reducer(timelineState, new SortBands(sort));

    expect(timelineState).toEqual({
      ...timelineState,
      bands: [
        {
          ...stateBand1,
          containerId: '0',
          sortOrder: 1,
        },
        {
          ...stateBand2,
          containerId: '1',
          sortOrder: 0,
        },
      ],
    });
  });

  it('handle UpdateBand', () => {
    const source: RavenSource = rootSource;

    const band = {
      ...compositeBand,
      height: 50,
      id: '0',
      subBands: [{
        ...stateBand,
        id: '1',
        parentUniqueId: '0',
        sourceIds: {
          '/': '/',
        },
      }],
    };

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    expect(timelineState.bands[0].height).toEqual(50);

    timelineState = reducer(timelineState, new UpdateBand(band.id, { height: 42 }));
    expect(timelineState.bands[0].height).toEqual(42);
  });

  it('handle UpdateSubBand', () => {
    const source: RavenSource = rootSource;

    const band = {
      ...compositeBand,
      height: 50,
      id: '0',
      subBands: [{
        ...stateBand,
        height: 50,
        id: '1',
        parentUniqueId: '0',
        sourceIds: {
          '/': '/',
        },
      }],
    };

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    expect(timelineState.bands[0].subBands[0].height).toEqual(50);

    timelineState = reducer(timelineState, new UpdateSubBand(band.id, '1', { height: 42 }));
    expect(timelineState.bands[0].subBands[0].height).toEqual(42);
  });

  it('handle UpdateTimeline', () => {
    timelineState = reducer(timelineState, new UpdateTimeline({ labelWidth: 200 }));
    expect(timelineState).toEqual({
      ...initialState,
      labelWidth: 200,
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
