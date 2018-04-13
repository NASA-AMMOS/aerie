import { Injectable } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { WebsocketService } from './websocket.service';

import { MpsServerMessage } from './../shared/models';

const URL = 'wss://localhost:8443/mpsserver/websocket/v1/topic/main';

@Injectable()
export class CollectionChangeService {
  // public messages: Subject<MpsServerMessage>;
  public messages: Subject<MessageEvent>;

  constructor(private webService: WebsocketService) {
    // this.messages = <Subject<MpsServerMessage>>this.webService
    this.messages = <Subject<MessageEvent>>this.webService
      .connect(URL);
      // .connect(URL)
      // .map((response: MessageEvent): MpsServerMessage => {
      //   const data = JSON.parse(response.data);
      //   return {
      //     aspect: data.aspect,
      //     detail: data.detail,
      //     status: data.status,
      //     subject: data.subject,
      //   };
      // });
  }
}
