import {Segment} from "./segment";
import {Client} from "ts-postgres";
import {Timeline} from "./timeline";

class DatabaseFacade {
  private sharedClient: SharedClient;
  private resources: Map<string, Timeline<Segment<any>>>

  constructor() {
    this.sharedClient = {
      client: new Client(),
      connected: false
    };

    this.resources = new Map();
  }

  public getResource(resourceId: string): Timeline<Segment<any>> {
    throw new Error("unimplemented");
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
  client: Client,
  connected: boolean
};

export default new DatabaseFacade();
