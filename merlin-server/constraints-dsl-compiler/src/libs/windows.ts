import {Profile, ProfileType} from "./Profile";
import type {Segment} from "./Segment";
import database from "./database";

export class Windows extends Profile<boolean> {
  constructor(segments: AsyncGenerator<Segment<boolean>>) {
    super(segments, ProfileType.Windows);
  }

  public static empty(): Windows {
    let segments = (async function* () {})();
    return (new Profile<boolean>(segments, ProfileType.Windows)).specialize();
  }

  public static override from(data: Segment<boolean>[]): Windows;
  public static override from(data: Segment<boolean>): Windows;
  public static override from(data: boolean): Windows;
  public static override from(data: any) {
    return Profile.from(data, ProfileType.Windows).specialize();
  }

  public static Value(value: boolean): Windows {
    return Windows.from(value);
  }

  public static Resource(name: string): Windows {
    return new Windows(database.getResource(name));
  }

  public not(): Windows {
   return this.map_values($ => !$.value);
  }
}
