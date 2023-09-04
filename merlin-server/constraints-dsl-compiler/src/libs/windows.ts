import {Profile} from "./profile";
import {Segment} from "./segment";
import database from "./database";
import {BinaryOperation} from "./binary-operation";
import {Interval} from "./interval";
import type {Timeline} from "./timeline";
import {ProfileType} from "./profile-type";

export class Windows extends Profile<boolean> {
  constructor(segments: Timeline<Segment<boolean>>) {
    super(segments, ProfileType.Windows);
  }

  public static empty(): Windows {
    return new Windows(_ => []);
  }

  public static override Value(value: boolean, interval?: Interval): Windows {
    return new Windows(bounds => [new Segment(
        value,
        interval === undefined ? bounds : Interval.intersect(bounds, interval)
    )]);
  }

  public static override Resource(name: string): Windows {
    return new Windows(database.getResource(name));
  }

  public not(): Windows {
   return this.mapValues($ => !$);
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

  public falsifyByDuration(min?: Temporal.Duration, max?: Temporal.Duration): Windows {
    return this.mapValues((v, i) => {
      if (v) {
        const duration = i.duration();
        return !((min !== undefined && Temporal.Duration.compare(min, duration) > 0) || (max !== undefined && Temporal.Duration.compare(max, duration) < 0));
      } else {
        return false;
      }
    });
  }

  public shorterThan(max: Temporal.Duration): Windows {
    return this.falsifyByDuration(undefined, max);
  }

  public longerThan(min: Temporal.Duration): Windows {
    return this.falsifyByDuration(min, undefined);
  }

  public shiftBy(shiftRising: Temporal.Duration, shiftFalling?: Temporal.Duration): Windows {
    if (shiftFalling === undefined) shiftFalling = shiftRising;
    const boundsMap = (bounds: Interval) => {
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
    return this.unsafe.mapIntervals((v, i) => {
      if (v) {
        return Interval.between(
          i.start.add(shiftRising),
          i.end.add(shiftFalling!),
          i.startInclusivity,
          i.endInclusivity
        );
      } else {
        return Interval.between(
          i.start.add(shiftFalling!),
          i.end.add(shiftRising),
          i.startInclusivity,
          i.endInclusivity
        );
      }
    }, boundsMap);
  }

  public starts = () => this.transitions(false, true);
  public ends = () => this.transitions(true, false);
}
