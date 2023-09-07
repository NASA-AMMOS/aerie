import {Segment} from './segment.js';
import {coalesce, Timeline, truncate} from './timeline.js';
import pg from 'pg';
import {Temporal} from "@js-temporal/polyfill";
import {Inclusivity, Interval} from "./interval.js";
import Duration = Temporal.Duration;
import {ProfileType} from "./profiles/profile-type.js";

export interface DataFetcher {
  resource<V>(name: string, valueMap: (v: any, t: Duration) => V, profileType: ProfileType): Timeline<Segment<V>>;
}

class UnimplementedDataFetcherStub implements DataFetcher {
  public resource<V>(name: string): Timeline<Segment<V>> {
    throw new Error("`fetcher` global variable has not been set.");
  }
}

export class AeriePostgresDataFetcher implements DataFetcher {
  private client: pg.Client;
  private resourceCache: Map<String, Promise<Segment<any>[]>> = new Map();
  private datasetId: number | undefined = undefined;

  constructor(
      clientOptions: {
        host: string,
        port: number,
        user: string,
        password: string,
        database: string
      },
      private planId: number,
      private simulationDatasetId: number
  ) {
    this.client = new pg.Client(clientOptions);
  }

  public clearCaches(): void {
    this.resourceCache = new Map();
  }

  public resource<V>(name: string, valueMap: (v: any, t: Duration) => V, profileType: ProfileType): Timeline<Segment<V>> {
    const cached = this.resourceCache.get(name);
    if (cached !== undefined) return async bounds => truncate(cached as unknown as Segment<V>[], bounds);
    const segmentsPromise = (async () => {
      const datasetId = await this.getDatasetId();
      const {id: profileId, duration} = await this.getProfileInfo(name);
      const segmentsResult = await this.client.query(
          'select start_offset, dynamics, is_gap from profile_segment where profile_id = $1 and dataset_id = $2;',
          [profileId, datasetId]
      );
      const result: Segment<V>[] = [];
      let previousValue: V | undefined = undefined;
      let previousStart: Duration | undefined = undefined;
      for (const segment of segmentsResult.rows) {
        const thisStart = Duration.from(segment.start_offset.toISOString());
        if (previousStart !== undefined) {
          const newSegment = new Segment(
              previousValue!,
              Interval.Between(previousStart, thisStart, Inclusivity.Inclusive, Inclusivity.Exclusive)
          );
          result.push(newSegment);
        }
        if (segment.is_gap === false) {
          previousValue = valueMap(segment.dynamics as any, thisStart);
          previousStart = thisStart;
        } else {
          previousValue = undefined;
          previousStart = undefined;
        }
      }
      if (previousStart !== undefined) {
        result.push(new Segment(
            previousValue!,
            Interval.Between(previousStart, duration, Inclusivity.Inclusive, Inclusivity.Exclusive)
        ));
      }
      coalesce(result, profileType);
      return result;
    })();
    this.resourceCache.set(name, segmentsPromise);
    return async bounds => truncate(await segmentsPromise, bounds);
  }

  private async getProfileInfo(name: string): Promise<{id: number, duration: Duration}> {
    const profileResult = await this.client.query(
        'select id, duration from profile where dataset_id = $1 and name = $2;',
        [this.datasetId!, name]
    );
    if (profileResult.rowCount === 0) throw new Error(`Profile ${name} not found in database`);
    else if (profileResult.rowCount > 1) throw new Error(`Multiple profiles named ${name} found in one simulation dataset`);
    const id = profileResult.rows[0].id as number;
    const duration = Duration.from(profileResult.rows[0].duration.toISOString());
    return {id, duration};
  }

  private async getDatasetId(): Promise<number> {
    if (this.datasetId !== undefined) return this.datasetId;
    const datasetResult = await this.client.query(
        'select dataset_id from simulation_dataset where id = $1;',
        [this.simulationDatasetId]
    );
    if (datasetResult.rowCount === 0) throw new Error(`Simulation dataset ${this.simulationDatasetId} not found in database`);
    else if (datasetResult.rowCount > 1) throw new Error(`Multiple datasets with id ${this.simulationDatasetId} found in database`);
    this.datasetId = datasetResult.rows[0].dataset_id as number;
    return this.datasetId;
  }

  public async connect(): Promise<void> {
    await this.client.connect();
  }

  public async disconnect(): Promise<void> {
    await this.client.end();
  }
}

export let fetcher: DataFetcher = new UnimplementedDataFetcherStub();

export function setFetcher(f: DataFetcher) {
  fetcher = f;
}
