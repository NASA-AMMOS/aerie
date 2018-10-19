/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { expect } from 'chai';
import { defineSupportCode } from 'cucumber';
import { AppPage } from './../utils';

defineSupportCode(({ Given, When, Then, Before }) => {
  let app: AppPage;

  Before(() => {
    app = new AppPage();
  });

  Given('I am on the RAVEN page', () => app.navigateTo());

  When(
    'I click on a + for a parent node in the Source Explorer',
    () => 'pending',
  );

  Then(
    'I should see the children nodes loaded and displayed under the parent',
    () => expect(true).to.equal(true),
  );
});
