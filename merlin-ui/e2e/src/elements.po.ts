import { by, element } from 'protractor';
import { testAdaptation } from './app.po';

export class Elements {
  static get adaptationMenu() {
    return element(by.id(`adaptation-menu-${testAdaptation.name}`));
  }
  static get adaptationMenuDelete() {
    return element(by.id(`adaptation-menu-delete-${testAdaptation.name}`));
  }
  static get adaptationsTable() {
    return element(by.id('adaptations-table'));
  }
  static get confirmDialogCancelButton() {
    return element(by.id('confirm-dialog-cancel-button'));
  }
  static get confirmDialogConfirmButton() {
    return element(by.id('confirm-dialog-confirm-button'));
  }
  static get createAdaptationFormCreateButton() {
    return element(by.id('create-adaptation-form-create-button'));
  }
  static get createAdaptationFormFile() {
    return element(by.id('create-adaptation-form-file'));
  }
  static get createAdaptationFormMission() {
    return element(by.id('create-adaptation-form-mission'));
  }
  static get createAdaptationFormName() {
    return element(by.id('create-adaptation-form-name'));
  }
  static get createAdaptationFormOwner() {
    return element(by.id('create-adaptation-form-owner'));
  }
  static get createAdaptationFormVersion() {
    return element(by.id('create-adaptation-form-version'));
  }
  static get noAdaptationsMessage() {
    return element(by.id('no-adaptations-message'));
  }
}
