import { by, element } from 'protractor';
import { SequencingPage } from '../sequencing.po';

export function selectMpsDictionary(page: SequencingPage) {
  page.commandDictionarySelect.click();
  element(
    by.cssContainingText('mat-option .mat-option-text', 'Test 1'),
  ).click();
}
