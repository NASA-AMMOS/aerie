import { Injectable } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { WebsocketService } from './websocket.service';

const URL = 'wss://localhost:8443/mpsserver/websocket/v1/topic/main';

@Injectable()
export class CollectionChangeService {
  public messages: Subject<MessageEvent>;

  constructor(private webService: WebsocketService) {
    this.messages = <Subject<MessageEvent>>this.webService
      .connect(URL);
  }
}
