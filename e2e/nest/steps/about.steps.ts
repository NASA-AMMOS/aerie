/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { expect } from 'chai';
import { Before, Given, Then, When } from 'cucumber';
import { $ } from 'protractor';
import { config } from '../../../src/config';
import { NestAppPage } from '../../utils';

let app: NestAppPage;

// An About dialog is displayed
Before(() => {
  app = new NestAppPage();
});
Given('A button called About in the Nest sidenav exists', () =>
  app.navigateTo(),
);
When('A user clicks the button', () => app.openNestAboutDialog());
Then(
  'The name of the accessible modules should be displayed in a dialog',
  async () => {
    for (let i = 0, l = config.appModules.length; i < l; ++i) {
      const { title } = config.appModules[i];
      const isPresent = await $(`.title-${title}`).isPresent();
      expect(isPresent).to.equal(true);
    }
  },
);
Then('The version of each module should be displayed', async () => {
  for (let i = 0, l = config.appModules.length; i < l; ++i) {
    const { title } = config.appModules[i];
    const isPresent = await $(`.version-${title}`).isPresent();
    expect(isPresent).to.equal(true);
  }
});

// Module versions are displayed using semver
Before(() => {
  app = new NestAppPage();
});
Given('The About dialog is visible', () => app.navigateTo());
When('The versions are displayed', () => app.openNestAboutDialog());
Then('They should be in the form MAJOR.MINOR.PATCH', async () => {
  for (let i = 0, l = config.appModules.length; i < l; ++i) {
    const { title } = config.appModules[i];
    const version = await $(`.version-${title}`).getText();
    expect(version.match(/\d+\.\d+\.\d+/)).to.not.equal(null);
  }
});
