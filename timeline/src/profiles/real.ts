import {Profile, ProfileType, Windows} from '../internal.js';
import {Segment} from '../segment.js';
import type {Timeline} from '../timeline.js';
import {coalesce} from '../timeline.js';
import {Inclusivity, Interval} from '../interval.js';
import {BinaryOperation} from '../binary-operation.js';
import {Temporal} from '@js-temporal/polyfill';
import { fetcher } from "../data-fetcher.js";

// @ts-ignore
export class Real extends Profile<LinearEquation> {
  constructor(segments: Timeline<Segment<LinearEquation>>) {
    super(segments, ProfileType.Real);
  }

  public static empty(): Real {
    return new Real(async _ => []);
  }

  public static override Value(value: number, interval?: Interval): Real;
  public static override Value(value: LinearEquation, interval?: Interval): Real;
  public static override Value(value: number | LinearEquation, interval?: Interval): Real {
    if (typeof value === 'number') value = LinearEquation.Constant(value);
    return new Real(async bounds => [
      new Segment(value as LinearEquation, interval === undefined ? bounds : Interval.intersect(bounds, interval))
    ]);
  }

  public static override Resource(name: string): Real {
    return new Real(fetcher.resource(name, (v, t) => new LinearEquation(t, v.initial as number, v.rate as number), ProfileType.Real));
  }

  public override assignGaps(def: Profile<LinearEquation> | LinearEquation | number): Real {
    if (typeof def === 'number') def = LinearEquation.Constant(def);
    return super.assignGaps(def);
  }

  public negate(): Real {
    return this.mapValues(v => v.negate());
  }

  public abs(): Real {
    return this.unsafe.flatMap(
      (eq, i) => eq.abs(i),
      b => b
    );
  }

  public plus(other: number | Profile<LinearEquation>): Real {
    if (typeof other === 'number') other = Real.Value(other);
    return this.map2Values(
      other,
      BinaryOperation.combineOrUndefined((l, r) => l.plus(r))
    );
  }

  public minus(other: number | Profile<LinearEquation>): Real {
    if (typeof other === 'number') other = Real.Value(other);
    return this.map2Values(
      other,
      BinaryOperation.combineOrUndefined((l, r) => l.minus(r))
    );
  }

  public times(other: number | Profile<LinearEquation>): Real {
    if (typeof other === 'number') return this.mapValues(eq => eq.times(other));
    else
      return this.map2Values(
        other,
        BinaryOperation.combineOrUndefined((l, r, i) => {
          if (l.rate === 0) {
            return new LinearEquation(r.initialTime, l.initialValue * r.initialValue, l.initialValue * r.rate);
          } else if (r.rate === 0) {
            return new LinearEquation(l.initialTime, r.initialValue * l.initialValue, r.initialValue * l.rate);
          }
          throw new Error(
            `Cannot multiply two non-constant real profiles, rates were ${l.rate} and ${r.rate}. Interval: ${i}`
          );
        }),
        ProfileType.Real
      );
  }

  public dividedBy(other: number | Profile<LinearEquation>): Real {
    if (typeof other === 'number') return this.mapValues(eq => eq.dividedBy(other));
    else
      return this.map2Values(
        other,
        BinaryOperation.combineOrUndefined((l, r, i) => {
          if (r.rate !== 0)
            throw new Error(
              `Denominator of real profile division must be piece-wise constant. Segment: ${new Segment(r, i)}`
            );
          return l.dividedBy(r.initialValue);
        }),
        ProfileType.Real
      );
  }

  public pow(exp: number | Profile<LinearEquation>): Real {
    if (typeof exp === 'number') {
      if (exp === 1) return this;
      if (exp === 0) return Real.Value(1);
      exp = Real.Value(exp);
    }
    return this.map2Values(
      exp,
      BinaryOperation.combineOrUndefined((eq, e, i) => {
        if (e.rate !== 0)
          throw new Error(`Cannot raise to an exponent that is not piece-wise constant. Segment: ${new Segment(e, i)}`);
        if (eq.rate !== 0)
          throw new Error(
            `Cannot exponentiate a real profile that is not piece-wise constant. Segment: ${new Segment(eq, i)}`
          );
        const newValue = Math.pow(eq.initialValue, e.initialValue);
        if (isNaN(newValue))
          throw new Error(
            `Exponentiation returned NaN on numbers ${eq.initialValue} ^ ${e.initialValue}; see Math.pow documentation for possible reasons why. Interval: ${i}`
          );
        return new LinearEquation(eq.initialTime, newValue, 0);
      })
    );
  }

