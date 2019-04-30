import { browser, by, element } from 'protractor';

export class PlanningPage {
  routeTitle = element(by.css('.top-bar-title'));
  formTitle = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/plans-app/main/mat-sidenav-container/mat-sidenav/div/div/div/div[1]/h2',
    ),
  );
  newPlanButton = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/plans-app/nest-app-header/mat-toolbar/div/div[2]/button',
    ),
  );
  planTableRows = element.all(by.css('.mat-row'));
  nameInput = element(by.xpath('//*[@id="plan-name"]'));
  planStartInput = element(by.xpath('//*[@id="plan-start"]'));
  planEndInput = element(by.xpath('//*[@id="plan-end"]'));
  adaptationInput = element(by.xpath('//*[@id="plan-adaptation"]'));
  adaptationOptions = element.all(by.tagName('option'));
  cancelButton = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/plans-app/main/mat-sidenav-container/mat-sidenav/div/div/div/div[1]/button',
    ),
  );
  saveButton = element(by.xpath('//*[@id="plan-save-button"]'));
  testPlanStart = element(
    by.xpath(
      '/html/body/div[2]/div[2]/div/owl-date-time-container/div[2]/owl-date-time-calendar/div[2]/owl-date-time-month-view/table/tbody/tr[4]/td[5]',
    ),
  );
  testPlanEnd = element(
    by.xpath(
      '/html/body/div[2]/div[2]/div/owl-date-time-container/div[2]/owl-date-time-calendar/div[2]/owl-date-time-month-view/table/tbody/tr[5]/td[3]',
    ),
  );
  datetimeContainer = element(by.css('.owl-dt-container'));
  datetimeSetButton = element(
    by.xpath(
      '/html/body/div[2]/div[2]/div/owl-date-time-container/div[2]/div/button[2]',
    ),
  );
  toastSuccess = element(by.css('.toast-success'));

  navigateTo(route: string) {
    return browser.get(`${browser.baseUrl}/#/${route}`) as Promise<any>;
  }
}
