import { Segment } from './segment.js';
import {bound, Timeline} from './timeline.js';
import {Interval} from "./interval.js";
import {Temporal} from "@js-temporal/polyfill";
import Duration = Temporal.Duration;

class DatabaseFacade {
  private sharedClient: SharedClient;
  private resources: Map<string, Timeline<Segment<any>>>;

  constructor() {
    this.sharedClient = {
      client: 5,
      connected: false
    };

    this.resources = new Map();
  }

  public getResource(resourceId: string): Timeline<Segment<any>> {
    return bound([new Segment(1, Interval.At(Duration.from({minutes: 5})))]);
    // if (this.resources.has(resourceId)) {
    //   // @ts-ignore
    //   return this.resources.get(resourceId).clone();
    // } else {
    //   // pretend this is a real request
    //   const gen = cachePlainGenerator((async function* (sharedClient: SharedClient) {
    //     if (!sharedClient.connected) {
    //       await sharedClient.client.connect();
    //       const result = sharedClient.client.query(
    //           "SELECT 'Hello ' || $1 || '!' AS message",
    //           ['world']
    //       );
    //
    //       for await (const row of result) {
    //         // 'Hello world!'
    //         console.log(row.get('message'));
    //       }
    //     }
    //     yield new Segment(Interval.Forever, 2);
    //   })(this.sharedClient)) as CachedAsyncGenerator<Segment<any>>;
    //   this.resources.set(resourceId, gen);
    //   return gen;
    // }
  }
}

type SharedClient = {
  client: number;
  connected: boolean;
};

export default new DatabaseFacade();
