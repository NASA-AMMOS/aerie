import { browser, by, element } from 'protractor';

export class SequencingPage {
  routeTitle = element(by.css('.top-bar-title'));
  commandDictionarySelect = element(by.css('#sequencing-command-select'));
  testCommandDictionary = element(by.xpath('//*[@id="mat-option-1"]'));
  commandList = element(by.xpath('//*[@id="sequencing-command-list"]'));
  commands = element.all(by.css('.mat-list-text'));
  noCommandsPrompt = element(
    by.xpath('//*[@id="sequencing-no-commands-prompt"]'),
  );
  codeMirrorEditor = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/main/seq-editor/div[2]/div',
    ),
  );
  filename = element(by.xpath('//*[@id="sequencing-filename"]'));
  toolbar = element(by.xpath('//*[@id="sequencing-toolbar"]'));
  helpButton = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/main/seq-editor/div[1]/button[6]',
    ),
  );
  helpDialog = element(by.css('.mat-dialog-container'));
  helpDialogCloseButton = element(
    by.xpath(
      '/html/body/div[3]/div[2]/div/mat-dialog-container/nest-confirm-dialog/div[2]/div/button',
    ),
  );
  fullscreenButton = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/main/seq-editor/div[1]/button[5]',
    ),
  );
  toggleThemeButton = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/main/seq-editor/div[1]/button[4]',
    ),
  );

  navigateTo(route: string) {
    return browser.get(`${browser.baseUrl}/#/${route}`) as Promise<any>;
  }
}
