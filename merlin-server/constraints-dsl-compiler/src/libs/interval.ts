export enum Inclusivity {
  Inclusive,
  Exclusive
}

export class Interval {
  public start: number;
  public end: number;
  public startInclusivity: Inclusivity;
  public endInclusivity: Inclusivity;

  constructor(start: number, end: number, startInclusivity: Inclusivity, endInclusivity: Inclusivity) {
    this.start = start;
    this.end = end;
    this.startInclusivity = startInclusivity;
    this.endInclusivity = endInclusivity;
  }

  public static readonly Forever = new Interval(0, 1, Inclusivity.Inclusive, Inclusivity.Exclusive);
}
