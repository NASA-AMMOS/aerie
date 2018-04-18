import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';

import { Subject } from 'rxjs/Subject';

@Injectable()
export class WebsocketService {

  private subject: Subject<MessageEvent>;

  public connect(url: string): Subject<MessageEvent> {
    if (!this.subject) {
      this.subject = this.create(url);
      console.log('Successfully connected: ' + url);
    }
    return this.subject;
  }

  public create(url: string): Subject<MessageEvent> {
    // const webSocket = Observable.webSocket;
    const ws = new WebSocket(url);
    const observable = Observable.create(
      (obs: Observer<MessageEvent>) => {
        ws.onmessage = obs.next.bind(obs);
        ws.onerror = obs.error.bind(obs);
        const self = this;
        ws.onclose = function() {
          // try to reconnect
          setTimeout (self.create(url), 5000);
        };
        return ws.close.bind(ws);
      });
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
