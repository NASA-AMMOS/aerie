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
  SourceExplorerState,
} from './source-explorer';

import {
  RavenSource,
} from './../shared/models';

import {
  CloseEvent,
  CollapseEvent,
  ExpandEvent,
  FetchInitialSources,
  NewSources,
  OpenEvent,
  SelectSource,
  SubBandIdAdd,
  UpdateSourceExplorer,
  UpdateTreeSource,
} from './../actions/source-explorer';

import {
  childSource,
  rootSource,
} from './../shared/mocks';

describe('source-explorer reducer', () => {
  let sourceExplorerState: SourceExplorerState;

  beforeEach(() => {
    sourceExplorerState = initialState;
  });

  it('handle default', () => {
    expect(sourceExplorerState).toEqual(initialState);
  });

  it('handle CloseEvent', () => {
    sourceExplorerState = reducer(sourceExplorerState, new CloseEvent(rootSource.id));
    expect(sourceExplorerState).toEqual({
      ...initialState,
    });
  });

  it('handle CollapseEvent', () => {
    sourceExplorerState = reducer(sourceExplorerState, new CollapseEvent(rootSource.id));
    expect(sourceExplorerState).toEqual({
      ...initialState,
    });
  });

  it('handle ExpandEvent', () => {
    sourceExplorerState = reducer(sourceExplorerState, new ExpandEvent(rootSource.id));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle FetchInitialSources', () => {
    sourceExplorerState = reducer(sourceExplorerState, new FetchInitialSources());
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle NewSources', () => {
    const sources = [childSource];

    sourceExplorerState = reducer(sourceExplorerState, new NewSources(rootSource.id, sources));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '/': {
          ...initialState.treeBySourceId['/'],
          childIds: ['/child'],
        },
        '/child': {
          ...childSource,
        },
      },
    });
  });

  it('handle OpenEvent', () => {
    sourceExplorerState = reducer(sourceExplorerState, new OpenEvent(rootSource.id));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle SelectSource', () => {
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
    sourceExplorerState = reducer(initState, new SelectSource(source));

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
    sourceExplorerState = reducer(sourceExplorerState, new SelectSource(source));

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

  it('handle SubBandIdAdd', () => {
    sourceExplorerState = reducer(sourceExplorerState, new SubBandIdAdd(rootSource.id, '100'));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '/': {
          ...initialState.treeBySourceId['/'],
          subBandIds: {
            '100': '100',
          },
        },
      },
    });
  });

  it('handle UpdateSourceExplorer', () => {
    sourceExplorerState = reducer(sourceExplorerState, new UpdateSourceExplorer({ selectedSourceId: '42' }));

    expect(sourceExplorerState).toEqual({
      ...initialState,
      selectedSourceId: '42',
    });
  });

  it('handle UpdateTreeSource', () => {
    sourceExplorerState = reducer(sourceExplorerState, new UpdateTreeSource(rootSource.id, {
      label: 'hi',
      opened: true,
    }));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        [rootSource.id]: {
          ...initialState.treeBySourceId[rootSource.id],
          label: 'hi',
          opened: true,
        },
      },
    });
  });
});
