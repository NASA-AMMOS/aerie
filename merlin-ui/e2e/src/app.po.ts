import { browser } from 'protractor';

export class AppPage {
  navigateTo(path: string): Promise<any> {
    return browser.get(`${browser.baseUrl}/#/${path}`) as Promise<any>;
  }
}
