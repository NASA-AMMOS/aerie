import { browser, ExpectedConditions as EC, logging } from 'protractor';
import { clickHarder, hasClass } from '../utils';
import { FalconPage } from './falcon.po';

describe('toolbar', () => {
  let page: FalconPage;

  beforeEach(() => {
    page = new FalconPage();
    page.navigateTo('falcon');
    // wait needed because CodeMirror mounting is stalling the test causing it to exit due to timeout
    browser.waitForAngularEnabled(false);
    page.prepareForCodeMirrorTesting();
  });

  it('[C135060] WHEN the user clicks the help button, a dialog pops up', () => {
    browser.wait(
      EC.visibilityOf(page.toolsButton),
      page.waitTimeout,
      'Tools button is not visible',
    );
    page.toolsButton.click();
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(0)),
      page.waitTimeout,
      'Tools menu did not appear',
    );
    clickHarder('#falcon-editor-help-button');
    browser.wait(
      EC.visibilityOf(page.helpDialog),
      page.waitTimeout,
      'Help dialog did not appear',
    );

    expect(page.helpDialog.isDisplayed()).toBe(true);
  });

  it('[C135064] WHEN a user clicks the toggle theme button, the theme should switch to the alternative theme', () => {
    const darkTheme = 'cm-s-monokai';
    const lightTheme = 'cm-s-default';

    expect(hasClass(page.codeMirrorEditor, darkTheme)).toBe(true);

    browser.wait(
      EC.visibilityOf(page.toolsButton),
      page.waitTimeout,
      'Tools button is not visible',
    );
    page.toolsButton.click();
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(0)),
      page.waitTimeout,
      'Tools menu did not appear',
    );
    clickHarder('#falcon-editor-toggle-color-scheme-button');

    expect(hasClass(page.codeMirrorEditor, lightTheme)).toBe(true);
  });

  it('[C135062] WHEN a user clicks the fullscreen button, the editor SHOULD be fullscreen', () => {
    page.editorToolsPanelButton.click();
    clickHarder('#seq-editor-fullscreen-button');

    expect(hasClass(page.codeMirrorEditor, 'CodeMirror-fullscreen')).toBe(true);
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
