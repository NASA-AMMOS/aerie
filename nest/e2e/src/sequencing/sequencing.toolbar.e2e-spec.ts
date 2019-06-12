import {
  browser,
  ExpectedConditions as EC,
  logging,
  protractor,
} from 'protractor';
import { clickHarder, hasClass } from '../utils';
import { SequencingPage } from './sequencing.po';

describe('/sequencing.toolbar', () => {
  let page: SequencingPage;

  beforeEach(() => {
    page = new SequencingPage();
    page.navigateTo('sequencing');
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
    clickHarder('#sequencing-editor-help-button');
    browser.wait(
      EC.visibilityOf(page.helpDialog),
      page.waitTimeout,
      'Help dialog did not appear',
    );

    expect(page.helpDialog.isDisplayed()).toBe(true);
  });

  it('[C135064] WHEN a user clicks the toggle theme button, the theme should switch to the alternative theme', () => {
    page.prepareForCodeMirrorTesting();
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
    clickHarder('#sequencing-editor-toggle-color-scheme-button');

    expect(hasClass(page.codeMirrorEditor, lightTheme)).toBe(true);
  });

  it('[C135063] WHEN the editor is fullscreen, the user can press ESC to exit fullscreen', () => {
    page.prepareForCodeMirrorTesting();
    page.fullscreenButton.click();

    expect(hasClass(page.codeMirrorEditor, 'CodeMirror-fullscreen')).toBe(true);

    page.sendGlobalKeys(protractor.Key.ESCAPE);

    expect(hasClass(page.codeMirrorEditor, 'CodeMirror-fullscreen')).toBe(
      false,
    );
  });

  it('[C135062] WHEN a user clicks the fullscreen button, the editor SHOULD be fullscreen', () => {
    page.prepareForCodeMirrorTesting();
    page.fullscreenButton.click();

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
