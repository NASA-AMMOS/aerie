import { browser, logging } from 'protractor';
import { SequencingPage } from './sequencing.po';

describe('/sequencing.pageinit', () => {
  let page: SequencingPage;

  beforeEach(() => {
    page = new SequencingPage();
    page.navigateTo('sequencing');
    // wait needed because CodeMirror mounting is stalling the test causing it to exit due to timeout
    browser.waitForAngularEnabled(false);
  });

  it('[C136471] WHEN the Falcon is loaded, the browser tab title SHOULD be Falcon', () => {
    expect(browser.getTitle()).toBe(page.config.title);
  });

  it("[C135056] WHEN the sequencing page is loaded, the title should say 'Sequencing'", () => {
    expect(page.routeTitle.getText()).toBe(page.config.title);
  });

  it('[C135057] WHEN no command dictionary is loaded, the user should be prompted to select one', () => {
    expect(page.noCommandsPrompt.isDisplayed()).toBe(true);
  });

  it('[C135061] WHEN the user loads the page, they SHOULD NOT see the editor', () => {
    expect(page.codeMirrorEditor.isDisplayed()).toBe(false);
  });

  it('[C135058] The tools toggle button SHOULD NOT be visible when the page is loaded', () => {
    expect(page.toolsToggleButton.isPresent()).toBe(false);
  });

  it('[C136496] A user SHOULD be shown all 3 panels on load', () => {
    expect(page.leftPanel.isDisplayed()).toBe(true);
    expect(page.middlePanel.isDisplayed()).toBe(true);
    expect(page.rightPanel.isDisplayed()).toBe(true);
  });

  it('[C139217] There SHOULD be no tabs opened when page first loads', () => {
    expect(page.tabs.count()).toBe(0);
  });

  it('[C139218] The create tab button SHOULD NOT be visible when the page first loads', () => {
    expect(page.addTabButton.isPresent()).toBe(false);
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
