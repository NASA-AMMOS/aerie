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

describe('raven2 - settings - delete sub-band', () => {
  let bands: any;
  let page: AppPage;

  const source0: RavenTree = new RavenTree(1, 'SolarArrayGimbalParkAngle');
  const source1: RavenTree = new RavenTree(1, 'SolarArrayNorm2SunAngle');
  const source2: RavenTree = new RavenTree(1, 'SolarArrayRotationAngle');

  const ids: string[] = [
    'raven-tree-leucadia-expand',
    'raven-tree-EuropaSimulations-expand',
    'raven-tree-15F10_Cruise_Simulation_CheckoutActivities-expand',
    'raven-tree-Resources-expand',
    'raven-tree-Array-expand',
  ];

  beforeAll(async() => {
    page = new AppPage();
    page.navigateTo();
    clickByIds(ids);
    source0.open();
    source1.open();
    source2.open();
  });

  it('should initially have 3 bands drawn with the correct sources opened', async() => {
    bands = await probe(page.bands, 'bands');

    expect(bands.length).toEqual(3);
    expect(source0.getProp('opened')).toBe(true);
    expect(source1.getProp('opened')).toBe(true);
    expect(source2.getProp('opened')).toBe(true);
  });

  it('should properly delete the 3rd band and close the corresponding source', async() => {
    page.band2.click();
    page.selectedBandTab.click();
    page.settingsDeleteBand.click();

    bands = await probe(page.bands, 'bands');

    expect(bands.length).toEqual(2);
    expect(source0.getProp('opened')).toBe(true);
    expect(source1.getProp('opened')).toBe(true);
    expect(source2.getProp('opened')).toBe(false);
  });

  it('should properly delete an overlay band and close the corresponding source', async() => {
    page.band1.click();
    page.settingsOverlay.click();
    source2.open();
    page.settingsDeleteBand.click();

    bands = await probe(page.bands, 'bands');

    expect(bands.length).toEqual(2);
    expect(bands[0].subBands.length).toEqual(1);
    expect(bands[1].subBands.length).toEqual(1);
    expect(source0.getProp('opened')).toBe(true);
    expect(source1.getProp('opened')).toBe(false);
    expect(source2.getProp('opened')).toBe(true);
  });
});
