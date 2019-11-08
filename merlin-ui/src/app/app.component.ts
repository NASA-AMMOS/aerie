import { ChangeDetectorRef, Component } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { SubSink } from 'subsink';
import { MerlinActions } from './actions';
import { AppState } from './app-store';
import { getLoading } from './selectors';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: [`./app.component.css`],
})
export class AppComponent {
  loading = false;
  private subs = new SubSink();

  constructor(private ref: ChangeDetectorRef, private store: Store<AppState>) {
    this.subs.add(
      this.store.pipe(select(getLoading)).subscribe(loading => {
        this.loading = loading;
        this.ref.markForCheck();
      }),
    );
  }

  onAbout() {
    this.store.dispatch(MerlinActions.openAboutDialog());
  }
}
