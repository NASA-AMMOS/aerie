import {Profile, ProfileType} from "./profile";
import {Segment} from "./segment";
import database from "./database";
import type {Timeline} from "./timeline";
import {Inclusivity, Interval} from "./interval";
import {BinaryOperation} from "./binary-operation";
import {Windows} from "./windows";
import {coalesce} from "./timeline";

export class Real extends Profile<LinearEquation> {
  constructor(segments: Timeline<Segment<LinearEquation>>) {
    super(segments, ProfileType.Real);
  }

  public static empty(): Real {
    return new Real(_ => []);
  }

  public static Value(value: number, interval?: Interval): Real {
    return new Real(bounds => [new Segment(
        LinearEquation.Constant(value),
        interval === undefined ? bounds : Interval.intersect(bounds, interval)
    )]);
  }

  public static Resource(name: string): Real {
    return new Real(database.getResource(name));
  }

  public times(coefficient: number): Real {
    return this.mapValues(eq => eq.times(coefficient));
  }

  public plus(other: Profile<LinearEquation>): Real {
    return this.map2Values(other, BinaryOperation.combineOrUndefined((l, r) => l.plus(r)));
  }

  public minus(other: Profile<LinearEquation>): Real {
    return this.map2Values(other, BinaryOperation.combineOrUndefined((l, r) => l.minus(r)));
  }

  public rate(unit?: Temporal.Duration): Real {
    if (unit !== undefined) return this.mapValues(v => LinearEquation.Constant(v.rate));
    else return this.mapValues(v => LinearEquation.Constant(v.rate * unit!.total('microsecond') / 1_000_000));
  }

  public compare(other: Profile<LinearEquation>, comparator: (l: number, r: number) => boolean): Windows {
    return this.unsafe.flatMap2(other, BinaryOperation.combineOrUndefined(
        (l, r, interval) => l.compare(r, comparator, interval)
    ), ProfileType.Windows);
  }

  public override equalTo(other: Profile<LinearEquation>): Windows {
    return this.compare(other, (l, r) => l === r);
  }

  public override notEqualTo(other: Profile<LinearEquation>): Windows {
    return this.compare(other, (l, r) => l !== r);
  }

  public lessThan(other: Profile<LinearEquation>): Windows {
    return this.compare(other, (l, r) => l < r);
  }

  public lessThanOrEqual(other: Profile<LinearEquation>): Windows {
    return this.compare(other, (l, r) => l <= r);
  }

  public greaterThan(other: Profile<LinearEquation>): Windows {
    return this.compare(other, (l, r) => l > r);
  }

  public greaterThanOrEqual(other: Profile<LinearEquation>): Windows {
    return this.compare(other, (l, r) => l >= r);
  }

  public override changes(): Windows {
    const segments = (bounds: Interval) => {
      let previous: Segment<LinearEquation> | undefined = undefined;
      return coalesce(this.segments(bounds).flatMap(
        currentSegment => {
          let leftEdge: boolean | undefined;

          const currentInterval = currentSegment.interval;

          if (previous !== undefined) {
            if (Interval.compareEndToStart(previous.interval, currentInterval) === 0 && currentInterval.includesStart()) {
              leftEdge = previous.value.valueAt(currentInterval.start) === currentSegment.value.valueAt(currentInterval.start);
            } else {
              leftEdge = undefined;
            }
          } else {
            if (Interval.compareStarts(currentInterval, bounds) === 0) {
              leftEdge = currentSegment.value.rate !== 0;
            } else {
              leftEdge = undefined;
            }
          }

          return [
            (new Segment(leftEdge, Interval.at(currentInterval.start))).transpose(),
            new Segment(currentSegment.value.rate !== 0, Interval.between(currentInterval.start, currentInterval.end, Inclusivity.Exclusive))
          ].filter($ => $ !== undefined) as Segment<boolean>[];
        }
      ));
    }
    return new Windows(segments);
  }

  // @ts-ignore
  public override transitions(from: number, to: number): Windows {
    return this.edges(BinaryOperation.combineOrUndefined(
        (l, r, i) => l.valueAt(i.start) === from && r.valueAt(i.start) === to
    ));
  }
}

export class LinearEquation {
  public initialTime: Temporal.Duration;
  public initialValue: number;
  public rate: number;

  constructor(initialTime: Temporal.Duration, initialValue: number, rate: number) {
    this.initialTime = initialTime;
    this.initialValue = initialValue;
    this.rate = rate;
  }

  public static Constant(c: number): LinearEquation {
    return new LinearEquation(new Temporal.Duration(), c, 0);
  }

  public negate(): LinearEquation {
    return new LinearEquation(this.initialTime, -this.initialValue, -this.rate);
  }

  public times(c: number): LinearEquation {
    return new LinearEquation(this.initialTime, c * this.initialValue, c * this.rate);
  }

  public plus(other: LinearEquation): LinearEquation {
    const shifted = other.shiftInitialTime(this.initialTime);
    return new LinearEquation(this.initialTime, this.initialValue + other.initialValue, this.rate + other.rate);
  }

  public minus(other: LinearEquation): LinearEquation {
    return this.plus(other.negate());
  }

  public valueAt(time: Temporal.Duration): number {
    const change = this.rate*time.total('second') - this.rate*this.initialTime.total('second');
    return this.initialValue + change;
  }

  public shiftInitialTime(newInitialTime: Temporal.Duration): LinearEquation {
    return new LinearEquation(
        newInitialTime,
        this.initialValue + newInitialTime.subtract(this.initialTime).total('second')*this.rate,
        this.rate
    );
  }

  public equals(other: LinearEquation): boolean {
    return this.initialValue === other.valueAt(this.initialTime)
      && this.rate === other.rate;
  }

  public intersectionPointWith(other: LinearEquation): Temporal.Duration | undefined {
    if (this.rate === other.rate) return undefined;
    return this.initialTime.add(
        Temporal.Duration.from({
          microseconds: Math.round(1_000_000 * (other.valueAt(this.initialTime) - this.initialValue) / (this.rate - other.rate))
        })
    )
  }

  public compare(other: LinearEquation, comparator: (l: number, r: number) => boolean, bounds: Interval): Segment<boolean>[] {
    const intersection = this.intersectionPointWith(other);
    if (intersection === undefined) return [new Segment(comparator(this.initialValue, other.valueAt(this.initialTime)), bounds)];
    else {
      return [
        new Segment(
            comparator(this.valueAt(bounds.start), other.valueAt(bounds.start)),
            Interval.between(bounds.start, intersection, bounds.startInclusivity, Inclusivity.Exclusive)
        ),
        new Segment(
            comparator(this.valueAt(intersection), other.valueAt(intersection)),
            Interval.at(intersection)
        ),
        new Segment(
            comparator(this.valueAt(bounds.end), other.valueAt(bounds.end)),
            Interval.between(intersection, bounds.end, Inclusivity.Exclusive, bounds.endInclusivity)
        )
      ].filter($ => !$.interval.isEmpty());
    }
  }
}
