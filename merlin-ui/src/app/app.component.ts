import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
} from '@angular/core';
import { select, Store } from '@ngrx/store';
import { SubSink } from 'subsink';
import { MerlinActions } from './actions';
import { AppState } from './app-store';
import { getLoading } from './selectors';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  styleUrls: [`./app.component.css`],
  templateUrl: './app.component.html',
})
export class AppComponent implements OnDestroy {
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

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  onAbout(): void {
    this.store.dispatch(MerlinActions.openAboutDialog());
  }
}
