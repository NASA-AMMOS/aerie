import { browser, ElementFinder } from 'protractor';

export function click(el: ElementFinder): void {
  browser
    .actions()
    .click(el)
    .perform();
}

export function hasClass(targetElement: any, className: string) {
  return targetElement.getAttribute('class').then((classes: string) => {
    return classes.split(' ').indexOf(className) !== -1;
  });
}
