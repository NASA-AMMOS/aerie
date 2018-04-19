/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';

import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs/Subject';

import { Store } from '@ngrx/store';

import { AppState } from './../../app/store';
import * as fromConfig from './../reducers/config';

/**
 * Helper service for creating and subscribing to a web-socket.
 * The websocket URL should look like this: `wss://localhost:8443/mpsserver/websocket/v1/topic/main`.
 */

@Injectable()
export class WebsocketService {
  public connection: Subject<MessageEvent>;
  private ngUnsubscribe: Subject<{}> = new Subject();

  /**
   * Default constructor.
   * Subscribes to the store to get config URLs and connects to the websocket.
   */
  constructor(private store$: Store<AppState>) {
    this.store$.select(fromConfig.getConfigState).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(config => {
      this.connection = <Subject<MessageEvent>>this.connect(`${config.baseUrl.replace('https', 'wss')}/${config.baseSocketUrl}`);
    });
  }

  /**
   * Connect to a websocket.
   */
  public connect(url: string): Subject<MessageEvent> {
    if (!this.connection) {
      this.connection = this.create(url);
    }
    return this.connection;
  }

  /**
   * Create a websocket and wrap it in a Subject.
   */
  public create(url: string): Subject<MessageEvent> {
    const ws = new WebSocket(url);

    const observable = Observable.create(
      (obs: Observer<MessageEvent>) => {
        ws.onmessage = obs.next.bind(obs);
        ws.onerror = obs.error.bind(obs);
        const self = this;
        ws.onclose = function() {
          setTimeout(self.create(url), 5000);
        };
        return ws.close.bind(ws);
      },
    );

    const observer = {
      next: (data: Object) => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify(data));
        }
      },
    };

    return Subject.create(observer, observable);
  }
}
