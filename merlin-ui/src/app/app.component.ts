import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { MerlinActions } from './actions';
import { AppState } from './app-store';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: [`./app.component.css`],
})
export class AppComponent {
  constructor(private store: Store<AppState>) {}

  onAbout() {
    this.store.dispatch(MerlinActions.openAboutDialog());
  }
}
