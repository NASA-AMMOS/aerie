import { browser, by, element, logging, protractor } from 'protractor';
import { FalconPage } from './falcon.po';

describe('parameter-editor', () => {
  let page: FalconPage;

  beforeEach(() => {
    page = new FalconPage();
    page.navigateTo('falcon');
    // wait needed because CodeMirror mounting is stalling the test causing it to exit due to timeout
    browser.waitForAngularEnabled(false);
    page.prepareForCodeMirrorTesting();
    page.navigateToParameterEditorPanel();
  });

  it('[C144973] WHEN a user loads Falcon, the parameter editor should display "No Active Command"', () => {
    expect(page.noActiveCommandPrompt.isPresent()).toBe(true);
  });

  it('[C144974] WHEN a user has a command with parameter(s) in the editor THEN the parameter form SHOULD have the correct title and number of input fields ', () => {
    const testCommand = 'IAL_IMG_PRM_SE';
    page.sendKeysToCodeMirror(testCommand);
    page.sendKeysToCodeMirror(protractor.Key.ENTER);

    expect(page.activeCommandName.getText()).toBe(`${testCommand}T`);
    // IAL_IMG_PRM_SET has 4 parameters
    expect(page.parameterInputFields.count()).toBe(4);
  });

  it('[C144975] WHEN a user has a command with no parameters in the editor THEN the parameter form SHOULD have the correct title, no input fields, and the no parameter prompt', () => {
    const testCommand = 'EDL_MEDLI_ALPHA_PRM_SA';
    page.sendKeysToCodeMirror(testCommand);
    page.sendKeysToCodeMirror(protractor.Key.ENTER);

    expect(page.activeCommandName.getText()).toBe(`${testCommand}V`);
    // EDL_MEDLI_ALPHA_PRM_SAV has 0 parameters
    expect(page.parameterInputFields.count()).toBe(0);
    expect(page.noParameterPrompt.isPresent()).toBe(true);
  });

  it('[C144976] A user SHOULD be able to update the parameters in the editor by using the form', () => {
    const testCommand = 'IAL_IMG_PRM_SE';
    page.sendKeysToCodeMirror(testCommand);
    page.sendKeysToCodeMirror(protractor.Key.ENTER);

    page.parameterInputFields.get(0).sendKeys('BOBA');
    page.updateParametersButton.click();

    const bobaParameter = element(by.className('cm-variable'));

    expect(bobaParameter.getText()).toBe('BOBA');
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
