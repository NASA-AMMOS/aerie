import { AppPage } from './app.po';
import { Elements } from './elements';
import { adaptation } from './mocks';

/**
 * @note These tests assume a clean/empty database.
 * @note Only run these tests against Firefox for now,
 * there is a possible Chromium bug that will break some of the tests: https://github.com/angular/components/issues/10140
 */
describe('merlin-ui App', () => {
  let page: AppPage;

  beforeEach(() => {
    page = new AppPage();
  });

  describe('plans page with no adaptations', () => {
    it(`should navigate to the plans page`, () => {
      page.navigateTo('plans');
    });

    it(`the no adaptations message and no plans message should be present, and the plans table should NOT be present`, () => {
      expect(Elements.noAdaptationsMessage.isPresent()).toBe(true);
      expect(Elements.noPlansMessage.isPresent()).toBe(true);
      expect(Elements.plansTable.isPresent()).toBe(false);
    });
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
      Elements.createAdaptationFormName.sendKeys(adaptation.name);
      Elements.createAdaptationFormVersion.sendKeys(adaptation.version);
      Elements.createAdaptationFormMission.sendKeys(adaptation.mission);
      Elements.createAdaptationFormOwner.sendKeys(adaptation.owner);
      Elements.createAdaptationFormFile.sendKeys(adaptation.filePath);
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

    it(`after clicking 'No' on the confirm dialog, the adaptation table should still be present`, () => {
      Elements.confirmDialogCancelButton.click();
      expect(Elements.adaptationsTable.isPresent()).toBe(true);
    });

    it(`after clicking 'Yes' on the confirm dialog, the no adaptations message should be present`, () => {
      Elements.adaptationMenu.click();
      Elements.adaptationMenuDelete.click();
      Elements.confirmDialogConfirmButton.click();
      expect(Elements.noAdaptationsMessage.isPresent()).toBe(true);
    });
  });

  describe('plans page with adaptations', () => {
    it(`should navigate to the plans page`, () => {
      page.navigateTo('plans');
    });
  });
});
