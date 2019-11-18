import { browser, WebElement } from 'protractor';

export class AppPage {
  navigateTo(path: string): Promise<any> {
    return browser.get(`${browser.baseUrl}/#/${path}`) as Promise<any>;
  }

  /**
   * @see https://github.com/angular/protractor/issues/5346
   * @see https://stackoverflow.com/questions/58092837/mousemove-not-working-in-firefox-giving-http-method-not-allowed
   */
  rightClick(el: WebElement) {
    const script = `
      const element = arguments[0];
      const { x, y } = element.getBoundingClientRect();
      const event = new MouseEvent('contextmenu', {
        clientX: x,
        clientY: y
      });
      element.dispatchEvent(event);
    `;
    browser.executeScript(script, el);
    browser.sleep(1000);
  }
}
