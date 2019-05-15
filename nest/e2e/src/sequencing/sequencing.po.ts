import { browser, by, element } from 'protractor';
import { config } from '../../../src/config';

export class SequencingPage {
  routeTitle = element(by.css('.top-bar-title'));
  config = config.appModules[1];
  commandDictionarySelect = element(by.css('#sequencing-command-select'));
  testCommandDictionary = element(by.xpath('//*[@id="mat-option-1"]'));
  commandList = element(by.xpath('//*[@id="sequencing-command-list"]'));
  commands = element.all(by.css('.mat-list-text'));
  noCommandsPrompt = element(
    by.xpath('//*[@id="sequencing-no-commands-prompt"]'),
  );
  codeMirrorEditor = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/as-split/as-split-area[2]/main/seq-editor/div[2]/div',
    ),
  );
  codeMirrorTextArea = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/as-split/as-split-area[2]/main/seq-editor/div[2]/div/div[1]/textarea',
    ),
  );
  filename = element(by.xpath('//*[@id="sequencing-filename"]'));
  toolbar = element(by.xpath('//*[@id="sequencing-toolbar"]'));
  helpButton = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/as-split/as-split-area[2]/main/seq-editor/div[1]/button[6]',
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
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/as-split/as-split-area[2]/main/seq-editor/div[1]/button[5]',
    ),
  );
  toggleThemeButton = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/as-split/as-split-area[2]/main/seq-editor/div[1]/button[4]',
    ),
  );
  autocompleteButton = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/as-split/as-split-area[2]/main/seq-editor/div[1]/button[3]',
    ),
  );
  hintsContainer = element(by.css('.CodeMirror-hints'));
  panelButton = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/nest-app-header/mat-toolbar/div/div[2]/button',
    ),
  );
  panelMenu = element(by.xpath('/html/body/div[3]/div[2]/div/div'));
  leftPanelToggleButton = element(
    by.xpath('/html/body/div[3]/div[2]/div/div/div/button[1]'),
  );
  rightPanelToggleButton = element(
    by.xpath('/html/body/div[3]/div[2]/div/div/div/button[2]'),
  );
  leftPanel = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/as-split/as-split-area[1]',
    ),
  );
  middlePanel = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/as-split/as-split-area[2]',
    ),
  );
  rightPanel = element(
    by.xpath(
      '/html/body/app-root/div/mat-sidenav-container/mat-sidenav-content/main/sequencing-app/as-split/as-split-area[3]',
    ),
  );

  navigateTo(route: string) {
    return browser.get(`${browser.baseUrl}/#/${route}`) as Promise<any>;
  }
}
