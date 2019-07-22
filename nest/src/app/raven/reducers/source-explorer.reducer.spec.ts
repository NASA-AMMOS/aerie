/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { SourceExplorerActions } from '../actions';
import {
  categorySource,
  childSource,
  customFilterSource,
  filterSourceLocation,
  rootSource,
} from '../mocks';
import { RavenPin, RavenSource } from '../models';
import {
  initialState,
  reducer,
  SourceExplorerState,
} from './source-explorer.reducer';

describe('source-explorer reducer', () => {
  let sourceExplorerState: SourceExplorerState;

  beforeEach(() => {
    sourceExplorerState = initialState;
  });

  it('handle default', () => {
    expect(sourceExplorerState).toEqual(initialState);
  });

  it('handle AddCustomFilter', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.addCustomFilter({
        customFilter: '.*IPS.*',
        label: 'ips',
        sourceId: '/child',
      }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      customFiltersBySourceId: {
        '/child': [{ filter: '.*IPS.*', label: 'ips', subBandId: '' }],
      },
    });
  });

  it('handle AddFilter', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.newSources({
        sourceId: rootSource.id,
        sources: [categorySource],
      }),
    );
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.newSources({
        sourceId: categorySource.id,
        sources: [filterSourceLocation],
      }),
    );
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.addFilter({ source: filterSourceLocation }),
    );
    expect(sourceExplorerState.filtersByTarget).toEqual({
      '/SequenceTracker': { meeting: [filterSourceLocation.id] },
    });
  });

  it('handle AddGraphableFilter', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.addGraphableFilter({ source: customFilterSource }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle ApplyLayout', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.applyLayout({
        update: { pins: {}, targetSourceIds: ['/source1'] },
      }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle ApplyState', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.applyState({
        sourceId: rootSource.id,
        sourceUrl: rootSource.url,
      }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle CloseEvent', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.closeEvent({ sourceId: rootSource.id }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
    });
  });

  it('handle CollapseEvent', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.collapseEvent({ sourceId: rootSource.id }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
    });
  });

  it('handle ExpandEvent', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.expandEvent({ sourceId: rootSource.id }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        [rootSource.id]: {
          ...initialState.treeBySourceId[rootSource.id],
          expanded: true,
        },
      },
    });
  });

  it('handle FetchInitialSources', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.fetchInitialSources(),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle LoadErrorsAdd', () => {
    const loadErrors = ['/id1', '/id2'];
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.loadErrorsAdd({ sourceIds: loadErrors }),
    );
    expect(sourceExplorerState.loadErrors).toEqual(loadErrors);
  });

  it('handle NewSources', () => {
    const sources = [childSource];

    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.newSources({ sourceId: rootSource.id, sources }),
    );
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
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.openEvent({ sourceId: rootSource.id }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        [rootSource.id]: {
          ...initialState.treeBySourceId[rootSource.id],
          opened: true,
        },
      },
    });
  });

  it('handle PinAdd', () => {
    // Seed the root source with an `Add Pin` action.
    sourceExplorerState.treeBySourceId['/'] = {
      ...sourceExplorerState.treeBySourceId['/'],
      actions: [{ event: 'pin-add', name: 'Add Pin' }],
    };

    const pin: RavenPin = { name: 'somePin', sourceId: '/' };
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.pinAdd({ pin }),
    );

    expect(sourceExplorerState).toEqual({
      ...initialState,
      pins: [pin],
      treeBySourceId: {
        ...sourceExplorerState.treeBySourceId,
        '/': {
          ...sourceExplorerState.treeBySourceId['/'],
          actions: [
            { event: 'pin-remove', name: 'Remove Pin' },
            { event: 'pin-rename', name: 'Rename Pin' },
          ],
        },
      },
    });
  });

  it('handle PinRemove', () => {
    // Seed the root source with `Remove Pin` and `Rename Pin` actions.
    sourceExplorerState.treeBySourceId['/'] = {
      ...sourceExplorerState.treeBySourceId['/'],
      actions: [
        { event: 'pin-remove', name: 'Remove Pin' },
        { event: 'pin-rename', name: 'Rename Pin' },
      ],
    };

    // Add a pin that we can remove.
    const pin: RavenPin = { name: 'somePin', sourceId: '/' };
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.pinAdd({ pin }),
    );

    // Finally remove the pin.
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.pinRemove({ sourceId: '/' }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      pins: [],
      treeBySourceId: {
        ...sourceExplorerState.treeBySourceId,
        '/': {
          ...sourceExplorerState.treeBySourceId['/'],
          actions: [{ event: 'pin-add', name: 'Add Pin' }],
        },
      },
    });
  });

  it('handle PinRename', () => {
    // Seed the root source with `Remove Pin` and `Rename Pin` actions.
    sourceExplorerState.treeBySourceId['/'] = {
      ...sourceExplorerState.treeBySourceId['/'],
      actions: [
        { event: 'pin-remove', name: 'Remove Pin' },
        { event: 'pin-rename', name: 'Rename Pin' },
      ],
    };

    // Add a pin that we can rename.
    const pin: RavenPin = { name: 'somePin', sourceId: '/' };
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.pinAdd({ pin }),
    );

    // Finally rename the pin.
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.pinRename({
        newName: 'someNewNamePin',
        sourceId: '/',
      }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      pins: [
        {
          ...pin,
          name: 'someNewNamePin',
        },
      ],
      treeBySourceId: {
        ...sourceExplorerState.treeBySourceId,
        '/': {
          ...sourceExplorerState.treeBySourceId['/'],
          actions: [
            { event: 'pin-remove', name: 'Remove Pin' },
            { event: 'pin-rename', name: 'Rename Pin' },
          ],
        },
      },
    });
  });

  it('handle RemoveFilter', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.newSources({
        sourceId: rootSource.id,
        sources: [categorySource],
      }),
    );
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.newSources({
        sourceId: categorySource.id,
        sources: [filterSourceLocation],
      }),
    );
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.addFilter({ source: filterSourceLocation }),
    );
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.removeFilter({ source: filterSourceLocation }),
    );
    expect(sourceExplorerState.filtersByTarget).toEqual({
      '/SequenceTracker': { meeting: [] },
    });
  });

  it('handle RemoveSource', () => {
    // First add a source we can remove.
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.newSources({
        sourceId: rootSource.id,
        sources: [childSource],
      }),
    );
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.removeSource({ sourceId: childSource.id }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
    });
  });

  it('handle RemoveSourceEvent', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.removeSourceEvent({ source: rootSource }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle SaveState', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.saveState({ source: rootSource, name: 'hello' }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle SelectSource', () => {
    const source: RavenSource = rootSource;
    const initState: SourceExplorerState = sourceExplorerState;

    // Select node.
    sourceExplorerState = reducer(
      initState,
      SourceExplorerActions.selectSource({ sourceId: source.id }),
    );

    expect(sourceExplorerState).toEqual({
      ...initState,
      selectedSourceId: source.id,
    });

    // Deselect node.
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.selectSource({ sourceId: '' }),
    );

    expect(sourceExplorerState).toEqual({
      ...initState,
      selectedSourceId: '',
    });
  });

  it('handle SetCustomFilter', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.newSources({
        sourceId: rootSource.id,
        sources: [categorySource],
      }),
    );
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.newSources({
        sourceId: categorySource.id,
        sources: [customFilterSource],
      }),
    );
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.setCustomFilter({
        filter: 'jm0132',
        source: customFilterSource,
      }),
    );
    expect(customFilterSource).toEqual({
      ...customFilterSource,
      filter: 'jm0132',
    });
  });

  it('handle SetCustomFilterSubBandId', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.addCustomFilter({
        customFilter: '.*IPS.*',
        label: 'ips',
        sourceId: '/child',
      }),
    );
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.setCustomFilterSubBandId({
        label: 'ips',
        sourceId: '/child',
        subBandId: '45',
      }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      customFiltersBySourceId: {
        '/child': [{ filter: '.*IPS.*', label: 'ips', subBandId: '45' }],
      },
    });
  });

  it('handle SubBandIdAdd', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.subBandIdAdd({
        sourceId: rootSource.id,
        subBandId: '100',
      }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '/': {
          ...initialState.treeBySourceId['/'],
          subBandIds: ['100'],
        },
      },
    });
  });

  it('handle SubBandIdRemove', () => {
    // First add a sub-band id we can remove.
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.subBandIdAdd({
        sourceId: rootSource.id,
        subBandId: '100',
      }),
    );
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.subBandIdRemove({
        sourceIds: ['/'],
        subBandId: '100',
      }),
    );
    expect(sourceExplorerState).toEqual({
      ...initialState,
      customFiltersBySourceId: {},
      treeBySourceId: {
        ...initialState.treeBySourceId,
        '/': {
          ...initialState.treeBySourceId['/'],
          subBandIds: [],
        },
      },
    });
  });

  it('handle UpdateSourceExplorer', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.updateSourceExplorer({
        update: { selectedSourceId: '42' },
      }),
    );

    expect(sourceExplorerState).toEqual({
      ...initialState,
      selectedSourceId: '42',
    });
  });

  it('handle UpdateTreeSource', () => {
    sourceExplorerState = reducer(
      sourceExplorerState,
      SourceExplorerActions.updateTreeSource({
        sourceId: rootSource.id,
        update: { label: 'hi', opened: true },
      }),
    );
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
