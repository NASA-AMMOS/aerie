import { browser, by, element } from 'protractor';
import { config } from '../../../src/config';
import { click } from '../utils';

export class SequencingPage {
  routeTitle = element(by.css('.top-bar-title'));

  config = config.appModules[1];

  waitTimeout = 5000;

  planAppNavButton = element(by.css('a.nav-item:nth-child(1)'));
  sequencingAppNavButton = element(by.css('a.nav-item:nth-child(2)'));

  commandDictionarySelect = element(by.css('#sequencing-command-select'));
  testCommandDictionary = element(by.id('mat-option-1'));
  commandList = element(by.id('sequencing-command-list'));
  commands = element.all(by.css('.mat-list-text'));
  noCommandsPrompt = element(by.id('sequencing-no-commands-prompt'));

  codeMirrorEditor = element(
    by.css('#sequencing-editor-mount > div:nth-of-type(1)'),
  );
  codeMirrorTextArea = element(
    by.css('#sequencing-editor-mount > div > div > textarea'),
  );
  codeMirrorWrapper = element(by.css('.CodeMirror-wrap'));

  hintsContainer = element(by.css('.CodeMirror-hints'));

  helpDialog = element(by.className('mat-dialog-container'));

  editorToolbarMenus = element.all(by.className('mat-menu-content'));
  panelMenuIndex = 0;
  toolsMenuIndex = 1;

  panelButton = element(by.id('sequencing-panels-button'));
  leftPanelToggleButton = element(
    by.id('sequencing-panels-left-toggle-button'),
  );
  rightPanelToggleButton = element(
    by.id('sequencing-panels-right-toggle-button'),
  );
  addEditorPanelButton = element(by.id('sequencing-add-editor-pane-button'));
  toggleEditorPanelsDirectionButton = element(
    by.id('sequencing-toggle-editor-pane-direction'),
  );
  leftPanel = element(by.id('left-panel-area'));
  middlePanel = element(by.id('middle-panel-area'));
  editorPanelSplit = element(by.id('sequencing-editor-panel-split'));
  rightPanel = element(by.id('right-panel-area'));
  editorPanels = element.all(by.className('editor-panel'));

  toolsButton = element(by.id('sequencing-editor-button'));
  autocompleteButton = element(
    by.id('sequencing-editor-toggle-autocomplete-button'),
  );
  colorschemeButton = element(
    by.id('sequencing-editor-toggle-color-scheme-button'),
  );
  tooltipsButton = element(by.id('sequencing-editor-toggle-tooltips-button'));
  helpButton = element(by.id('sequencing-editor-help-button'));

  firstCommand = element
    .all(
      by.css(
        '#sequencing-command-list > mat-accordion > cdk-virtual-scroll-viewport > div > mat-expansion-panel > mat-expansion-panel-header > span > mat-panel-title',
      ),
    )
    .first();
  firstCommandExpansion = element
    .all(
      by.css(
        '#sequencing-command-list > mat-accordion > cdk-virtual-scroll-viewport > div > mat-expansion-panel > div',
      ),
    )
    .first();

  addTabButton = element.all(by.id('seq-create-tab-button'));
  tabs = element.all(by.className('seq-tab'));
  tabTitles = element.all(by.css('.seq-tab-text'));
  tabCloseButtons = element.all(by.css('.seq-tab-icon'));

  editorToolsPanelButton = element(by.id('seq-editor-tools-button'));
  fullscreenButton = element(by.id('seq-editor-fullscreen-button'));

  parameterEditorTabButton = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/as-split/as-split-area[3]/mat-tab-group/mat-tab-header/div[2]/div/div/div[2]',
    ),
  );
  noActiveCommandPrompt = element(
    by.id('seq-parameter-form-no-active-command-prompt'),
  );
  noParameterPrompt = element(by.id('seq-parameter-form-no-parameters-prompt'));
  parameterFormContainer = element(by.id('seq-command-form-editor'));
  activeCommandName = element(by.id('seq-parameter-form-command-name'));
  updateParametersButton = element(by.id('seq-parameter-form-update-button'));
  parameterInputFields = element.all(by.className('mat-input-element'));

  selectTestCommandDictionary() {
    this.commandDictionarySelect.click();
    element(
      by.cssContainingText('mat-option .mat-option-text', 'Test 1'),
    ).click();
  }

  /**
   * Prepares the test for testing CodeMirror
   * 1. Refreshes the browser
   * 2. Loads the test command dictionary
   * 3. Creates a new tab
   */
  prepareForCodeMirrorTesting() {
    this.refreshBrowser();
    this.selectTestCommandDictionary();
    this.addTab();
  }

  addTab() {
    this.addTabButton.get(0).click();
  }

  sendKeysToCodeMirror(text: string) {
    click(this.codeMirrorWrapper);
    this.codeMirrorTextArea.sendKeys(text);
  }

  /**
   * Handles sending keys to the window rather than an element
   */
  sendGlobalKeys(keys: string) {
    browser
      .actions()
      .sendKeys(keys)
      .perform();
  }

  refreshBrowser() {
    browser.navigate().refresh();
  }

  navigateToParameterEditorPanel() {
    this.parameterEditorTabButton.click();
  }

  navigateTo(route: string) {
    return browser.get(`${browser.baseUrl}/#/${route}`) as Promise<any>;
  }
}
