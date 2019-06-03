import { browser, ExpectedConditions as EC, logging } from 'protractor';
import { clickHarder } from '../utils';
import { SequencingPage } from './sequencing.po';

describe('/sequencing.panels', () => {
  let page: SequencingPage;

  beforeEach(() => {
    page = new SequencingPage();
    page.navigateTo('sequencing');
    // wait needed because CodeMirror mounting is stalling the test causing it to exit due to timeout
    browser.waitForAngularEnabled(false);
    page.refreshBrowser();
  });

  it('[C136493] A user SHOULD be able to open the panel menu', () => {
    page.panelButton.click();
    browser.wait(EC.visibilityOf(page.panelMenu), 5000);
    expect(page.panelMenu.isDisplayed()).toBe(true);
  });

  it('[C136494] A user SHOULD be able to toggle the left panel off and on', () => {
    expect(page.leftPanel.isDisplayed()).toBe(true);

    page.panelButton.click();

    browser.wait(
      EC.visibilityOf(page.panelMenu),
      1000,
      'Panels menu should appear',
    );

    clickHarder('#sequencing-panels-left-toggle-button');

    browser.wait(
      EC.invisibilityOf(page.leftPanel),
      2000,
      'Left panel should no longer be visible',
    );

    expect(page.leftPanel.isDisplayed()).toBe(false);

    page.panelButton.click();

    browser.wait(
      EC.visibilityOf(page.panelMenu),
      1000,
      'Panels menu should appear',
    );

    clickHarder('#sequencing-panels-left-toggle-button');

    browser.wait(
      EC.visibilityOf(page.leftPanel),
      1000,
      'Left panel should be visible',
    );

    expect(page.leftPanel.isDisplayed()).toBe(true);
  });

  it('[C136495] A user SHOULD be able to toggle the right panel off and on', () => {
    expect(page.rightPanel.isDisplayed()).toBe(true);

    page.panelButton.click();

    browser.wait(EC.visibilityOf(page.panelMenu), 5000);
    clickHarder('#sequencing-panels-right-toggle-button');
    browser.wait(EC.invisibilityOf(page.rightPanel), 5000);

    expect(page.rightPanel.isDisplayed()).toBe(false);

    page.panelButton.click();

    browser.wait(EC.visibilityOf(page.panelMenu), 5000);
    clickHarder('#sequencing-panels-right-toggle-button');
    browser.wait(EC.visibilityOf(page.rightPanel), 5000);

    expect(page.rightPanel.isDisplayed()).toBe(true);
  });

  afterEach(async () => {
    // Assert that there are no errors emitted from the browser
    const logs = await browser
      .manage()
      .logs()
      .get(logging.Type.BROWSER);
    expect(logs).not.toContain(
      jasmine.objectContaining({
        level: logging.Level.SEVERE,
      } as logging.Entry),
    );
  });
});
