import { browser, by, element, logging, protractor } from 'protractor';
import { hasClass } from '../utils';
import { selectMpsDictionary } from './helpers';
import { SequencingPage } from './sequencing.po';

describe('/sequencing', () => {
  let page: SequencingPage;

  beforeEach(() => {
    page = new SequencingPage();
    page.navigateTo('sequencing');
    // wait needed because CodeMirror mounting is stalling the test causing it to exit due to timeout
    browser.waitForAngularEnabled(false);
  });

  it("[C134582] WHEN the sequencing page is loaded, the title should say 'Sequencing'", () => {
    expect(page.routeTitle.getText()).toBe('Sequencing');
  });

  it('[C134583] WHEN no command dictionary is loaded, the user should be prompted to select one', () => {
    expect(page.noCommandsPrompt.isDisplayed()).toBe(true);
  });

  it('[C134587] WHEN the user loads the page, they should see the editor', () => {
    expect(page.codeMirrorEditor.isDisplayed()).toBe(true);
  });

  it('[C125549] On Command Dictionary selection, a list of commands SHOULD be displayed to the user', () => {
    expect(page.noCommandsPrompt.isPresent()).toBe(true);
    expect(page.commandList.isPresent()).toBe(false);
    page.commandDictionarySelect.click();
    element(
      by.cssContainingText('mat-option .mat-option-text', 'Test 1'),
    ).click();
    expect(page.noCommandsPrompt.isPresent()).toBe(false);
    expect(page.commandList.isPresent()).toBe(true);
    expect(page.commands.count()).toBeGreaterThan(0);
  });

  it('[C134584] The toolbar should be visible', () => {
    expect(page.toolbar.isDisplayed()).toBe(true);
  });

  it('[C134585] The filename SHOULD be correct ', () => {
    expect(page.filename.getText()).toBe('FileName.mps');
  });

  it('[C136005] WHEN a user starts typing a valid command, suggestions should appear', () => {
    selectMpsDictionary(page);
    page.autocompleteButton.click();

    expect(hasClass(page.autocompleteButton, 'mat-primary')).toBe(true);
    expect(page.hintsContainer.isPresent()).toBe(false);

    page.codeMirrorTextArea.sendKeys('IAL_IMG_PRM_SE');
    expect(page.hintsContainer.isPresent()).toBe(true);
  });

  it('[C136006] WHEN a user types a valid stem and selects a suggestion then the editor SHOULD be filled with the command and the correct default parameters', () => {
    selectMpsDictionary(page);
    page.autocompleteButton.click();

    expect(hasClass(page.autocompleteButton, 'mat-primary')).toBe(true);
    expect(page.hintsContainer.isPresent()).toBe(false);

    page.codeMirrorTextArea.sendKeys('IAL_IMG_PRM_SE');
    expect(page.hintsContainer.isPresent()).toBe(true);

    page.codeMirrorTextArea.sendKeys(protractor.Key.ENTER);
    // One off selectors for this test
    const commandToken = element(
      by.xpath(
        '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/main/seq-editor/div[2]/div/div[6]/div[1]/div/div/div/div[5]/div/pre/span/span[1]',
      ),
    );
    const arg1Token = element(
      by.xpath(
        '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/main/seq-editor/div[2]/div/div[6]/div[1]/div/div/div/div[5]/div/pre/span/span[2]',
      ),
    );
    const arg2Token = element(
      by.xpath(
        '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/main/seq-editor/div[2]/div/div[6]/div[1]/div/div/div/div[5]/div/pre/span/span[3]',
      ),
    );
    const arg3Token = element(
      by.xpath(
        '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/main/seq-editor/div[2]/div/div[6]/div[1]/div/div/div/div[5]/div/pre/span/span[4]',
      ),
    );
    const arg4Token = element(
      by.xpath(
        '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/main/seq-editor/div[2]/div/div[6]/div[1]/div/div/div/div[5]/div/pre/span/span[5]',
      ),
    );

    expect(commandToken.getText()).toBe('IAL_IMG_PRM_SET');
    expect(arg1Token.getText()).toBe('"NAVF"');
    expect(arg2Token.getText()).toBe('1');
    expect(arg3Token.getText()).toBe('"GND"');
    expect(arg4Token.getText()).toBe('"GND"');
  });

  it("[C136008] WHEN a user types a valid stem and selects a suggestion that doesn't have parameters then the editor SHOULD be filled with just the command", () => {
    selectMpsDictionary(page);
    page.autocompleteButton.click();

    expect(hasClass(page.autocompleteButton, 'mat-primary')).toBe(true);
    expect(page.hintsContainer.isPresent()).toBe(false);

    page.codeMirrorTextArea.sendKeys('HGA_HIST_PRM_S');
    expect(page.hintsContainer.isPresent()).toBe(true);

    page.codeMirrorTextArea.sendKeys(protractor.Key.ENTER);

    // One off selectors for this test
    // Holds the individual tokens for a command "CMD 1 2 3" would be [CMD, 1, 2, 3]
    // We are testing the case where the command is "CMD", expecting [CMD]
    const commandTokens = element.all(
      by.css('.CodeMirror-line > span:nth-child(1) span'),
    );
    expect(commandTokens.count()).toBe(1);
  });

  it('[C134586] WHEN the user clicks the help button, a dialog pops up', () => {
    page.helpButton.click();
    expect(page.helpDialog.isDisplayed()).toBe(true);
  });

  it('[C134590] WHEN a user clicks the toggle theme button, the theme should switch to the alternative theme', () => {
    const darkTheme = 'cm-s-monokai';
    const lightTheme = 'cm-s-default';

    expect(hasClass(page.codeMirrorEditor, darkTheme)).toBe(true);
    expect(hasClass(page.codeMirrorEditor, lightTheme)).toBe(false);
    page.toggleThemeButton.click();
    expect(hasClass(page.codeMirrorEditor, darkTheme)).toBe(false);
    expect(hasClass(page.codeMirrorEditor, lightTheme)).toBe(true);
  });

  it('[C134589] WHEN the editor is fullscreen, the user can press ESC to exit fullscreen', () => {
    page.fullscreenButton.click();
    browser
      .actions()
      .sendKeys(protractor.Key.ESCAPE)
      .perform();
    expect(hasClass(page.codeMirrorEditor, 'CodeMirror-fullscreen')).toBe(
      false,
    );
  });

  it('[C134588] WHEN a user clicks the fullscreen button, the editor SHOULD be fullscreen', () => {
    expect(hasClass(page.codeMirrorEditor, 'CodeMirror-fullscreen')).toBe(
      false,
    );
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
