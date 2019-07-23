/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { TimelineActions } from '../actions';
import {
  activityBand,
  activityPoint,
  activityPoints,
  activityPointToAdd,
  compositeBand,
  grandChildSource,
  overlayResourceBands,
  resourceBand,
  rootSource,
  stateBand,
} from '../mocks';
import { RavenActivityPoint, RavenSource } from '../models';
import { hasTwoResourceBands } from '../util';
import { initialState, reducer, TimelineState } from './timeline.reducer';

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
      subBands: [
        {
          ...stateBand,
        },
      ],
    };
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: newBand }),
    );

    expect(timelineState).toEqual({
      ...initialState,
      bands: [
        {
          ...compositeBand,
          containerId: '0',
          sortOrder: 0,
          subBands: [
            {
              ...stateBand,
              parentUniqueId: compositeBand.id,
              sourceIds: [source.id],
            },
          ],
        },
      ],
      maxTimeRange: { end: 110, start: 10 },
      selectedBandId: '200',
      selectedSubBandId: '102',
      viewTimeRange: { end: 110, start: 10 },
    });
  });

  it('handle AddBand (with filterTarget)', () => {
    const source: RavenSource = rootSource;
    const newBand = {
      ...compositeBand,
      subBands: [
        {
          ...activityBand,
        },
      ],
    };
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({
        band: newBand,
        modifiers: {
          additionalSubBandProps: { filterTarget: 'DKF' },
        },
        sourceId: source.id,
      }),
    );

    expect(timelineState).toEqual({
      ...initialState,
      bands: [
        {
          ...compositeBand,
          containerId: '0',
          sortOrder: 0,
          subBands: [
            {
              ...activityBand,
              filterTarget: 'DKF',
              parentUniqueId: compositeBand.id,
              sourceIds: [source.id],
            },
          ],
        },
      ],
      maxTimeRange: { end: 210, start: 50 },
      selectedBandId: '200',
      selectedSubBandId: '100',
      viewTimeRange: { end: 210, start: 50 },
    });
  });

  it('handle AddBand (no source)', () => {
    const newBand = {
      ...compositeBand,
      subBands: [
        {
          ...stateBand,
        },
      ],
    };
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: null, band: newBand }),
    );

    expect(timelineState).toEqual({
      ...initialState,
      bands: [
        {
          ...compositeBand,
          containerId: '0',
          sortOrder: 0,
          subBands: [
            {
              ...stateBand,
              parentUniqueId: compositeBand.id,
              sourceIds: [],
            },
          ],
        },
      ],
      maxTimeRange: { end: 110, start: 10 },
      selectedBandId: '200',
      selectedSubBandId: '102',
      viewTimeRange: { end: 110, start: 10 },
    });
  });

  it('handle AddPointAtIndex', () => {
    const source: RavenSource = rootSource;
    const newBand = {
      ...compositeBand,
      subBands: [
        {
          ...activityBand,
        },
      ],
    };

    // First add band to state so we have something to add point to.
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: newBand }),
    );

    expect(timelineState.bands[0].subBands[0].points).toEqual([]);
    timelineState = reducer(
      timelineState,
      TimelineActions.addPointsToSubBand({
        bandId: newBand.id,
        points: activityPoints,
        sourceId: source.id,
        subBandId: activityBand.id,
      }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.addPointAtIndex({
        bandId: newBand.id,
        index: 1,
        point: activityPointToAdd,
        subBandId: activityBand.id,
      }),
    );
    expect(timelineState.bands[0].subBands[0].points[1]).toEqual(
      activityPointToAdd,
    );
  });

  it('handle MarkRemovePointsInSubBand', () => {
    const source: RavenSource = rootSource;
    const band = {
      ...compositeBand,
      id: '1',
      subBands: [
        {
          ...activityBand,
          id: '2',
          parentUniqueId: '1',
          points: [
            {
              ...activityPoint,
            },
            {
              ...activityPoint,
              id: 'abc',
              uniqueId: '_abc',
            },
          ],
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.markRemovePointsInSubBand({
        bandId: band.id,
        points: [activityPoint],
        subBandId: '2',
      }),
    );
    expect(timelineState.bands[0].subBands[0].points[0].pointStatus).toEqual(
      'deleted',
    );
    expect(timelineState.bands[0].subBands[0].points[1].pointStatus).toEqual(
      'unchanged',
    );
  });

  it('handle RemovePointsInSubBand', () => {
    const source: RavenSource = rootSource;
    const band = {
      ...compositeBand,
      id: '1',
      subBands: [
        {
          ...activityBand,
          id: '2',
          parentUniqueId: '1',
          points: [
            {
              ...activityPoint,
            },
            {
              ...activityPoint,
              id: 'abc',
              uniqueId: '_abc',
            },
          ],
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.removePointsInSubBand({
        bandId: band.id,
        points: [activityPoint],
        subBandId: '2',
      }),
    );
    expect(timelineState.bands[0].subBands[0].points[0]).toEqual({
      ...activityPoint,
      id: 'abc',
      uniqueId: '_abc',
    });
  });

  it('handle ToggleGuide', () => {
    timelineState = reducer(
      timelineState,
      TimelineActions.toggleGuide({
        guide: { guideTime: 1665067939, timePerPixel: 20 },
      }),
    );
    expect(timelineState).toEqual({
      ...initialState,
      guides: [1665067939],
    });

    timelineState = reducer(
      timelineState,
      TimelineActions.toggleGuide({
        guide: { guideTime: 1665067949, timePerPixel: 20 },
      }),
    );
    expect(timelineState).toEqual({
      ...initialState,
      guides: [],
    });
  });

  it('handle UpdatePointInSubBand', () => {
    const source: RavenSource = rootSource;
    const band = {
      ...compositeBand,
      id: '1',
      subBands: [
        {
          ...activityBand,
          id: '2',
          parentUniqueId: '1',
          points: [
            {
              ...activityPoint,
              id: '123',
            },
          ],
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.updatePointInSubBand({
        bandId: band.id,
        pointId: '123',
        subBandId: '2',
        update: { start: 67890 },
      }),
    );
    expect(timelineState.bands[0].subBands[0].points[0].start).toEqual(67890);
  });

  it('handle AddBand (insert after named band)', () => {
    const actions = [
      TimelineActions.addBand({
        band: { ...compositeBand, id: '0' },
        sourceId: null,
      }),
      TimelineActions.addBand({
        band: { ...compositeBand, id: '1' },
        sourceId: null,
      }),
      TimelineActions.addBand({
        band: { ...compositeBand, id: '42' },
        modifiers: { afterBandId: '0' },
        sourceId: null,
      }),
    ];

    timelineState = actions.reduce(
      (state, action) => reducer(state, action),
      timelineState,
    );

    const sortedBands = [...timelineState.bands].sort(
      (a, b) => a.sortOrder - b.sortOrder,
    );

    expect(sortedBands.map(b => b.id)).toEqual(['0', '42', '1']);
  });

  it('handle AddPointsToSubBand', () => {
    const source: RavenSource = rootSource;
    const newBand = {
      ...compositeBand,
      subBands: [
        {
          ...activityBand,
        },
      ],
    };

    // First add band to state so we have something to add points to.
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: newBand }),
    );

    expect(timelineState.bands[0].subBands[0].points).toEqual([]);
    timelineState = reducer(
      timelineState,
      TimelineActions.addPointsToSubBand({
        bandId: newBand.id,
        points: [activityPoint],
        sourceId: source.id,
        subBandId: activityBand.id,
      }),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([activityPoint]);
  });

  it('handle AddSubBand', () => {
    const source: RavenSource = rootSource;
    const newBand = {
      ...compositeBand,
      subBands: [
        {
          ...stateBand,
        },
      ],
    };

    // First add band to state so we have something to add a sub-band to.
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: newBand }),
    );

    expect(timelineState.bands[0].subBands.length).toEqual(1);
    timelineState = reducer(
      timelineState,
      TimelineActions.addSubBand({
        bandId: newBand.id,
        sourceId: source.id,
        subBand: activityBand,
      }),
    );
    expect(timelineState.bands[0].subBands.length).toEqual(2);
    expect(timelineState).toEqual({
      ...initialState,
      bands: [
        {
          ...compositeBand,
          containerId: '0',
          sortOrder: 0,
          subBands: [
            {
              ...stateBand,
              parentUniqueId: compositeBand.id,
              sourceIds: [source.id],
            },
            {
              ...activityBand,
              parentUniqueId: compositeBand.id,
              sourceIds: [source.id],
            },
          ],
        },
      ],
      maxTimeRange: { end: 210, start: 10 },
      selectedBandId: '200',
      selectedSubBandId: '102',
      viewTimeRange: { end: 110, start: 10 },
    });
  });

  it('handle expandPoint ExpandChildrenOrDescendants children', () => {
    const source: RavenSource = rootSource;
    const newBand = {
      ...compositeBand,
      subBands: [
        {
          ...activityBand,
        },
      ],
    };

    // First add band to state so we have something to add points to.
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: newBand }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.addPointsToSubBand({
        bandId: newBand.id,
        points: [activityPoint],
        sourceId: source.id,
        subBandId: activityBand.id,
      }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.expandChildrenOrDescendants({
        activityPoint,
        bandId: newBand.id,
        expandType: 'expandChildren',
        subBandId: activityBand.id,
      }),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([
      { ...activityPoint, expansion: 'expandChildren' },
    ]);
  });

  it('handle expandPoint ExpandChildrenOrDescendants descendants', () => {
    const source: RavenSource = rootSource;
    const newBand = {
      ...compositeBand,
      subBands: [
        {
          ...activityBand,
        },
      ],
    };

    // First add band to state so we have something to add points to.
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: newBand }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.addPointsToSubBand({
        bandId: newBand.id,
        points: [activityPoint],
        sourceId: source.id,
        subBandId: activityBand.id,
      }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.expandChildrenOrDescendants({
        activityPoint,
        bandId: newBand.id,
        expandType: 'expandDescendants',
        subBandId: activityBand.id,
      }),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([
      { ...activityPoint, expansion: 'expandDescendants' },
    ]);
  });

  it('handle hasTwoResourceBands', () => {
    const source: RavenSource = rootSource;
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({
        band: overlayResourceBands,
        sourceId: source.id,
      }),
    );
    expect(hasTwoResourceBands(timelineState.bands[0])).toBe(true);
  });

  it('handle hasTwoResourceBands', () => {
    const newBand = {
      ...compositeBand,
      subBands: [
        {
          ...resourceBand,
        },
      ],
    };
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: newBand.id, band: newBand }),
    );
    expect(hasTwoResourceBands(timelineState.bands[0])).toBe(false);
  });

  it('handle PanLeftViewTimeRange', () => {
    // First set the timeline state to have realistic time ranges we can test.
    timelineState = {
      ...timelineState,
      viewTimeRange: { end: 1741564830, start: 1655143200 },
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.panLeftViewTimeRange(),
    );
    expect(timelineState).toEqual({
      ...initialState,
      viewTimeRange: { end: 1663785363, start: 1577363733 },
    });
  });

  it('handle PanRightViewTimeRange', () => {
    // First set the timeline state to have realistic time ranges we can test.
    timelineState = {
      ...timelineState,
      viewTimeRange: { end: 1741564830, start: 1655143200 },
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.panRightViewTimeRange(),
    );
    expect(timelineState).toEqual({
      ...initialState,
      viewTimeRange: { end: 1819344297, start: 1732922667 },
    });
  });

  it('handle PinRemove', () => {
    timelineState = reducer(
      timelineState,
      TimelineActions.pinRemove({ sourceId: '/' }),
    );
    expect(timelineState).toEqual(initialState);
  });

  it('handle PinRename', () => {
    timelineState = reducer(
      timelineState,
      TimelineActions.pinRename({ sourceId: '/', newName: 'hello' }),
    );
    expect(timelineState).toEqual(initialState);
  });

  it('handle RemoveAllPointsInSubBandWithParentSource', () => {
    const source: RavenSource = grandChildSource;

    const band = {
      ...compositeBand,
      id: '0',
      subBands: [
        {
          ...activityBand,
          id: '1',
          parentUniqueId: '0',
          points: [
            {
              ...activityPoint,
              id: '100',
              sourceId: '/child/grandChild',
            },
            {
              ...activityPoint,
              id: '101',
              sourceId: '/child/gandChild',
            },
          ],
          sourceIds: [],
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.removeAllPointsInSubBandWithParentSource({
        parentSourceId: '/child',
      }),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([]);
  });

  it('handle RemoveBandsOrPointsForSource', () => {
    const source: RavenSource = rootSource;

    const band = {
      ...compositeBand,
      id: '0',
      subBands: [
        {
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
          sourceIds: ['/', '/child'],
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    expect(timelineState.bands[0].subBands[0].points.length).toEqual(2);

    timelineState = reducer(
      timelineState,
      TimelineActions.removeBandsOrPointsForSource({ sourceId: '/' }),
    );
    expect(timelineState.bands[0].subBands[0].points[0].id).toEqual('101');
    expect(timelineState.bands[0].subBands[0].points.length).toEqual(1);

    timelineState = reducer(
      timelineState,
      TimelineActions.removeBandsOrPointsForSource({ sourceId: '/child' }),
    );
    expect(timelineState.bands.length).toEqual(0);
  });

  it('handle RemoveBandsWithNoPoints', () => {
    const source: RavenSource = rootSource;

    const band0 = {
      ...compositeBand,
      id: '0',
      subBands: [
        {
          ...activityBand,
          id: '1',
          parentUniqueId: '0',
          points: [],
          sourceIds: ['/', '/child'],
        },
      ],
    };

    const band1 = {
      ...compositeBand,
      id: '1',
      subBands: [
        {
          ...activityBand,
          id: '2',
          parentUniqueId: '1',
          points: [
            {
              ...activityPoint,
              id: '100',
              sourceId: '/',
            },
          ],
          sourceIds: ['/', '/child'],
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: band0 }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: band1 }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.removeBandsWithNoPoints(),
    );
    expect(timelineState.bands.length).toEqual(1);
  });

  it('handle removeDescendants', () => {
    const source: RavenSource = rootSource;
    const newBand = {
      ...compositeBand,
      subBands: [
        {
          ...activityBand,
        },
      ],
    };

    // First add band to state so we have something to add points to.
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: newBand }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.addPointsToSubBand({
        bandId: newBand.id,
        points: [activityPoint],
        sourceId: source.id,
        subBandId: activityBand.id,
      }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.expandChildrenOrDescendants({
        activityPoint,
        bandId: newBand.id,
        expandType: 'expandChildren',
        subBandId: activityBand.id,
      }),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([
      { ...activityPoint, expansion: 'expandChildren' },
    ]);
    timelineState = reducer(
      timelineState,
      TimelineActions.removeChildrenOrDescendants({
        activityPoint,
        bandId: newBand.id,
        subBandId: activityBand.id,
      }),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([activityPoint]);
  });

  it('handle RemoveSourceIdFromSubBands', () => {
    const source: RavenSource = grandChildSource;
    const band = {
      ...compositeBand,
      id: '0',
      subBands: [
        {
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
          sourceIds: ['/', '/child'],
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    expect(timelineState.bands[0].subBands[0].sourceIds).toEqual([
      '/',
      '/child',
      '/child/grandChild',
    ]);
    timelineState = reducer(
      timelineState,
      TimelineActions.removeSourceIdFromSubBands({
        sourceId: '/child/grandChild',
      }),
    );
    expect(timelineState.bands[0].subBands[0].sourceIds).toEqual([
      '/',
      '/child',
    ]);
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
          sourceIds: ['/'],
        },
        {
          ...activityBand,
          id: '2',
          parentUniqueId: '0',
          sourceIds: ['/'],
        },
      ],
    };

    // First add a band with some sub-bands so we can remove them.
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    expect(timelineState.bands[0].subBands.length).toEqual(2);
    expect(timelineState.bands[0].subBands[0].id).toEqual('1');
    expect(timelineState.bands[0].subBands[1].id).toEqual('2');

    timelineState = reducer(
      timelineState,
      TimelineActions.removeSubBand({ subBandId: '1' }),
    );
    expect(timelineState.bands[0].subBands.length).toEqual(1);
    expect(timelineState.bands[0].subBands[0].id).toEqual('2');

    timelineState = reducer(
      timelineState,
      TimelineActions.removeSubBand({ subBandId: '2' }),
    );
    expect(timelineState.bands.length).toEqual(0);
  });

  it('handle ResetViewTimeRange', () => {
    // First set the timeline state to have realistic time ranges we can test.
    timelineState = {
      ...timelineState,
      maxTimeRange: { end: 1741564830, start: 1655143200 },
      viewTimeRange: { end: 1000, start: 0 },
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.resetViewTimeRange(),
    );
    expect(timelineState).toEqual({
      ...initialState,
      maxTimeRange: { end: 1741564830, start: 1655143200 },
      viewTimeRange: { end: 1741564830, start: 1655143200 },
    });
  });

  it('handle SelectBand', () => {
    const source: RavenSource = rootSource;

    const newBand = {
      ...compositeBand,
      id: '0',
      subBands: [
        {
          ...stateBand,
          id: '1',
          parentUniqueId: '0',
          sourceIds: ['/'],
        },
      ],
    };

    expect(timelineState.selectedBandId).toEqual('');
    expect(timelineState.selectedSubBandId).toEqual('');

    // Add a band and select it.
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: newBand }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.selectBand({ bandId: '0' }),
    );

    expect(timelineState.selectedBandId).toEqual('0');
    expect(timelineState.selectedSubBandId).toEqual('1');
  });

  it('handle SelectPoint', () => {
    const source: RavenSource = rootSource;

    const point: RavenActivityPoint = {
      ...activityPoint,
      subBandId: '1',
      uniqueId: '400',
    };

    const newBand = {
      ...compositeBand,
      id: '0',
      subBands: [
        {
          ...activityBand,
          id: '1',
          parentUniqueId: '0',
          points: [
            {
              ...point,
            },
          ],
          sourceIds: ['/'],
        },
      ],
    };

    expect(timelineState.selectedBandId).toEqual('');
    expect(timelineState.selectedSubBandId).toEqual('');

    // Add a band and and select a point from it.
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: newBand }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.selectPoint({
        bandId: '0',
        pointId: '400',
        subBandId: '1',
      }),
    );

    expect(timelineState.selectedPoint).toEqual(point);
  });

  it('handle SetCompositeYLabelDefault', () => {
    const source: RavenSource = rootSource;
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({
        band: overlayResourceBands,
        sourceId: source.id,
      }),
    );
    expect(timelineState.bands[0].compositeYAxisLabel).toBe(false);
    timelineState = reducer(
      timelineState,
      TimelineActions.setCompositeYLabelDefault({
        bandId: overlayResourceBands.id,
      }),
    );
    expect(timelineState.bands[0].compositeYAxisLabel).toBe(true);
  });

  it('handle SetCompositeYLabelDefault', () => {
    const newBand = {
      ...compositeBand,
      subBands: [
        {
          ...resourceBand,
        },
      ],
    };
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: newBand.id, band: newBand }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.setCompositeYLabelDefault({ bandId: newBand.id }),
    );
    expect(timelineState.bands[0].compositeYAxisLabel).toBe(false);
  });

  it('handle SetPointsForSubBand', () => {
    const source = grandChildSource;
    const points = [activityPoint];
    const band = {
      ...compositeBand,
      id: '0',
      subBands: [
        {
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
          sourceIds: ['/', '/child'],
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.setPointsForSubBand({
        bandId: '0',
        points,
        subBandId: '1',
      }),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual(points);
  });

  it('handle SortBands', () => {
    const source: RavenSource = rootSource;

    const stateBand1 = {
      ...compositeBand,
      id: '0',
      subBands: [
        {
          ...stateBand,
          id: '400',
          parentUniqueId: '0',
          sourceIds: ['/'],
        },
      ],
    };

    const stateBand2 = {
      ...compositeBand,
      id: '1',
      subBands: [
        {
          ...stateBand,
          id: '500',
          parentUniqueId: '1',
          sourceIds: ['/'],
        },
      ],
    };

    // First add some bands we can sort.
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: stateBand1 }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band: stateBand2 }),
    );

    const sort = {
      [stateBand1.id]: { containerId: '0', sortOrder: 1 },
      [stateBand2.id]: { containerId: '1', sortOrder: 0 },
    };

    timelineState = reducer(timelineState, TimelineActions.sortBands({ sort }));

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
      subBands: [
        {
          ...stateBand,
          id: '1',
          parentUniqueId: '0',
          sourceIds: ['/'],
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    expect(timelineState.bands[0].height).toEqual(50);

    timelineState = reducer(
      timelineState,
      TimelineActions.updateBand({ bandId: band.id, update: { height: 42 } }),
    );
    expect(timelineState.bands[0].height).toEqual(42);
  });

  it('handle UpdateLastClickTime', () => {
    timelineState = reducer(
      timelineState,
      TimelineActions.updateLastClickTime({ time: 1665067123 }),
    );

    expect(timelineState).toEqual({
      ...initialState,
      lastClickTime: 1665067123,
    });
  });

  it('handle UpdateSubBand', () => {
    const source: RavenSource = rootSource;

    const band = {
      ...compositeBand,
      height: 50,
      id: '0',
      subBands: [
        {
          ...stateBand,
          height: 50,
          id: '1',
          parentUniqueId: '0',
          sourceIds: ['/'],
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    expect(timelineState.bands[0].subBands[0].height).toEqual(50);

    timelineState = reducer(
      timelineState,
      TimelineActions.updateSubBand({
        bandId: band.id,
        subBandId: '1',
        update: { height: 42 },
      }),
    );
    expect(timelineState.bands[0].subBands[0].height).toEqual(42);
  });

  it('handle UpdateSubBandTimeDelta', () => {
    const source: RavenSource = rootSource;
    const points = [
      {
        duration: null,
        end: 12000,
        id: '1',
        interpolateEnding: true,
        selected: false,
        sourceId: '',
        start: 11000,
        subBandId: '',
        type: 'state',
        uniqueId: '123',
        value: 'on',
      },
      {
        duration: null,
        end: 22000,
        id: '2',
        interpolateEnding: true,
        selected: false,
        sourceId: '',
        start: 21000,
        subBandId: '',
        type: 'state',
        uniqueId: '1234',
        value: 'off',
      },
    ];

    const band = {
      ...compositeBand,
      height: 50,
      id: '0',
      subBands: [
        {
          ...stateBand,
          points,
        },
      ],
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.updateSubBandTimeDelta({
        bandId: band.id,
        subBandId: stateBand.id,
        timeDelta: 200,
      }),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([
      { ...points[0], end: 12200, start: 11200 },
      { ...points[1], end: 22200, start: 21200 },
    ]);

    timelineState = reducer(
      timelineState,
      TimelineActions.addBand({ sourceId: source.id, band }),
    );
    timelineState = reducer(
      timelineState,
      TimelineActions.updateSubBandTimeDelta({
        bandId: band.id,
        subBandId: stateBand.id,
        timeDelta: 0,
      }),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([
      { ...points[0], end: 12000, start: 11000 },
      { ...points[1], end: 22000, start: 21000 },
    ]);
  });

  it('handle UpdateTimeline', () => {
    timelineState = reducer(
      timelineState,
      TimelineActions.updateTimeline({
        update: {
          viewTimeRange: { end: 314, start: 272 },
        },
      }),
    );
    expect(timelineState).toEqual({
      ...initialState,
      viewTimeRange: { end: 314, start: 272 },
    });
  });

  it('handle UpdateViewTimeRange', () => {
    timelineState = reducer(
      timelineState,
      TimelineActions.updateViewTimeRange({
        viewTimeRange: { end: 314, start: 272 },
      }),
    );
    expect(timelineState).toEqual({
      ...initialState,
      viewTimeRange: { end: 314, start: 272 },
    });
  });

  it('handle ZoomInViewTimeRange', () => {
    // First set the timeline state to have realistic time ranges we can test.
    timelineState = {
      ...timelineState,
      maxTimeRange: { end: 1741564830, start: 1655143200 },
      viewTimeRange: { end: 1741564830, start: 1655143200 },
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.zoomInViewTimeRange(),
    );
    expect(timelineState).toEqual({
      ...initialState,
      maxTimeRange: { end: 1741564830, start: 1655143200 },
      viewTimeRange: { end: 1732922667, start: 1663785363 },
    });
  });

  it('handle ZoomOutViewTimeRange', () => {
    // First set the timeline state to have realistic time ranges we can test.
    timelineState = {
      ...timelineState,
      maxTimeRange: { end: 1741564830, start: 1655143200 },
      viewTimeRange: { end: 1741564830, start: 1655143200 },
    };

    timelineState = reducer(
      timelineState,
      TimelineActions.zoomOutViewTimeRange(),
    );
    expect(timelineState).toEqual({
      ...initialState,
      maxTimeRange: { end: 1741564830, start: 1655143200 },
      viewTimeRange: { end: 1750206993, start: 1646501037 },
    });
  });
});
