import { browser, logging } from 'protractor';
import { AppPage, testAdaptation } from './app.po';
import { Elements } from './elements.po';

/**
 * @note These tests assume a clean/empty database.
 */
describe('merlin-ui App', () => {
  let page: AppPage;

  beforeEach(() => {
    page = new AppPage();
  });

  describe('adaptations page', () => {
    it(`should navigate to the adaptations page`, () => {
      page.navigateTo('adaptations');
    });

    it(`the no adaptations message should be present, and the adaptation table should NOT be present`, () => {
      expect(Elements.noAdaptationsMessage.isPresent()).toBe(true);
      expect(Elements.adaptationsTable.isPresent()).toBe(false);
    });

    it(`create adaptation form 'Create' button should be disabled`, () => {
      expect(Elements.createAdaptationFormCreateButton.isEnabled()).toBe(false);
    });

    it(`filling all the create adaptation form fields should enable the 'Create' button`, () => {
      Elements.createAdaptationFormName.sendKeys(testAdaptation.name);
      Elements.createAdaptationFormVersion.sendKeys(testAdaptation.version);
      Elements.createAdaptationFormMission.sendKeys(testAdaptation.mission);
      Elements.createAdaptationFormOwner.sendKeys(testAdaptation.owner);
      Elements.createAdaptationFormFile.sendKeys(testAdaptation.filePath);
      expect(Elements.createAdaptationFormCreateButton.isEnabled()).toBe(true);
    });

    it(`should click the 'Create' button on the create adaptation form`, () => {
      Elements.createAdaptationFormCreateButton.click();
    });

    it('the no adaptations message should NOT be present, and the adaptation table should be present', () => {
      expect(Elements.noAdaptationsMessage.isPresent()).toBe(false);
      expect(Elements.adaptationsTable.isPresent()).toBe(true);
    });

    it('trying to delete an adaptation should show a confirm dialog', () => {
      Elements.adaptationMenu.click();
      Elements.adaptationMenuDelete.click();
      expect(Elements.confirmDialogConfirmButton.isPresent()).toBe(true);
      expect(Elements.confirmDialogCancelButton.isPresent()).toBe(true);
    });

    it(`after clicking 'No' on the confirm dialog the adaptation table should still be present`, () => {
      Elements.confirmDialogCancelButton.click();
      expect(Elements.adaptationsTable.isPresent()).toBe(true);
    });

    it(`after clicking 'Yes' on the confirm dialog the no adaptations message should be present`, () => {
      Elements.adaptationMenu.click();
      browser.sleep(1000); // Make sure there are no overlays (snack-bar or dialogs).
      Elements.adaptationMenuDelete.click();
      Elements.confirmDialogConfirmButton.click();
      expect(Elements.noAdaptationsMessage.isPresent()).toBe(true);
    });
  });

  describe('plans page', () => {
    it(`should navigate to the plans page`, () => {
      page.navigateTo('plans');
    });
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
