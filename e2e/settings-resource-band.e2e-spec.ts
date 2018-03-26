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
  Key,
} from 'protractor';

import {
  AppPage,
  clickById,
  clickByIds,
  get,
} from './utils';

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
    clickByIds(ids);
    clickById(`raven-tree-${resourceName}-open`);
    page.band0.click();
  });

  it('selected band settings should be present', () => {
    expect(page.settingsSelectedBand.isPresent()).toBeTruthy();
  });

  it('label in the settings should equal the label in the band', async () => {
    const bandLabel = await get(page.resourceBand, 'label');
    const settingsLabel = await page.settingsLabel.getAttribute('value');
    expect(bandLabel).toEqual(resourceName);
    expect(settingsLabel).toEqual(resourceName);
  });

  it('clearing label in the settings should clear the label in the band', async () => {
    page.settingsLabel.clear();

    const bandLabel = await get(page.resourceBand, 'label');
    const settingsLabel = await page.settingsLabel.getAttribute('value');
    expect(bandLabel).toEqual('');
    expect(settingsLabel).toEqual('');
  });

  it('inputting a label in the settings should change the label in the band', async () => {
    const inputLabel = 'Hello, World!';
    page.settingsLabel.sendKeys(inputLabel);
    page.settingsLabel.sendKeys(Key.ENTER);

    const bandLabel = await get(page.resourceBand, 'label');
    const settingsLabel = await page.settingsLabel.getAttribute('value');
    expect(bandLabel).toEqual(inputLabel);
    expect(settingsLabel).toEqual(inputLabel);
  });

  it('height in the settings should equal the height in the band', async () => {
    const bandHeight = await get(page.compositeBand, 'height');
    const settingsHeight = await page.settingsHeight.getAttribute('aria-valuenow');
    expect(bandHeight.toString()).toEqual(initialResourceHeight);
    expect(settingsHeight).toEqual(initialResourceHeight);
  });

  it('showTooltip in the settings should equal showTooltip in the band', async () => {
    const bandShowTooltip = await get(page.compositeBand, 'showTooltip');
    const settingsShowTooltip = await page.settingsShowTooltip.getAttribute('ng-reflect-checked');
    expect(bandShowTooltip.toString()).toEqual('true');
    expect(settingsShowTooltip).toEqual('true');
  });

  it('toggling showTooltip to false in the settings should toggle showTooltip to false in the band', async () => {
    page.settingsShowTooltip.click();

    const bandShowTooltip = await get(page.compositeBand, 'showTooltip');
    const settingsShowTooltip = await page.settingsShowTooltip.getAttribute('ng-reflect-checked');
    expect(bandShowTooltip.toString()).toEqual('false');
    expect(settingsShowTooltip).toEqual('false');
  });

  it('fill in the settings should equal fill in the band', async () => {
    const bandFill = await get(page.resourceBand, 'fill');
    const settingsFill = await page.settingsFill.getAttribute('ng-reflect-checked');
    expect(bandFill.toString()).toEqual('false');
    expect(settingsFill).toEqual('false');
  });

  it('toggling fill to true in the settings should toggle fill to true in the band', async () => {
    page.settingsFill.click();

    const bandFill = await get(page.resourceBand, 'fill');
    const settingsFill = await page.settingsFill.getAttribute('ng-reflect-checked');
    expect(bandFill.toString()).toEqual('true');
    expect(settingsFill).toEqual('true');
  });

  it('interpolation in the settings should equal interpolation in the band', async () => {
    const bandInterpolation = await get(page.resourceBand, 'interpolation');
    const settingsInterpolation = await page.settingsInterpolation.getAttribute('ng-reflect-value');
    expect(bandInterpolation).toEqual('linear');
    expect(settingsInterpolation).toEqual('linear');
  });

  it('changing interpolation to constant in the settings should change it to constant in the band', async () => {
    page.settingsInterpolation.click();
    element(by.css('mat-option[ng-reflect-value=constant]')).click();

    const bandInterpolation = await get(page.resourceBand, 'interpolation');
    const settingsInterpolation = await page.settingsInterpolation.getAttribute('ng-reflect-value');
    expect(bandInterpolation).toEqual('constant');
    expect(settingsInterpolation).toEqual('constant');
  });

  it('changing interpolation to none in the settings should change it to none in the band', async () => {
    page.settingsInterpolation.click();
    element(by.css('mat-option[ng-reflect-value=none]')).click();

    const bandInterpolation = await get(page.resourceBand, 'interpolation');
    const settingsInterpolation = await page.settingsInterpolation.getAttribute('ng-reflect-value');
    expect(bandInterpolation).toEqual('none');
    expect(settingsInterpolation).toEqual('none');
  });

  it('rescale in the settings should equal rescale in the band', async () => {
    const bandRescale = await get(page.resourceBand, 'rescale');
    const settingsRescale = await page.settingsRescale.getAttribute('ng-reflect-checked');
    expect(bandRescale.toString()).toEqual('true');
    expect(settingsRescale).toEqual('true');
  });

  it('toggling rescale in the settings should toggle rescale in the band', async () => {
    page.settingsRescale.click();

    const bandRescale = await get(page.resourceBand, 'rescale');
    const settingsRescale = await page.settingsRescale.getAttribute('ng-reflect-checked');
    expect(bandRescale.toString()).toEqual('false');
    expect(settingsRescale).toEqual('false');
  });

  it('showIcon in the settings should equal showIcon in the band', async () => {
    const bandShowIcon = await get(page.resourceBand, 'showIcon');
    const settingsShowIcon = await page.settingsShowIcon.getAttribute('ng-reflect-checked');
    expect(bandShowIcon.toString()).toEqual('false');
    expect(settingsShowIcon).toEqual('false');
  });

  it('toggling showIcon in the settings should toggle showIcon in the band', async () => {
    page.settingsShowIcon.click();

    const bandShowIcon = await get(page.resourceBand, 'showIcon');
    const settingsShowIcon = await page.settingsShowIcon.getAttribute('ng-reflect-checked');
    expect(bandShowIcon.toString()).toEqual('true');
    expect(settingsShowIcon).toEqual('true');
  });
});
