import { AppPage } from './app.po';

describe('raven2 App', () => {
  let page: AppPage;

  beforeEach(() => {
    page = new AppPage();
  });

  it('the app title should be correct', () => {
    page.navigateTo();
    expect(page.getAppTitle()).toEqual('Raven2');
  });
});
