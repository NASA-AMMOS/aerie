import { Injectable } from '@angular/core';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs/Subject';
import { WebsocketService } from './websocket.service';

import { Store } from '@ngrx/store';
import { AppState } from './../../app/store';

import * as fromConfig from './../reducers/config';

@Injectable()
export class CollectionChangeService {
  private ngUnsubscribe: Subject<{}> = new Subject();
  public messages: Subject<MessageEvent>;

  constructor(private store$: Store<AppState>, private webService: WebsocketService) {
    // Config state.
    this.store$.select(fromConfig.getConfigState).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(state => {
      const baseUrl = state.baseUrl;

      // websocket url should look like this: 'wss://localhost:8443/mpsserver/websocket/v1/topic/main'
      this.messages = <Subject<MessageEvent>>this.webService
        .connect(`${baseUrl.replace('https', 'wss')}/mpsserver/websocket/v1/topic/main`);
    });
  }

}
