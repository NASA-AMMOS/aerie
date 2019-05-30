import { browser, by, element } from 'protractor';
import { config } from '../../../src/config';

export class SequencingPage {
  routeTitle = element(by.css('.top-bar-title'));
  config = config.appModules[1];
  commandDictionarySelect = element(by.css('#sequencing-command-select'));
  testCommandDictionary = element(by.id('mat-option-1'));
  commandList = element(by.id('sequencing-command-list'));
  commands = element.all(by.css('.mat-list-text'));
  noCommandsPrompt = element(
    by.xpath('//*[@id="sequencing-no-commands-prompt"]'),
  );
  codeMirrorEditor = element(by.css('#sequencing-editor-mount > div'));
  codeMirrorTextArea = element(
    by.css('#sequencing-editor-mount > div > div > textarea'),
  );
  filename = element(by.id('sequencing-filename'));
  toolbar = element(by.id('sequencing-toolbar'));
  helpButton = element(by.id('seq-toolbar-help-button'));
  helpDialog = element(by.css('.mat-dialog-container'));
  helpDialogCloseButton = element(by.id('confirm-dialog-cancel-button'));
  fullscreenButton = element(by.id('seq-toolbar-fullscreen-button'));
  toggleThemeButton = element(by.id('seq-toolbar-theme-button'));
  autocompleteButton = element(by.id('seq-toolbar-autocomplete-button'));
  hintsContainer = element(by.css('.CodeMirror-hints'));
  panelButton = element(by.id('sequencing-panels-button'));
  panelMenu = element(by.css('.mat-menu-content'));
  leftPanelToggleButton = element(
    by.id('sequencing-panels-left-toggle-button'),
  );
  rightPanelToggleButton = element(
    by.id('sequencing-panels-right-toggle-button'),
  );
  leftPanel = element(by.id('left-panel-area'));
  middlePanel = element(by.id('middle-panel-area'));
  rightPanel = element(by.id('right-panel-area'));
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

  navigateTo(route: string) {
    return browser.get(`${browser.baseUrl}/#/${route}`) as Promise<any>;
  }
}
