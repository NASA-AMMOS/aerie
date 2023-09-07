import {LinearEquation, Profile, ProfileType, Real, Timeline} from '../internal.js';
import {Segment} from '../segment.js';
import {BinaryOperation} from '../binary-operation.js';
import {Interval} from '../interval.js';
import {Temporal} from '@js-temporal/polyfill';
import { fetcher } from "../data-fetcher.js";

// @ts-ignore
export class Windows extends Profile<boolean> {
  constructor(segments: Timeline<Segment<boolean>>) {
    super(segments, ProfileType.Windows);
  }

  public static empty(): Windows {
    return new Windows(async _ => []);
  }

  public static override Value(value: boolean, interval?: Interval): Windows {
    return new Windows(async bounds => [
      new Segment(value, interval === undefined ? bounds : Interval.intersect(bounds, interval))
    ]);
  }

  public static override Resource(name: string): Windows {
    return new Windows(fetcher.resource(name, $ => $ as boolean, ProfileType.Windows));
  }

  public not(): Windows {
    return this.mapValues($ => !$);
  }

  public and(other: Windows): Windows {
    return this.map2Values(
      other,
      BinaryOperation.cases(
        l => (l ? undefined : false),
        r => (r ? undefined : false),
        (l, r) => l && r
      ),
      ProfileType.Windows
    );
  }

  public or(other: Windows): Windows {
    return this.map2Values(
      other,
      BinaryOperation.cases(
        l => (l ? true : undefined),
        r => (r ? true : undefined),
        (l, r) => l || r
      ),
      ProfileType.Windows
    );
  }

  public add(other: Windows): Windows {
    return this.map2Values(
      other,
      BinaryOperation.combineOrIdentity((l, r) => l || r),
      ProfileType.Windows
    );
  }

  public falsifyByDuration(min?: Temporal.Duration, max?: Temporal.Duration): Windows {
    return this.mapValues((v, i) => {
      if (v) {
        const duration = i.duration();
        return !(
          (min !== undefined && Temporal.Duration.compare(min, duration) > 0) ||
          (max !== undefined && Temporal.Duration.compare(max, duration) < 0)
        );
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

  public override shiftBy(shiftRising: Temporal.Duration, shiftFalling?: Temporal.Duration): Windows {
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
      return Interval.Between(start, end, bounds.startInclusivity, bounds.endInclusivity);
    };
    return this.unsafe.mapIntervals((v, i) => {
      if (v) {
        return i.shiftBy(shiftRising, shiftFalling!);
      } else {
        return i.shiftBy(shiftFalling!, shiftRising);
      }
    }, boundsMap);
  }

  public starts = () => this.transitions(false, true);
  public ends = () => this.transitions(true, false);

  public accumulatedDuration(unit: Temporal.Duration): Real {
    return this.mapValues(b => LinearEquation.Constant(b ? 1 : 0), ProfileType.Real).integrate(unit);
  }
}
