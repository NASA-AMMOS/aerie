import {Profile, ProfileType} from "./Profile";
import type {Segment} from "./Segment";
import database from "./database";

export class Real extends Profile<LinearEquation> {
  constructor(segments: AsyncGenerator<Segment<LinearEquation>>) {
    super(segments, ProfileType.Real);
  }

  public static empty(): Real {
    let segments = (async function* () {})();
    return (new Profile<LinearEquation>(segments, ProfileType.Real)).specialize();
  }

  public static override from(data: Segment<LinearEquation>[]): Real;
  public static override from(data: Segment<LinearEquation>): Real;
  public static override from(data: number): Real;
  public static override from(data: any) {
    if (typeof data === "number") {
      data = {
        initialTime: 0,
        initialValue: data,
        rate: 0
      };
    }
    return Profile.from(data, ProfileType.Real).specialize();
  }

  public static Value(value: number): Real {
    return Real.from(value);
  }

  public static Resource(name: string): Real {
    return new Real(database.getResource(name));
  }
}

export interface LinearEquation {
  initialTime: number;
  initialValue: number;
  rate: number;
}
