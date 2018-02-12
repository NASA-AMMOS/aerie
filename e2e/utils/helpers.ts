/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  browser,
  by,
  element,
  ElementFinder,
} from 'protractor';

/**
 * Click via css class.
 */
export function clickByCss(cssClass: string): void {
  element(by.css(cssClass)).click();
}

/**
 * Click via id.
 */
export function clickById(id: string): void {
  element(by.id(id)).click();
}

/**
 * Clicks all ids given in the 'ids' parameter.
 */
export function clickByIds(ids: string[]): void {
  ids.forEach(id => {
    element(by.id(id)).click();
  });
}

/**
 * Gets an Angular component instance property using ng.probe().
 *
 * Note: We have to get a specific prop here and not the entire component instance because
 * of a known "max callback stack size exceeded" error when sending big objects between selenium and the browser.
 */
export function probe(elem: ElementFinder, prop: string): any {
  return browser.executeScript('return ng.probe(arguments[0]).componentInstance[arguments[1]]', elem, prop);
}
