import {Profile, ProfileType} from "./profile";
import {Segment} from "./segment";
import database from "./database";
import {BinaryOperation} from "./binary-operation";
import {Interval} from "./interval";
import type {Timeline} from "./timeline";

export class Windows extends Profile<boolean> {
  constructor(segments: Timeline<Segment<boolean>>) {
    super(segments, ProfileType.Windows);
  }

  public static empty(): Windows {
    return new Windows(_ => []);
  }

  public static Value(value: boolean): Windows {
    return new Windows(bounds => [new Segment(bounds, value)]);
  }

  public static Resource(name: string): Windows {
    return new Windows(database.getResource(name));
  }

  public not(): Windows {
   return this.mapValues($ => !$.value);
  }

  public and(other: Windows): Windows {
    return this.map2Values(other, BinaryOperation.cases(
        l => l ? undefined : false,
        r => r ? undefined : false,
        (l, r) => l && r
    ), ProfileType.Windows);
  }

  public or(other: Windows): Windows {
    return this.map2Values(other, BinaryOperation.cases(
        l => l ? true : undefined,
        r => r ? true : undefined,
        (l, r) => l || r
    ), ProfileType.Windows);
  }

  public add(other: Windows): Windows {
    return this.map2Values(other, BinaryOperation.combineOrIdentity(
        (l, r) => l || r
    ), ProfileType.Windows);
  }

  public filterByDuration(min?: Temporal.Duration, max?: Temporal.Duration): Windows {
    return this.mapValues(s => {
      if (s.value) {
        const duration = s.interval.duration();
        return !((min !== undefined && Temporal.Duration.compare(min, duration) > 0) || (max !== undefined && Temporal.Duration.compare(max, duration) < 0));
      } else {
        return false;
      }
    });
  }

  public shorterThan(max: Temporal.Duration): Windows {
    return this.filterByDuration(undefined, max);
  }

  public longerThan(min: Temporal.Duration): Windows {
    return this.filterByDuration(min, undefined);
  }

  public shiftBy(shiftRising: Temporal.Duration, shiftFalling?: Temporal.Duration): Windows {
    if (shiftFalling === undefined) shiftFalling = shiftRising;
    let boundsMap = (bounds: Interval) => {
      let start: Temporal.Duration;
      let end: Temporal.Duration;
      if (Temporal.Duration.compare(shiftRising, shiftFalling!) === 1) {
        start = bounds.start.subtract(shiftRising);
        end = bounds.end.subtract(shiftFalling!);
      } else {
        start = bounds.start.subtract(shiftFalling!);
        end = bounds.end.subtract(shiftRising);
      }
      return Interval.between(
          start,
          end,
          bounds.startInclusivity,
          bounds.endInclusivity
      );
    }
    return this.unsafe.mapIntervals($ => {
      if ($.value) {
        return Interval.between(
          $.interval.start.add(shiftRising),
          $.interval.end.add(shiftFalling!),
          $.interval.startInclusivity,
          $.interval.endInclusivity
        );
      } else {
        return Interval.between(
          $.interval.start.add(shiftFalling!),
          $.interval.end.add(shiftRising),
          $.interval.startInclusivity,
          $.interval.endInclusivity
        );
      }
    }, boundsMap);
  }

  public starts = () => this.specificEdges(false, true);
  public ends = () => this.specificEdges(true, false);
}
