import {
  applyOperation,
  BoundsMap,
  coalesce,
  LinearEquation,
  Profile,
  ProfileType,
  Real,
  Timeline,
  truncate
} from '../internal.js';
import { Segment } from '../segment.js';
import { BinaryOperation } from '../binary-operation.js';
import { Interval } from '../interval.js';
import { Temporal } from '@js-temporal/polyfill';
import { fetcher } from '../data-fetcher.js';
import { shiftEdgesBoundsMap } from '../bounds-utils.js';

// @ts-ignore
export class Windows extends Profile<boolean> {
  constructor(segments: Timeline<Segment<boolean>>) {
    super(segments, ProfileType.Windows);
  }

  public static override Empty(): Windows {
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
    const shiftInterval = (s: Segment<boolean>) => {
      if (s.value) {
        return new Segment(true, s.interval.shiftBy(shiftRising, shiftFalling!));
      } else {
        return new Segment(false, s.interval.shiftBy(shiftFalling!, shiftRising));
      }
    };
    // I would delegate to `this.unsafe.mapIntervals`, but the result needs to be truncated, so a custom operation it is.
    const windowsShiftByOp = ({ next: bounds }: { next: Interval }, [$]: Segment<boolean>[][]) =>
      truncate(coalesce($.map(shiftInterval), ProfileType.Windows), bounds);
    const timeline = applyOperation(windowsShiftByOp, shiftEdgesBoundsMap(shiftRising, shiftFalling), this.segments);
    return new Windows(timeline);
  }

  public starts = () => this.transitions(false, true);
  public ends = () => this.transitions(true, false);

  public accumulatedDuration(unit: Temporal.Duration): Real {
    return this.mapValues(b => LinearEquation.Constant(b ? 1 : 0), ProfileType.Real).integrate(unit);
  }

  public override async any(predicate?: (v: boolean, i: Interval) => boolean, bounds?: Interval): Promise<boolean> {
    if (predicate === undefined) predicate = (b: boolean) => b;
    return await super.any(predicate, bounds);
  }

  public override async all(predicate?: (v: boolean, i: Interval) => boolean, bounds?: Interval): Promise<boolean> {
    if (predicate === undefined) predicate = (b: boolean) => b;
    return await super.all(predicate, bounds);
  }
}
