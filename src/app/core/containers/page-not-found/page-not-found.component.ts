import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-page-not-found',
  styles: [],
  template: `
    Page Not Found :(
  `
})
export class PageNotFoundComponent {}
