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
  RavenPin,
  RavenSource,
} from './../shared/models';

import {
  AddCustomFilter,
  AddFilter,
  ApplyState,
  CloseEvent,
  CollapseEvent,
  ExpandEvent,
  FetchInitialSources,
  NewSources,
  OpenEvent,
  PinAdd,
  PinRemove,
  PinRename,
  RemoveFilter,
  RemoveSource,
  RemoveSourceEvent,
  SaveState,
  SelectSource,
  SetCustomFilter,
  SetCustomFilterSubBandId,
  SubBandIdAdd,
  SubBandIdRemove,
  UpdateSourceExplorer,
  UpdateTreeSource,
} from './../actions/source-explorer';

import {
  categorySource,
  childSource,
  customFilterSource,
  filterSourceLocation,
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

  it('handle AddCustomFilter', () => {
    sourceExplorerState = reducer(sourceExplorerState, new AddCustomFilter('/child', 'ips', '.*IPS.*'));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      customFiltersBySourceId: {
        '/child': [{ filter: '.*IPS.*', label: 'ips', subBandId: '' }],
      },
    });
  });

  it('handle AddFilter', () => {
    sourceExplorerState = reducer(sourceExplorerState, new NewSources(rootSource.id, [categorySource]));
    sourceExplorerState = reducer(sourceExplorerState, new NewSources(categorySource.id, [filterSourceLocation]));
    sourceExplorerState = reducer(sourceExplorerState, new AddFilter(filterSourceLocation));
    expect(sourceExplorerState.filtersByTarget).toEqual({ '/SequenceTracker': { meeting: [filterSourceLocation.id] } });
  });

  it('handle ApplyState', () => {
    sourceExplorerState = reducer(sourceExplorerState, new ApplyState(rootSource.url, rootSource.id));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
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
      actions: [
        { event: 'pin-add', name: 'Add Pin' },
      ],
    };

    const pin: RavenPin = { name: 'somePin', sourceId: '/' };
    sourceExplorerState = reducer(sourceExplorerState, new PinAdd(pin));

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
    sourceExplorerState = reducer(sourceExplorerState, new PinAdd(pin));

    // Finally remove the pin.
    sourceExplorerState = reducer(sourceExplorerState, new PinRemove('/'));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      pins: [],
      treeBySourceId: {
        ...sourceExplorerState.treeBySourceId,
        '/': {
          ...sourceExplorerState.treeBySourceId['/'],
          actions: [
            { event: 'pin-add', name: 'Add Pin' },
          ],
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
    sourceExplorerState = reducer(sourceExplorerState, new PinAdd(pin));

    // Finally rename the pin.
    sourceExplorerState = reducer(sourceExplorerState, new PinRename('/', 'someNewNamePin'));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      pins: [{
        ...pin,
        name: 'someNewNamePin',
      }],
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
    sourceExplorerState = reducer(sourceExplorerState, new NewSources(rootSource.id, [categorySource]));
    sourceExplorerState = reducer(sourceExplorerState, new NewSources(categorySource.id, [filterSourceLocation]));
    sourceExplorerState = reducer(sourceExplorerState, new AddFilter(filterSourceLocation));
    sourceExplorerState = reducer(sourceExplorerState, new RemoveFilter(filterSourceLocation));
    expect(sourceExplorerState.filtersByTarget).toEqual({ '/SequenceTracker': { meeting: [] } });
  });

  it('handle RemoveSource', () => {
    // First add a source we can remove.
    sourceExplorerState = reducer(sourceExplorerState, new NewSources(rootSource.id, [childSource]));
    sourceExplorerState = reducer(sourceExplorerState, new RemoveSource(childSource.id));
    expect(sourceExplorerState).toEqual({
      ...initialState,
    });
  });

  it('handle RemoveSourceEvent', () => {
    sourceExplorerState = reducer(sourceExplorerState, new RemoveSourceEvent(rootSource));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      fetchPending: true,
    });
  });

  it('handle SaveState', () => {
    sourceExplorerState = reducer(sourceExplorerState, new SaveState(rootSource, 'hello'));
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

  it('handle SetCustomFilter', () => {
    sourceExplorerState = reducer(sourceExplorerState, new NewSources(rootSource.id, [categorySource]));
    sourceExplorerState = reducer(sourceExplorerState, new NewSources(categorySource.id, [customFilterSource]));
    sourceExplorerState = reducer(sourceExplorerState, new SetCustomFilter(customFilterSource, 'jm0132'));
    expect(customFilterSource).toEqual({
      ...customFilterSource,
      filter: 'jm0132',
    });
  });

  it('handle SetCustomFilterSubBandId', () => {
    sourceExplorerState = reducer(sourceExplorerState, new AddCustomFilter('/child', 'ips', '.*IPS.*'));
    sourceExplorerState = reducer(sourceExplorerState, new SetCustomFilterSubBandId('/child', 'ips', '45'));
    expect(sourceExplorerState).toEqual({
      ...initialState,
      customFiltersBySourceId: {
        '/child': [{ filter: '.*IPS.*', label: 'ips', subBandId: '45' }],
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
          subBandIds: ['100'],
        },
      },
    });
  });

  it('handle SubBandIdRemove', () => {
    // First add a sub-band id we can remove.
    sourceExplorerState = reducer(sourceExplorerState, new SubBandIdAdd(rootSource.id, '100'));
    sourceExplorerState = reducer(sourceExplorerState, new SubBandIdRemove(['/'], '100'));
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
