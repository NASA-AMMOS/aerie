import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-page-not-found',
  styles: [],
  template: `
    <page-not-found></page-not-found>
  `
})
export class PageNotFoundComponent {}
