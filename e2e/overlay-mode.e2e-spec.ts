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

describe('raven2 - overlay mode', () => {
  let bands: any;
  let page: AppPage;

  const source0: RavenTree = new RavenTree(1, 'SolarArrayGimbalParkAngle');
  const source1: RavenTree = new RavenTree(1, 'SolarArrayNorm2SunAngle');
  const source2: RavenTree = new RavenTree(1, 'SolarArrayRotationAngle');
  const source3: RavenTree = new RavenTree(3, 'PostCal');
  const source4: RavenTree = new RavenTree(3, 'PreCal');

  const ids: string[] = [
    'raven-tree-leucadia-expand',
    'raven-tree-EuropaSimulations-expand',
    'raven-tree-15F10_Cruise_Simulation_CheckoutActivities-expand',
    'raven-tree-Resources-expand',
    'raven-tree-Array-expand',
  ];

  beforeAll(async () => {
    page = new AppPage();
    page.navigateTo();
    clickByIds(ids);
    source0.open();
  });

  it('after opening source0 there should be only one band', async () => {
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(1);
  });

  it('after opening source1 there should be two bands since we are not in overlay mode', async () => {
    source1.open();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(2);
  });

  it('selecting band0 should show the overlay toggle for that band, and it should be unchecked', async () => {
    page.band0.click();
    const checked = await page.settingsOverlay.getAttribute('ng-reflect-checked');
    expect(checked).toBe('false');
  });

  it('clicking the overlay toggle should enable overlay mode', async () => {
    page.settingsOverlay.click();
    const checked = await page.settingsOverlay.getAttribute('ng-reflect-checked');
    expect(checked).toBe('true');
  });

  it('opening source2 should overlay a new band on the selected band (band0)', async () => {
    source2.open();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(2);
    expect(bands[0].subBands.length).toEqual(2);
    expect(bands[1].subBands.length).toEqual(1);
    expect(bands[0].subBands[0].name).toEqual(source0.name);
    expect(bands[0].subBands[1].name).toEqual(source2.name);
  });

  it('closing source1 should remove a band', async () => {
    source1.close();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(1);
    expect(bands[0].subBands.length).toEqual(2);
  });

  it('closing source0 should remove a band from the first band', async () => {
    source0.close();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(1);
    expect(bands[0].subBands.length).toEqual(1);
  });

  it('overlaying a 3-legend source should add 3 sub-bands to the selected band (as long as those legends are not already displayed in other bands)', async () => {
    clickByIds([
      'raven-tree-Resources-collapse',
      'raven-tree-Activities by Type-expand',
      'raven-tree-DSN-expand',
    ]);
    source3.open();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(1);
    expect(bands[0].subBands.length).toEqual(4);
  });

  it('overlaying a same 3-legend source should not add any additional sub-bands to the selected band', async () => {
    source4.open();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(1);
    expect(bands[0].subBands.length).toEqual(4);
  });

  it('closing source3 (which has the same legends as source4) should not remove 3 sub-bands from the selected band', async () => {
    source3.close();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(1);
    expect(bands[0].subBands.length).toEqual(4);
  });

  it('closing source4 should remove 3 more sub-bands from the selected band', async () => {
    source4.close();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(1);
    expect(bands[0].subBands.length).toEqual(1);
  });

  it('turning off overlay mode and drawing a 3-legend source should add 3 new bands', async () => {
    page.settingsOverlay.click();
    source3.open();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(4);
    expect(bands[0].subBands.length).toEqual(1);
    expect(bands[1].subBands.length).toEqual(1);
    expect(bands[2].subBands.length).toEqual(1);
    expect(bands[3].subBands.length).toEqual(1);
  });

  it('turning back on overlay mode and drawing another 3-legend source should add those points to the existing legend sub-bands and NOT overlay them', async () => {
    page.settingsOverlay.click();
    source4.open();
    bands = await probe(page.bands, 'bands');
    expect(bands.length).toEqual(4);
    expect(bands[0].subBands.length).toEqual(1);
    expect(bands[1].subBands.length).toEqual(1);
    expect(bands[2].subBands.length).toEqual(1);
    expect(bands[3].subBands.length).toEqual(1);
  });
});