  public root(n: number): Real {
    return this.pow(1 / n);
  }

  public sqrt(): Real {
    return this.pow(0.5);
  }

  public rate(unit?: Temporal.Duration): Real {
    if (unit !== undefined) return this.mapValues(v => LinearEquation.Constant(v.rate));
    else return this.mapValues(v => LinearEquation.Constant((v.rate * unit!.total('microsecond')) / 1_000_000));
  }

  public integrate(unit: Temporal.Duration): Real {
    const timeline = async (bounds: Interval) => {
      const baseRate = 1 / unit.total('second');
      const segments = await this.segments(bounds);
      const result: Segment<LinearEquation>[] = [];
      let previousTime = bounds.start;
      let acc = 0;
      for (const segment of segments) {
        if (Temporal.Duration.compare(previousTime, segment.interval.start) !== 0)
          throw new Error(
            `Cannot integrate a real profile with gaps. Gap: ${Interval.Between(previousTime, segment.interval.start)}`
          );
        if (segment.value.rate !== 0)
          throw new Error(`Cannot integrate real profiles that aren't piecewise constant. Segment: ${segment}.`);
        let rate = segment.value.initialValue * baseRate;
        let nextAcc = acc + rate * segment.interval.duration().total('second');
        result.push(new Segment(new LinearEquation(previousTime, acc, rate), segment.interval));
        previousTime = segment.interval.end;
        acc = nextAcc;
      }
      return result;
    };
    return new Real(timeline);
  }

  public compare(other: Profile<LinearEquation>, comparator: (l: number, r: number) => boolean): Windows {
    return this.unsafe.flatMap2(
      other,
      BinaryOperation.combineOrUndefined((l, r, interval) => l.compare(r, comparator, interval)),
      b => b,
      ProfileType.Windows
    );
  }

  public override equalTo(other: number | Profile<LinearEquation>): Windows {
    if (typeof other === 'number') other = Real.Value(other);
    return this.compare(other, (l, r) => l === r);
  }

  public override notEqualTo(other: number | Profile<LinearEquation>): Windows {
    if (typeof other === 'number') other = Real.Value(other);
    return this.compare(other, (l, r) => l !== r);
  }

  public lessThan(other: number | Profile<LinearEquation>): Windows {
    if (typeof other === 'number') other = Real.Value(other);
    return this.compare(other, (l, r) => l < r);
  }

  public lessThanOrEqual(other: number | Profile<LinearEquation>): Windows {
    if (typeof other === 'number') other = Real.Value(other);
    return this.compare(other, (l, r) => l <= r);
  }

  public greaterThan(other: number | Profile<LinearEquation>): Windows {
    if (typeof other === 'number') other = Real.Value(other);
    return this.compare(other, (l, r) => l > r);
  }

  public greaterThanOrEqual(other: number | Profile<LinearEquation>): Windows {
    if (typeof other === 'number') other = Real.Value(other);
    return this.compare(other, (l, r) => l >= r);
  }

