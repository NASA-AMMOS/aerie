/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  by,
  element,
} from 'protractor';

import {
  AppPage,
  clickByIds,
  probe,
  RavenTree,
} from './utils';

/**
 * Note: Source0 and Source1 need to have the same legends for these tests.
 */
describe('raven2 - activities by type', () => {
  let bands: any;
  let page: AppPage;

  const source0: RavenTree = new RavenTree(3, 'PostCal');
  const source1: RavenTree = new RavenTree(3, 'PreCal');

  const ids: string[] = [
    'raven-tree-leucadia-expand',
    'raven-tree-EuropaSimulations-expand',
    'raven-tree-15F10_Cruise_Simulation_CheckoutActivities-expand',
    'raven-tree-Activities by Type-expand',
    'raven-tree-DSN-expand',
  ];

  beforeAll(async () => {
    page = new AppPage();
    page.navigateTo();
    clickByIds(ids);
    source0.open();
  });

  it('opening source0 that has 3 legends should draw 3 bands', async () => {
    bands = await probe(element(by.id('raven-bands-0')), 'bands');
    expect(bands.length).toEqual(source0.legendCount);
  });

  it('bands sourceIds should be defined for the source0 id and map to the source0 name', async () => {
    for (let i = 0, l = source0.legendCount; i < l; ++i) {
      expect(bands[i].sourceIds[source0.id]).toBeDefined();
      expect(bands[i].sourceIds[source0.id]).toEqual(source0.name);
    }
  });

  it('the source0 bandIds should be defined for the correct band id and point to the correct band name', async () => {
    const sourceBandIds = source0.tree[source0.id].bandIds;

    for (let i = 0, l = source0.legendCount; i < l; ++i) {
      expect(sourceBandIds[bands[i].id]).toBeDefined();
      expect(sourceBandIds[bands[i].id]).toEqual(bands[i].name);
    }
  });

  it('opening source1 which has the same legends as source0 should not draw any additional bands', async () => {
    source1.open();
    bands = await probe(element(by.id('raven-bands-0')), 'bands');
    expect(bands.length).toEqual(3);
  });

  it('bands sourceIds should be defined for source0 and source1 and map to the source0 or source1 name respectively', async () => {
    // Source0.
    for (let i = 0, l = source0.legendCount; i < l; ++i) {
      expect(bands[i].sourceIds[source0.id]).toBeDefined();
      expect(bands[i].sourceIds[source0.id]).toEqual(source0.name);
    }

    // Source1.
    for (let i = 0, l = source1.legendCount; i < l; ++i) {
      expect(bands[i].sourceIds[source1.id]).toBeDefined();
      expect(bands[i].sourceIds[source1.id]).toEqual(source1.name);
    }
  });

  it('the source1 bandIds should be defined for the correct band id and point to the correct band name', async () => {
    const sourceBandIds = source1.tree[source1.id].bandIds;

    for (let i = 0, l = source1.legendCount; i < l; ++i) {
      expect(sourceBandIds[bands[i].id]).toBeDefined();
      expect(sourceBandIds[bands[i].id]).toEqual(bands[i].name);
    }
  });

  it('after closing source0, 3 bands should still be drawn', async () => {
    source0.close();
    bands = await probe(element(by.id('raven-bands-0')), 'bands');
    expect(bands.length).toEqual(3);
  });

  it('bands sourceIds should be defined for the source1 id and map to the source1 name, they should not correspond to source0 anymore', async () => {
    for (let i = 0, l = source0.legendCount; i < l; ++i) {
      expect(bands[i].sourceIds[source0.id]).toBeUndefined();
    }

    for (let i = 0, l = source1.legendCount; i < l; ++i) {
      expect(bands[i].sourceIds[source1.id]).toBeDefined();
      expect(bands[i].sourceIds[source1.id]).toEqual(source1.name);
    }
  });

  it('source0 should not point to any bandIds', async () => {
    const sourceBandIds = source0.tree[source0.id].bandIds;
    expect(sourceBandIds).toEqual({});
  });

  it('source1 should still point to the correct bandIds', async () => {
    const sourceBandIds = source1.tree[source1.id].bandIds;

    for (let i = 0, l = source1.legendCount; i < l; ++i) {
      expect(sourceBandIds[bands[i].id]).toBeDefined();
      expect(sourceBandIds[bands[i].id]).toEqual(bands[i].name);
    }
  });

  it('after closing source1, no bands should be drawn', async () => {
    source1.close();

    // TODO: This is a hack to see if no bands are drawn.
    // We should actually get bands data and check it here.
    expect(element(by.id('get-started-message')).isPresent()).toBeTruthy();
    expect(element(by.id('raven-timeline')).isPresent()).toBeFalsy();
  });

  it('source1 should not point to any bandIds', async () => {
    const sourceBandIds = source1.tree[source1.id].bandIds;
    expect(sourceBandIds).toEqual({});
  });
});
