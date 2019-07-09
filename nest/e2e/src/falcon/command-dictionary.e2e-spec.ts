import { browser, ExpectedConditions as EC, logging } from 'protractor';
import { FalconPage } from './falcon.po';

describe('command-dictionary', () => {
  let page: FalconPage;

  beforeEach(() => {
    page = new FalconPage();
    page.navigateTo('falcon');
    // wait needed because CodeMirror mounting is stalling the test causing it to exit due to timeout
    browser.waitForAngularEnabled(false);
    page.refreshBrowser();
  });

  it('[C134977] On Command Dictionary selection, a list of commands SHOULD be displayed to the user', () => {
    expect(page.noCommandsPrompt.isPresent()).toBe(true);
    expect(page.commandList.isPresent()).toBe(false);

    page.selectTestCommandDictionary();
    expect(page.noCommandsPrompt.isPresent()).toBe(false);
    expect(page.commandList.isPresent()).toBe(true);
    expect(page.commands.count()).toBeGreaterThan(0);
  });

  it('[C137585] WHEN a user clicks on a command, an expansion panel SHOULD open up with more information', async () => {
    page.selectTestCommandDictionary();

    expect(page.firstCommand.isDisplayed()).toBe(true);
    expect(page.firstCommandExpansion.getCssValue('height')).toBe('0px');
    expect(page.firstCommandExpansion.getCssValue('visibility')).toBe('hidden');

    page.firstCommand.click();
    browser.wait(EC.visibilityOf(page.firstCommandExpansion), page.waitTimeout);

    const heightPx = await page.firstCommandExpansion.getCssValue('height');
    const heightNum = Number(heightPx.split('px')[0]);
    expect(heightNum).toBeGreaterThan(0);

    expect(page.firstCommandExpansion.getCssValue('visibility')).toBe(
      'visible',
    );
  });

  it('[C137586] WHEN a user clicks on a command to expand it, they SHOULD be able to click it again to collapse it', async () => {
    page.selectTestCommandDictionary();

    expect(page.firstCommand.isDisplayed()).toBe(true);
    expect(page.firstCommandExpansion.getCssValue('height')).toBe('0px');
    expect(page.firstCommandExpansion.getCssValue('visibility')).toBe('hidden');

    page.firstCommand.click();
    browser.wait(EC.visibilityOf(page.firstCommandExpansion), page.waitTimeout);

    const heightPx = await page.firstCommandExpansion.getCssValue('height');
    const heightNum = Number(heightPx.split('px')[0]);
    expect(heightNum).toBeGreaterThan(0);

    expect(page.firstCommandExpansion.getCssValue('visibility')).toBe(
      'visible',
    );

    page.firstCommand.click();
    browser.wait(
      EC.invisibilityOf(page.firstCommandExpansion),
      page.waitTimeout,
    );
    expect(page.firstCommandExpansion.getCssValue('visibility')).toBe('hidden');
  });

  it('[C139219] The create tab button SHOULD be visible after selecting a command dictionary', () => {
    page.selectTestCommandDictionary();
    expect(page.addTabButton.get(0).isPresent()).toBe(true);
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