  public override changes(): Windows {
    const segments = async (bounds: Interval) => {
      let previous: Segment<LinearEquation> | undefined = undefined;
      return coalesce(
        (await this.segments(bounds)).flatMap(currentSegment => {
          let leftEdge: boolean | undefined;

          const currentInterval = currentSegment.interval;

          if (previous !== undefined) {
            if (
              Interval.compareEndToStart(previous.interval, currentInterval) === 0 &&
              currentInterval.includesStart()
            ) {
              leftEdge =
                previous.value.valueAt(currentInterval.start) === currentSegment.value.valueAt(currentInterval.start);
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
            new Segment(leftEdge, Interval.At(currentInterval.start)).transpose(),
            new Segment(
              currentSegment.value.rate !== 0,
              Interval.Between(currentInterval.start, currentInterval.end, Inclusivity.Exclusive)
            )
          ].filter($ => $ !== undefined) as Segment<boolean>[];
        }),
        ProfileType.Real
      );
    };
    return new Windows(segments);
  }

  public override transitions(from: number, to: number): Windows;
  public override transitions(from: LinearEquation, to: LinearEquation): Windows;
  public override transitions(from: number | LinearEquation, to: number | LinearEquation): Windows {
    if (typeof from === 'number') {
      return this.edges(
        BinaryOperation.combineOrUndefined((l, r, i) => l.valueAt(i.start) === from && r.valueAt(i.start) === to)
      );
    } else return super.transitions(from, to as LinearEquation);
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

  public abs(bounds: Interval): Segment<LinearEquation>[] {
    const root = this.intersectionPointWith(LinearEquation.Constant(0));
    if (root === undefined || !bounds.contains(root)) {
      if (this.valueAt(bounds.start) > 0)
        return [new Segment(new LinearEquation(this.initialTime, this.initialValue, this.rate), bounds)];
      else return [new Segment(new LinearEquation(this.initialTime, -this.initialValue, -this.rate), bounds)];
    }
    return [
      new Segment(
        new LinearEquation(root, 0, -Math.abs(this.rate)),
        Interval.Between(bounds.start, root, bounds.startInclusivity, Inclusivity.Exclusive)
      ),
      new Segment(
        new LinearEquation(root, 0, Math.abs(this.rate)),
        Interval.Between(root, bounds.end, Inclusivity.Inclusive, bounds.endInclusivity)
      )
    ];
  }

  public times(c: number): LinearEquation {
    return new LinearEquation(this.initialTime, c * this.initialValue, c * this.rate);
  }

  public dividedBy(c: number): LinearEquation {
    return this.times(1 / c);
  }

  public plus(other: LinearEquation | number): LinearEquation {
    if (typeof other === 'number') other = LinearEquation.Constant(other);
    const shifted = other.shiftInitialTime(this.initialTime);
    return new LinearEquation(this.initialTime, this.initialValue + shifted.initialValue, this.rate + other.rate);
  }

  public minus(other: LinearEquation | number): LinearEquation {
    if (typeof other === 'number') return this.plus(-other);
    return this.plus(other.negate());
  }

  public valueAt(time: Temporal.Duration): number {
    const change = this.rate * time.total('second') - this.rate * this.initialTime.total('second');
    return this.initialValue + change;
  }

  public shiftInitialTime(newInitialTime: Temporal.Duration): LinearEquation {
    return new LinearEquation(
      newInitialTime,
      this.initialValue + newInitialTime.subtract(this.initialTime).total('second') * this.rate,
      this.rate
    );
  }

  public equals(other: LinearEquation): boolean {
    return this.initialValue === other.valueAt(this.initialTime) && this.rate === other.rate;
  }

  public intersectionPointWith(other: LinearEquation): Temporal.Duration | undefined {
    if (this.rate === other.rate) return undefined;
    return this.initialTime.add(
      Temporal.Duration.from({
        microseconds: Math.round(
          (1_000_000 * (other.valueAt(this.initialTime) - this.initialValue)) / (this.rate - other.rate)
        )
      })
    );
  }

  public compare(
    other: LinearEquation,
    comparator: (l: number, r: number) => boolean,
    bounds: Interval
  ): Segment<boolean>[] {
    const intersection = this.intersectionPointWith(other);
    if (intersection === undefined)
      return [new Segment(comparator(this.initialValue, other.valueAt(this.initialTime)), bounds)];
    else {
      return [
        new Segment(
          comparator(this.valueAt(bounds.start), other.valueAt(bounds.start)),
          Interval.Between(bounds.start, intersection, bounds.startInclusivity, Inclusivity.Exclusive)
        ),
        new Segment(comparator(this.valueAt(intersection), other.valueAt(intersection)), Interval.At(intersection)),
        new Segment(
          comparator(this.valueAt(bounds.end), other.valueAt(bounds.end)),
          Interval.Between(intersection, bounds.end, Inclusivity.Exclusive, bounds.endInclusivity)
        )
      ].filter($ => !$.interval.isEmpty());
    }
  }
}
