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
  appTile =               element(by.css('.app-title'));
  bands =                 element(by.css('.raven-bands-0'));
  band0 =                 element(by.css('.raven-band-0'));
  band1 =                 element(by.css('.raven-band-1'));
  band2 =                 element(by.css('.raven-band-2'));
  timeline0 =             element(by.css('.timeline-0'));
  timeline1 =             element(by.css('.timeline-1'));

  selectedBandTab =       element(by.id('mat-tab-label-0-0'));
  settingsDeleteBand =    element(by.id('raven-settings-delete-band'));
  settingsOverlay =       element(by.id('raven-settings-overlay'));

  navigateTo(): promise.Promise<any> {
    return browser.get('/');
  }
}
