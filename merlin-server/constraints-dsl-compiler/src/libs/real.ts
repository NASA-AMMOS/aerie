import {Profile, ProfileType} from "./profile";
import {Segment} from "./segment";
import database from "./database";
import type {Timeline} from "./timeline";

export class Real extends Profile<LinearEquation> {
  constructor(segments: Timeline<Segment<LinearEquation>>) {
    super(segments, ProfileType.Real);
  }

  public static empty(): Real {
    return new Real(_ => []);
  }

  public static Value(value: number): Real {
    return new Real(bounds => [new Segment(bounds, LinearEquation.Constant(value))]);
  }

  public static Resource(name: string): Real {
    return new Real(database.getResource(name));
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
}
