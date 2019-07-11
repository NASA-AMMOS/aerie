/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  AddBand,
  AddPointsToSubBand,
  AddSubBand,
  ExpandChildrenOrDescendants,
  MarkRemovePointsInSubBand,
  PanLeftViewTimeRange,
  PanRightViewTimeRange,
  PinRemove,
  PinRename,
  RemoveAllPointsInSubBandWithParentSource,
  RemoveBandsOrPointsForSource,
  RemoveBandsWithNoPoints,
  RemoveChildrenOrDescendants,
  RemoveSourceIdFromSubBands,
  RemoveSubBand,
  ResetViewTimeRange,
  SelectBand,
  SelectPoint,
  SetCompositeYLabelDefault,
  SetPointsForSubBand,
  SortBands,
  ToggleGuide,
  UpdateBand,
  UpdateLastClickTime,
  UpdatePointInSubBand,
  UpdateSubBand,
  UpdateSubBandTimeDelta,
  UpdateTimeline,
  UpdateViewTimeRange,
  ZoomInViewTimeRange,
  ZoomOutViewTimeRange,
} from '../actions/timeline.actions';
import {
  activityBand,
  activityPoint,
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
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));

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
      new AddBand(source.id, newBand, {
        additionalSubBandProps: { filterTarget: 'DKF' },
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
    timelineState = reducer(timelineState, new AddBand(null, newBand));

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

  it('handle ToggleGuide', () => {
    timelineState = reducer(
      timelineState,
      new ToggleGuide({ guideTime: 1665067939, timePerPixel: 20 }),
    );
    expect(timelineState).toEqual({
      ...initialState,
      guides: [1665067939],
    });

    timelineState = reducer(
      timelineState,
      new ToggleGuide({ guideTime: 1665067949, timePerPixel: 20 }),
    );
    expect(timelineState).toEqual({
      ...initialState,
      guides: [],
    });
  });

  it('handle AddBand (insert after named band)', () => {
    const actions = [
      new AddBand(null, { ...compositeBand, id: '0' }),
      new AddBand(null, { ...compositeBand, id: '1' }),
      new AddBand(null, { ...compositeBand, id: '42' }, { afterBandId: '0' }),
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
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));

    expect(timelineState.bands[0].subBands[0].points).toEqual([]);
    timelineState = reducer(
      timelineState,
      new AddPointsToSubBand(source.id, newBand.id, activityBand.id, [
        activityPoint,
      ]),
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
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));

    expect(timelineState.bands[0].subBands.length).toEqual(1);
    timelineState = reducer(
      timelineState,
      new AddSubBand(source.id, newBand.id, activityBand),
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
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));
    timelineState = reducer(
      timelineState,
      new AddPointsToSubBand(source.id, newBand.id, activityBand.id, [
        activityPoint,
      ]),
    );
    timelineState = reducer(
      timelineState,
      new ExpandChildrenOrDescendants(
        newBand.id,
        activityBand.id,
        activityPoint,
        'expandChildren',
      ),
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
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));
    timelineState = reducer(
      timelineState,
      new AddPointsToSubBand(source.id, newBand.id, activityBand.id, [
        activityPoint,
      ]),
    );
    timelineState = reducer(
      timelineState,
      new ExpandChildrenOrDescendants(
        newBand.id,
        activityBand.id,
        activityPoint,
        'expandDescendants',
      ),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([
      { ...activityPoint, expansion: 'expandDescendants' },
    ]);
  });

  it('handle hasTwoResourceBands', () => {
    const source: RavenSource = rootSource;
    timelineState = reducer(
      timelineState,
      new AddBand(source.id, overlayResourceBands),
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
    timelineState = reducer(timelineState, new AddBand(newBand.id, newBand));
    expect(hasTwoResourceBands(timelineState.bands[0])).toBe(false);
  });

  it('handle PanLeftViewTimeRange', () => {
    // First set the timeline state to have realistic time ranges we can test.
    timelineState = {
      ...timelineState,
      viewTimeRange: { end: 1741564830, start: 1655143200 },
    };

    timelineState = reducer(timelineState, new PanLeftViewTimeRange());
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

    timelineState = reducer(timelineState, new PanRightViewTimeRange());
    expect(timelineState).toEqual({
      ...initialState,
      viewTimeRange: { end: 1819344297, start: 1732922667 },
    });
  });

  it('handle PinRemove', () => {
    timelineState = reducer(timelineState, new PinRemove('/'));
    expect(timelineState).toEqual(initialState);
  });

  it('handle PinRename', () => {
    timelineState = reducer(timelineState, new PinRename('/', 'hello'));
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

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    timelineState = reducer(
      timelineState,
      new RemoveAllPointsInSubBandWithParentSource('/child'),
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

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    expect(timelineState.bands[0].subBands[0].points.length).toEqual(2);

    timelineState = reducer(
      timelineState,
      new RemoveBandsOrPointsForSource('/'),
    );
    expect(timelineState.bands[0].subBands[0].points[0].id).toEqual('101');
    expect(timelineState.bands[0].subBands[0].points.length).toEqual(1);

    timelineState = reducer(
      timelineState,
      new RemoveBandsOrPointsForSource('/child'),
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

    timelineState = reducer(timelineState, new AddBand(source.id, band0));
    timelineState = reducer(timelineState, new AddBand(source.id, band1));
    timelineState = reducer(timelineState, new RemoveBandsWithNoPoints());
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
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));
    timelineState = reducer(
      timelineState,
      new AddPointsToSubBand(source.id, newBand.id, activityBand.id, [
        activityPoint,
      ]),
    );
    timelineState = reducer(
      timelineState,
      new ExpandChildrenOrDescendants(
        newBand.id,
        activityBand.id,
        activityPoint,
        'expandChildren',
      ),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([
      { ...activityPoint, expansion: 'expandChildren' },
    ]);
    timelineState = reducer(
      timelineState,
      new RemoveChildrenOrDescendants(
        newBand.id,
        activityBand.id,
        activityPoint,
      ),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([activityPoint]);
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

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    timelineState = reducer(
      timelineState,
      new MarkRemovePointsInSubBand(band.id, '2', [activityPoint]),
    );
    expect(timelineState.bands[0].subBands[0].points[0].pointStatus).toEqual(
      'deleted',
    );
    expect(timelineState.bands[0].subBands[0].points[1].pointStatus).toEqual(
      'unchanged',
    );
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

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    expect(timelineState.bands[0].subBands[0].sourceIds).toEqual([
      '/',
      '/child',
      '/child/grandChild',
    ]);
    timelineState = reducer(
      timelineState,
      new RemoveSourceIdFromSubBands('/child/grandChild'),
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

  it('handle ResetViewTimeRange', () => {
    // First set the timeline state to have realistic time ranges we can test.
    timelineState = {
      ...timelineState,
      maxTimeRange: { end: 1741564830, start: 1655143200 },
      viewTimeRange: { end: 1000, start: 0 },
    };

    timelineState = reducer(timelineState, new ResetViewTimeRange());
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
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));
    timelineState = reducer(timelineState, new SelectBand('0'));

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
    timelineState = reducer(timelineState, new AddBand(source.id, newBand));
    timelineState = reducer(timelineState, new SelectPoint('0', '1', '400'));

    expect(timelineState.selectedPoint).toEqual(point);
  });

  it('handle SetCompositeYLabelDefault', () => {
    const source: RavenSource = rootSource;
    timelineState = reducer(
      timelineState,
      new AddBand(source.id, overlayResourceBands),
    );
    expect(timelineState.bands[0].compositeYAxisLabel).toBe(false);
    timelineState = reducer(
      timelineState,
      new SetCompositeYLabelDefault(overlayResourceBands.id),
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
    timelineState = reducer(timelineState, new AddBand(newBand.id, newBand));
    timelineState = reducer(
      timelineState,
      new SetCompositeYLabelDefault(newBand.id),
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

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    timelineState = reducer(
      timelineState,
      new SetPointsForSubBand('0', '1', points),
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

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    timelineState = reducer(
      timelineState,
      new UpdatePointInSubBand(band.id, '2', '123', { start: 67890 }),
    );
    expect(timelineState.bands[0].subBands[0].points[0].start).toEqual(67890);
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

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    timelineState = reducer(
      timelineState,
      new UpdatePointInSubBand(band.id, '2', '123', { end: 67890 }),
    );
    expect(timelineState.bands[0].subBands[0].points[0].end).toEqual(67890);
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

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    expect(timelineState.bands[0].height).toEqual(50);

    timelineState = reducer(
      timelineState,
      new UpdateBand(band.id, { height: 42 }),
    );
    expect(timelineState.bands[0].height).toEqual(42);
  });

  it('handle UpdateLastClickTime', () => {
    timelineState = reducer(timelineState, new UpdateLastClickTime(1665067123));

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

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    expect(timelineState.bands[0].subBands[0].height).toEqual(50);

    timelineState = reducer(
      timelineState,
      new UpdateSubBand(band.id, '1', { height: 42 }),
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

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    timelineState = reducer(
      timelineState,
      new UpdateSubBandTimeDelta(band.id, stateBand.id, 200),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([
      { ...points[0], end: 12200, start: 11200 },
      { ...points[1], end: 22200, start: 21200 },
    ]);

    timelineState = reducer(timelineState, new AddBand(source.id, band));
    timelineState = reducer(
      timelineState,
      new UpdateSubBandTimeDelta(band.id, stateBand.id, 0),
    );
    expect(timelineState.bands[0].subBands[0].points).toEqual([
      { ...points[0], end: 12000, start: 11000 },
      { ...points[1], end: 22000, start: 21000 },
    ]);
  });

  it('handle UpdateTimeline', () => {
    timelineState = reducer(
      timelineState,
      new UpdateTimeline({
        viewTimeRange: { end: 314, start: 272 },
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
      new UpdateViewTimeRange({ end: 314, start: 272 }),
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

    timelineState = reducer(timelineState, new ZoomInViewTimeRange());
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

    timelineState = reducer(timelineState, new ZoomOutViewTimeRange());
    expect(timelineState).toEqual({
      ...initialState,
      maxTimeRange: { end: 1741564830, start: 1655143200 },
      viewTimeRange: { end: 1750206993, start: 1646501037 },
    });
  });
});
