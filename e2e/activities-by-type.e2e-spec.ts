/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

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
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(source0.legendCount);
  });

  it('band source ids and names should point to source0', async () => {
    for (let i = 0, l = source0.legendCount; i < l; ++i) {
      expect(bands[i].subBands[0].sourceIds).toBeDefined();
      expect(bands[i].subBands[0].sourceIds[source0.id]).toEqual(source0.id);
    }
  });

  it('the source0 band ids should be defined for the correct band id and point to the correct band name', async () => {
    const sourceSubBandIds = source0.tree[source0.id].subBandIds;

    for (let i = 0, l = source0.legendCount; i < l; ++i) {
      const subBandId = bands[i].subBands[0].id;
      expect(sourceSubBandIds[subBandId]).toBeDefined();
      expect(sourceSubBandIds[subBandId]).toEqual(subBandId);
    }
  });

  it('opening source1 which has the same legends as source0 should not draw any additional bands', async () => {
    source1.open();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(3);
  });

  it('bands source ids and names should point to source0 and source1 correctly', async () => {
    for (let i = 0, l = source0.legendCount; i < l; ++i) {
      expect(bands[i].subBands[0].sourceIds).toBeDefined();
      expect(bands[i].subBands[0].sourceIds[source0.id]).toEqual(source0.id);
      expect(bands[i].subBands[0].sourceIds[source1.id]).toEqual(source1.id);
    }
  });

  it('the source1 band ids should be defined for the correct band id and point to the correct band name', async () => {
    const sourceSubBandIds = source1.tree[source1.id].subBandIds;

    for (let i = 0, l = source1.legendCount; i < l; ++i) {
      const subBandId = bands[i].subBands[0].id;
      expect(sourceSubBandIds[subBandId]).toBeDefined();
      expect(sourceSubBandIds[subBandId]).toEqual(subBandId);
    }
  });

  it('after closing source0, 3 bands should still be drawn', async () => {
    source0.close();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(3);
  });

  it('bands source ids and names should be defined for source1, they should not be defined for source0 anymore', async () => {
    for (let i = 0, l = source0.legendCount; i < l; ++i) {
      expect(bands[i].subBands[1]).toBeUndefined();
    }

    for (let i = 0, l = source1.legendCount; i < l; ++i) {
      expect(bands[i].subBands[0].sourceIds).toBeDefined();
      expect(bands[i].subBands[0].sourceIds[source1.id]).toEqual(source1.id);
    }
  });

  it('source0 should not point to any subBandIds', async () => {
    const sourceSubBandIds = source0.tree[source0.id].subBandIds;
    expect(sourceSubBandIds).toEqual({});
  });

  it('source1 should still point to the correct subBandIds', async () => {
    const sourceSubBandIds = source1.tree[source1.id].subBandIds;

    for (let i = 0, l = source1.legendCount; i < l; ++i) {
      const subBandId = bands[i].subBands[0].id;
      expect(sourceSubBandIds[subBandId]).toBeDefined();
      expect(sourceSubBandIds[subBandId]).toEqual(subBandId);
    }
  });

  it('after closing source1, no bands should be drawn', async () => {
    source1.close();
    expect(page.timeline0.isPresent()).toBeFalsy();
  });

  it('source1 should not point to any subBandIds', async () => {
    const sourceSubBandIds = source1.tree[source1.id].subBandIds;
    expect(sourceSubBandIds).toEqual({});
  });
});
