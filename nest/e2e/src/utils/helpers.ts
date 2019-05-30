import { browser, ElementFinder } from 'protractor';

export function click(el: ElementFinder): void {
  browser
    .actions()
    .click(el)
    .perform();
}

/**
 * Handles clicking on elements for the following cases:
 * 1. A Cdk element will receive click event
 * 2. Normal click doesn't actually click
 * 3. The click helper function doesn't actually click
 */
export function clickHarder(selector: string) {
  browser.executeScript(`
      const element = document.querySelector('${selector}');
      element.click();
    `);
}

export function hasClass(targetElement: any, className: string) {
  return targetElement.getAttribute('class').then((classes: string) => {
    return classes.split(' ').indexOf(className) !== -1;
  });
}
