/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  browser,
  by,
  element,
  promise,
} from 'protractor';

export class AppPage {
  band = element(by.css('.falcon-band'));
  settingsActivityBand = element(by.id('raven-settings-activity-band'));
  settingsAllBands = element(by.id('raven-settings-all-bands'));
  settingsFill = element(by.id('raven-settings-fill'));
  settingsHeight = element(by.id('raven-settings-height'));
  settingsLabel = element(by.id('raven-settings-label'));
  settingsResourceBand = element(by.id('raven-settings-resource-band'));
  settingsShowTooltip = element(by.id('raven-settings-show-tooltip'));
  settingsStateBand = element(by.id('raven-settings-state-band'));

  navigateTo(): promise.Promise<any> {
    return browser.get('/');
  }

  getAppTitle(): promise.Promise<string> {
    return element(by.id('app-title')).getText();
  }

  /**
   * Helper. Click via css class.
   */
  clickByCss(cssClass: string): void {
    element(by.css(cssClass)).click();
  }

  /**
   * Helper. Click via id.
   */
  clickById(id: string): void {
    element(by.id(id)).click();
  }

  /**
   * Helper. Clicks all ids given in the 'ids' parameter.
   */
  clickByIds(ids: string[]): void {
    ids.forEach(id => {
      element(by.id(id)).click();
    });
  }
}
