import { browser, ExpectedConditions as EC, logging } from 'protractor';
import { PlanningPage } from './planning.po';

describe('/plans', () => {
  let page: PlanningPage;

  beforeEach(() => {
    page = new PlanningPage();
    page.navigateTo('plans');
  });

  it('[C125558] WHEN the Plans View is Loaded THEN it SHOULD have Plan Catalog as title', () => {
    expect(page.routeTitle.getText()).toBe('Plan Catalog');
  });

  it('[C125559] WHEN the Plans View is Loaded THEN it SHOULD have a create plan button in the header', () => {
    expect(page.newPlanButton.isDisplayed()).toBe(true);
  });

  it('[C125565] WHEN the Create Plan form is shown THEN the form SHOULD require Name, Plan Start and End and Adaptation fields', () => {
    page.newPlanButton.click();

    expect(page.formTitle.getText()).toBe('Create Plan');
    expect(page.nameInput.isDisplayed()).toBe(true);
    expect(page.planStartInput.isDisplayed()).toBe(true);
    expect(page.planEndInput.isDisplayed()).toBe(true);
    expect(page.adaptationInput.isDisplayed()).toBe(true);
  });

  it('[C125576] WHEN the Create Plan form is shown THEN the form SHOULD NOT allow the user to click Save when the required fields are not filled', () => {
    page.newPlanButton.click();

    expect(page.saveButton.isEnabled()).toBe(false);
  });

  it('[C125578] WHEN the Create Plan form is shown AND the user presses the Cancel Button, THEN a plan SHOULD NOT be added to the plans list', () => {
    const numRowsStart = page.planTableRows.count();
    page.newPlanButton.click();
    page.cancelButton.click();
    const numRowsEnd = page.planTableRows.count();

    expect(numRowsEnd).toBe(numRowsStart);
  });

  it('[C125638] WHEN the Create Plan form is submitted THEN a toast SHOULD appear confirming the creation of the plan', () => {
    page.newPlanButton.click();

    page.nameInput.sendKeys('Test Plan 1002');

    page.planStartInput.click();
    page.testPlanStart.click();
    browser.sleep(500);
    page.datetimeSetButton.click();

    browser.wait(
      EC.not(EC.visibilityOf(page.datetimeContainer)),
      1000,
      'Datetime picker did not go away',
    );

    page.planEndInput.click();
    browser.sleep(500);
    page.testPlanEnd.click();
    page.datetimeSetButton.click();

    browser.wait(
      EC.not(EC.visibilityOf(page.datetimeContainer)),
      1000,
      'Datetime picker did not go away',
    );

    page.adaptationOptions.first().click();

    page.saveButton.click();

    expect(page.toastSuccess.isDisplayed()).toBeTruthy();
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
