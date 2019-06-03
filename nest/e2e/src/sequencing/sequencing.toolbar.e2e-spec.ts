import { browser, logging, protractor } from 'protractor';
import { hasClass, waitTilClickable } from '../utils';
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
    page.openToolbar();
    page.clickToolbarItem(page.helpButtonIndex);

    waitTilClickable(page.helpDialog, 10000);
    expect(page.helpDialog.isDisplayed()).toBe(true);
  });

  it('[C135064] WHEN a user clicks the toggle theme button, the theme should switch to the alternative theme', () => {
    const darkTheme = 'cm-s-monokai';
    const lightTheme = 'cm-s-default';

    expect(hasClass(page.codeMirrorEditor, darkTheme)).toBe(true);
    expect(hasClass(page.codeMirrorEditor, lightTheme)).toBe(false);

    page.openToolbar();
    page.clickToolbarItem(page.colorschemeButtonIndex);

    expect(hasClass(page.codeMirrorEditor, darkTheme)).toBe(false);
    expect(hasClass(page.codeMirrorEditor, lightTheme)).toBe(true);
  });

  it('[C135063] WHEN the editor is fullscreen, the user can press ESC to exit fullscreen', () => {
    page.openToolbar();
    page.clickToolbarItem(page.fullscreenButtonIndex);

    expect(hasClass(page.codeMirrorEditor, 'CodeMirror-fullscreen')).toBe(true);

    page.sendGlobalKeys(protractor.Key.ESCAPE);

    expect(hasClass(page.codeMirrorEditor, 'CodeMirror-fullscreen')).toBe(
      false,
    );
  });

  it('[C135062] WHEN a user clicks the fullscreen button, the editor SHOULD be fullscreen', () => {
    page.openToolbar();
    page.clickToolbarItem(page.fullscreenButtonIndex);

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
