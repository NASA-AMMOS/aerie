import { browser, ElementFinder, ExpectedConditions } from 'protractor';

export const testAdaptation = {
  name: 'test_banananation',
  version: 'test_1.0.0',
  mission: 'test_clipper',
  owner: 'test_ccamargo',
  filePath:
    '/Users/ccamargo/Projects/aerie/banananation/target/banananation-1.0-SNAPSHOT.jar',
};

export class AppPage {
  click(el: ElementFinder): void {
    const isClickable = ExpectedConditions.elementToBeClickable(el);
    browser.wait(isClickable, 5000);
    browser
      .actions()
      .click(el)
      .perform();
  }

  navigateTo(path: string): Promise<any> {
    return browser.get(`${browser.baseUrl}/#/${path}`) as Promise<any>;
  }

  waitTilClickable(el: ElementFinder, timeout: number = 5000): void {
    const isClickable = ExpectedConditions.elementToBeClickable(el);
    browser.wait(isClickable, timeout);
  }
}
