/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { initialState, reducer, SourceExplorerState } from './source-explorer';

import {
  RavenBand,
  RavenSource,
} from './../shared/models';

import {
  FetchGraphData,
  FetchGraphDataFailure,
  FetchGraphDataSuccess,
  FetchInitialSources,
  FetchInitialSourcesFailure,
  FetchInitialSourcesSuccess,
  FetchSources,
  FetchSourcesFailure,
  FetchSourcesSuccess,
  LoadContent,
  RemoveBands,
  SourceExplorerClose,
  SourceExplorerCollapse,
  SourceExplorerExpand,
  SourceExplorerOpen,
  SourceExplorerPin,
  SourceExplorerSelect,
  SourceExplorerUnpin,
} from './../actions/source-explorer';

import {
  activityBand,
  childSource,
  rootSource,
  stateBand,
} from './../shared/mocks';

describe('source-explorer reducer', () => {
  let sourceExplorerState: SourceExplorerState;

  beforeEach(() => {
    sourceExplorerState = initialState;
  });

  it('handle default', () => {
    expect(sourceExplorerState).toEqual(initialState);
  });

  it('handle FetchGraphData', () => {
    sourceExplorerState = reducer(sourceExplorerState, new FetchGraphData(childSource));
    expect(sourceExplorerState).toEqual({ ...initialState, fetchGraphDataRequestPending: true });
  });

  it('handle FetchGraphDataFailure', () => {
    sourceExplorerState = reducer(sourceExplorerState, new FetchGraphDataFailure());
    expect(sourceExplorerState).toEqual({ ...initialState, fetchGraphDataRequestPending: false });
  });

  it('handle FetchGraphDataSuccess', () => {
    const source: RavenSource = rootSource;

    const bandData = {
      newBands: [stateBand],
      updateActivityBands: {
        [activityBand.id]: {
          name: 'test-activity-band',
          points: [],
        },
      },
    };

    sourceExplorerState = reducer(sourceExplorerState, new FetchGraphDataSuccess(source, bandData));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchGraphDataRequestPending: false,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        [source.id]: {
          ...initialState.treeBySourceId[source.id],
          bandIds: {
            ...initialState.treeBySourceId[source.id].bandIds,
            '100': 'test-activity-band',
            '102': 'test-state-band',
          },
          opened: true,
        },
      },
    });
  });

  it('handle FetchInitialSources', () => {
    sourceExplorerState = reducer(sourceExplorerState, new FetchInitialSources());
    expect(sourceExplorerState).toEqual({ ...initialState, fetchInitialSourcesRequestPending: true });
  });

  it('handle FetchInitialSourcesFailure', () => {
    sourceExplorerState = reducer(sourceExplorerState, new FetchInitialSourcesFailure());
    expect(sourceExplorerState).toEqual({ ...initialState, fetchInitialSourcesRequestPending: false });
  });

  it('handle FetchInitialSourcesSuccess', () => {
    const sources = [childSource];

    sourceExplorerState = reducer(sourceExplorerState, new FetchInitialSourcesSuccess(sources));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchInitialSourcesRequestPending: false,
      initialSourcesLoaded: true,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '0': {
          ...initialState.treeBySourceId['0'],
          childIds: ['1'],
        },
        '1': {
          ...childSource,
        },
      },
    });
  });

  it('handle FetchSources', () => {
    sourceExplorerState = reducer(sourceExplorerState, new FetchSources(childSource));
    expect(sourceExplorerState).toEqual({ ...initialState, fetchSourcesRequestPending: true });
  });

  it('handle FetchSourcesFailure', () => {
    sourceExplorerState = reducer(sourceExplorerState, new FetchSourcesFailure());
    expect(sourceExplorerState).toEqual({ ...initialState, fetchSourcesRequestPending: false });
  });

  it('handle FetchSourcesSuccess', () => {
    const sources = [childSource];

    sourceExplorerState = reducer(sourceExplorerState, new FetchSourcesSuccess(rootSource, sources));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchSourcesRequestPending: false,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '0': {
          ...initialState.treeBySourceId['0'],
          childIds: ['1'],
          expanded: true,
        },
        '1': {
          ...childSource,
        },
      },
    });
  });

  it('handle LoadContent', () => {
    const sources = [childSource];

    sourceExplorerState = reducer(sourceExplorerState, new LoadContent(rootSource, sources));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchSourcesRequestPending: false,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '0': {
          ...initialState.treeBySourceId['0'],
          childIds: ['1'],
          expanded: true,
        },
        '1': {
          ...childSource,
        },
      },
    });
  });

  it('handle RemoveBands', () => {
    const source: RavenSource = rootSource;

    const bandData = {
      newBands: [stateBand],
      updateActivityBands: {
        [activityBand.id]: {
          name: 'test-activity-band',
          points: [],
        },
      },
    };

    // Add some bands first so we have something to remove.
    sourceExplorerState = reducer(sourceExplorerState, new FetchGraphDataSuccess(source, bandData));

    sourceExplorerState = reducer(sourceExplorerState, new RemoveBands(source, { bandIds: ['100', '102'], pointsBandIds: [] }));
    expect(sourceExplorerState).toEqual(initialState);
  });

  it('handle SourceExplorerCollapse', () => {
    const source: RavenSource = childSource;

    sourceExplorerState = reducer(sourceExplorerState, new SourceExplorerCollapse(source));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '1': {
          ...initialState.treeBySourceId['1'],
          expanded: false,
        },
      },
    });
  });

  it('handle SourceExplorerExpand', () => {
    const source: RavenSource = childSource;

    sourceExplorerState = reducer(sourceExplorerState, new SourceExplorerExpand(source));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '1': {
          ...initialState.treeBySourceId['1'],
          expanded: true,
        },
      },
    });
  });

  it('handle SourceExplorerClose', () => {
    const source: RavenSource = childSource;

    sourceExplorerState = reducer(sourceExplorerState, new SourceExplorerClose(source));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '1': {
          ...initialState.treeBySourceId['1'],
          opened: false,
        },
      },
    });
  });

  it('handle SourceExplorerOpen', () => {
    const source: RavenSource = childSource;

    sourceExplorerState = reducer(sourceExplorerState, new SourceExplorerOpen(source));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '1': {
          ...initialState.treeBySourceId['1'],
          opened: true,
        },
      },
    });
  });

  it('handle SourceExplorerPin', () => {
    const source: RavenSource = childSource;

    sourceExplorerState = reducer(sourceExplorerState, new SourceExplorerPin(source));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '1': {
          ...initialState.treeBySourceId['1'],
          pinned: true,
        },
      },
    });
  });

  it('handle SourceExplorerSelect', () => {
    const source: RavenSource = rootSource;

    // Make the rootSource node selectable in the initial state.
    const initState = {
      ...sourceExplorerState,
      treeBySourceId: {
        ...sourceExplorerState.treeBySourceId,
        [source.id]: {
          ...sourceExplorerState.treeBySourceId[source.id],
          selectable: true,
        },
      },
    };

    // Select node.
    sourceExplorerState = reducer(initState, new SourceExplorerSelect(source));

    expect(sourceExplorerState).toEqual({
      ...initState,
      selectedSourceId: source.id,
      treeBySourceId: {
        ...initState.treeBySourceId,
        [source.id]: {
          ...initState.treeBySourceId[source.id],
          selected: true,
        },
      },
    });

    // Deselect node.
    sourceExplorerState = reducer(sourceExplorerState, new SourceExplorerSelect(source));

    expect(sourceExplorerState).toEqual({
      ...initState,
      selectedSourceId: '',
      treeBySourceId: {
        ...initState.treeBySourceId,
        [source.id]: {
          ...initState.treeBySourceId[source.id],
          selected: false,
        },
      },
    });
  });

  it('handle SourceExplorerUnpin', () => {
    const source: RavenSource = childSource;

    sourceExplorerState = reducer(sourceExplorerState, new SourceExplorerUnpin(source));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '1': {
          ...initialState.treeBySourceId['1'],
          pinned: false,
        },
      },
    });
  });
});
