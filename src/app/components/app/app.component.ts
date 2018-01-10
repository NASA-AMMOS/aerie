import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';

import * as fromLayout from '../../reducers/layout';
import * as layout from '../../actions/layout';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  styleUrls: ['./app.component.css'],
  templateUrl: './app.component.html',
})
export class AppComponent {
  showLeftDrawer$: Observable<boolean>;

  /**
   * Default Constructor.
   */
  constructor(private store: Store<fromLayout.LayoutState>) {
    this.showLeftDrawer$ = this.store.select(fromLayout.getShowLeftDrawer);
  }

  toggleLeftDrawer() {
    this.store.dispatch(new layout.ToggleLeftDrawer());
  }
}
