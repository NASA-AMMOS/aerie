import { browser, ExpectedConditions as EC, logging } from 'protractor';
import { clickHarder, hasClass } from '../utils';
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
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(page.panelMenuIndex)),
      page.waitTimeout,
      'Panel menu did not appear',
    );
    expect(page.editorToolbarMenus.get(page.panelMenuIndex).isDisplayed()).toBe(
      true,
    );
  });

  it('[C136494] A user SHOULD be able to toggle the left panel off and on', () => {
    expect(page.leftPanel.isDisplayed()).toBe(true);

    page.panelButton.click();

    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(0)),
      page.waitTimeout,
      'Panels menu did not appear',
    );

    clickHarder('#sequencing-panels-left-toggle-button');

    browser.wait(
      EC.invisibilityOf(page.leftPanel),
      page.waitTimeout,
      'Left panel is still visible',
    );

    expect(page.leftPanel.isDisplayed()).toBe(false);

    page.panelButton.click();

    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(0)),
      page.waitTimeout,
      'Panels is not visible',
    );

    clickHarder('#sequencing-panels-left-toggle-button');

    browser.wait(
      EC.visibilityOf(page.leftPanel),
      page.waitTimeout,
      'Left panel is not visible',
    );

    expect(page.leftPanel.isDisplayed()).toBe(true);
  });

  it('[C136495] A user SHOULD be able to toggle the right panel off and on', () => {
    expect(page.rightPanel.isDisplayed()).toBe(true);

    page.panelButton.click();

    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(0)),
      page.waitTimeout,
      'Panels panel did not appear',
    );
    clickHarder('#sequencing-panels-right-toggle-button');
    browser.wait(EC.invisibilityOf(page.rightPanel), page.waitTimeout);

    expect(page.rightPanel.isDisplayed()).toBe(false);

    page.panelButton.click();

    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(0)),
      page.waitTimeout,
      'Panels panel did not appear',
    );
    clickHarder('#sequencing-panels-right-toggle-button');
    browser.wait(EC.visibilityOf(page.rightPanel), page.waitTimeout);

    expect(page.rightPanel.isDisplayed()).toBe(true);
  });

  it('[C141205] A user SHOULD be able to create a new editor panel', () => {
    page.panelButton.click();
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(page.panelMenuIndex)),
      page.waitTimeout,
    );
    clickHarder('#sequencing-add-editor-pane-button');

    expect(page.editorPanels.count()).toBe(2);
  });

  it('[C141206] An editor panel SHOULD be removed if there are no tabs AND there is not one editor panel', () => {
    page.prepareForCodeMirrorTesting();

    page.panelButton.click();
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(page.panelMenuIndex)),
      page.waitTimeout,
    );
    clickHarder('#sequencing-add-editor-pane-button');
    page.addTab();
    page.tabCloseButtons.get(0).click();
    page.tabCloseButtons.get(0).click();

    expect(page.editorPanels.count()).toBe(1);
  });

  it('[C141410] WHEN a user closes a tab resulting in no tabs in the only editor, the editor instance SHOULD still be there', () => {
    page.prepareForCodeMirrorTesting();

    page.tabCloseButtons.get(0).click();

    expect(page.editorPanels.count()).toBe(1);
  });

  it('[C143141] WHEN a user has 2 or more editor panels, they SHOULD be initially horizontal', () => {
    page.prepareForCodeMirrorTesting();

    page.panelButton.click();
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(page.panelMenuIndex)),
      page.waitTimeout,
    );
    clickHarder('#sequencing-add-editor-pane-button');

    expect(hasClass(page.editorPanelSplit, 'as-vertical')).toBe(true);
  });

  it('[C143142] WHEN a user has 2 or more editor panels, they SHOULD be able to toggle to vertical', () => {
    page.prepareForCodeMirrorTesting();

    page.panelButton.click();
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(page.panelMenuIndex)),
      page.waitTimeout,
    );
    clickHarder('#sequencing-add-editor-pane-button');
    page.panelButton.click();
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(page.panelMenuIndex)),
      page.waitTimeout,
    );
    clickHarder('#sequencing-toggle-editor-pane-direction');

    expect(hasClass(page.editorPanelSplit, 'as-horizontal')).toBe(true);
  });

  it('[C143143] WHEN a user has 2 or more editor panels in vertical, they SHOULD be able to toggle to horizontal', () => {
    page.prepareForCodeMirrorTesting();

    page.panelButton.click();
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(page.panelMenuIndex)),
      page.waitTimeout,
    );
    clickHarder('#sequencing-add-editor-pane-button');
    page.panelButton.click();
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(page.panelMenuIndex)),
      page.waitTimeout,
    );
    clickHarder('#sequencing-toggle-editor-pane-direction');
    page.panelButton.click();
    browser.wait(
      EC.visibilityOf(page.editorToolbarMenus.get(page.panelMenuIndex)),
      page.waitTimeout,
    );
    clickHarder('#sequencing-toggle-editor-pane-direction');

    expect(hasClass(page.editorPanelSplit, 'as-vertical')).toBe(true);
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
