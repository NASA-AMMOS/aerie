/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { AppPage } from './app.po';

import {
  browser,
  Key,
} from 'protractor';

describe('raven2 - settings - resource band', () => {
  const initialResourceHeight = '100';
  const resourceName = 'SolarArrayGimbalParkAngle';

  let page: AppPage;

  const ids: string[] = [
    'raven-tree-leucadia-expand',
    'raven-tree-EuropaSimulations-expand',
    'raven-tree-15F10_Cruise_Simulation_CheckoutActivities-expand',
    'raven-tree-Resources-expand',
    'raven-tree-Array-expand',
  ];

  beforeAll(() => {
    page = new AppPage();
    page.navigateTo();
    page.clickByIds(ids);
    page.clickById(`raven-tree-${resourceName}-open`);
    page.clickByCss('.falcon-band');
  });

  it('all band settings and resource band settings should be present', () => {
    expect(page.settingsAllBands.isPresent()).toBeTruthy();
    expect(page.settingsResourceBand.isPresent()).toBeTruthy();
  });

  it('activity band settings and state band setting should not be present', () => {
    expect(page.settingsActivityBand.isPresent()).toBeFalsy();
    expect(page.settingsStateBand.isPresent()).toBeFalsy();
  });

  it('label in the settings should equal the label in the band', async () => {
    const bandLabel = await page.band.getAttribute('label');
    const settingsLabel = await page.settingsLabel.getAttribute('value');
    expect(bandLabel).toEqual(resourceName);
    expect(settingsLabel).toEqual(resourceName);
  });

  it('clearing label in the settings should clear the label in the band', async () => {
    page.settingsLabel.clear();

    const bandLabel = await page.band.getAttribute('label');
    const settingsLabel = await page.settingsLabel.getAttribute('value');
    expect(bandLabel).toEqual('');
    expect(settingsLabel).toEqual('');
  });

  it('inputting a label in the settings should change the label in the band', async () => {
    const inputLabel = 'Hello, World!';
    page.settingsLabel.sendKeys(inputLabel);
    page.settingsLabel.sendKeys(Key.ENTER);

    const bandLabel = await page.band.getAttribute('label');
    const settingsLabel = await page.settingsLabel.getAttribute('value');
    expect(bandLabel).toEqual(inputLabel);
    expect(settingsLabel).toEqual(inputLabel);
  });

  it('height in the settings should equal the height in the band', async () => {
    const bandHeight = await page.band.getAttribute('height');
    const settingsHeight = await page.settingsHeight.getAttribute('aria-valuenow');
    expect(bandHeight.toString()).toEqual(initialResourceHeight);
    expect(settingsHeight).toEqual(initialResourceHeight);
  });

  it('changing the height in the settings should change the height in the band', async () => {
    // Setting x: 1 for the dragAndDrop action changes height from 100 -> 251.
    // This test could possibly fail if this 100 -> 251 mapping changes depending on the browser.
    // TODO: Find a more deterministic way to do this.
    browser.actions().dragAndDrop(page.settingsHeight, { x: 1, y: 0 }).perform();

    const newHeight = 251;
    const bandHeight = await page.band.getAttribute('height');
    const settingsHeight = await page.settingsHeight.getAttribute('aria-valuenow');
    expect(bandHeight.toString()).toEqual(newHeight.toString());
    expect(settingsHeight).toEqual(newHeight.toString());
  });

  it('showTooltip in the settings should equal showTooltip in the band', async () => {
    const bandShowTooltip = await page.band.getAttribute('showTooltip');
    const settingsShowTooltip = await page.settingsShowTooltip.getAttribute('ng-reflect-checked');
    expect(bandShowTooltip.toString()).toEqual('true');
    expect(settingsShowTooltip).toEqual('true');
  });

  it('toggling showTooltip to false in the settings should toggle showTooltip to false in the band', async () => {
    page.settingsShowTooltip.click();

    const bandShowTooltip = await page.band.getAttribute('showTooltip');
    const settingsShowTooltip = await page.settingsShowTooltip.getAttribute('ng-reflect-checked');
    expect(bandShowTooltip.toString()).toEqual('false');
    expect(settingsShowTooltip).toEqual('false');
  });

  it('fill in the settings should equal fill in the band', async () => {
    const bandFill = await page.band.getAttribute('fill');
    const settingsFill = await page.settingsFill.getAttribute('ng-reflect-checked');
    expect(bandFill.toString()).toEqual('false');
    expect(settingsFill).toEqual('false');
  });

  it('toggling fill to true in the settings should toggle fill to true in the band', async () => {
    page.settingsFill.click();

    const bandFill = await page.band.getAttribute('fill');
    const settingsFill = await page.settingsFill.getAttribute('ng-reflect-checked');
    expect(bandFill.toString()).toEqual('true');
    expect(settingsFill).toEqual('true');
  });
});
