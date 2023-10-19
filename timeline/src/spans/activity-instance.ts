import { bound, Interval, IntervalLike, Segment, Spans, Windows } from '../internal.js';

export class ActivityInstance<P = { [key: string]: any }> implements IntervalLike {
  public constructor(
    public readonly type: string,
    public readonly interval: Interval,
    public readonly parameters: P,
    public readonly directive_id: number
  ) {}

  /**
   * Produces a span for the duration of the activity.
   */
  public span(): Spans<ActivityInstance<P>> {
    return new Spans(bound([this]));
  }

  /**
   * Produces a window for the duration of the activity.
   */
  public window(): Windows {
    return new Windows(bound([new Segment(true, this.interval)])).assignGaps(false);
  }

  /**
   * Produces an instantaneous window at the start of the activity.
   */
  public start(): Spans<ActivityInstance<P>> {
    return this.span().starts();
  }

  /**
   * Produces an instantaneous window at the end of the activity.
   */
  public end(): Spans<ActivityInstance<P>> {
    return this.span().ends();
  }

  public bound(bounds: Interval): this | undefined {
    const intersection = Interval.intersect(bounds, this.interval);
    if (intersection.isEmpty()) return undefined;
    else {
      // @ts-ignore
      return new ActivityInstance<P>(this.type, intersection, this.parameters, this.directive_id);
    }
  }

  public mapInterval(map: (i: this) => Interval): this {
    // @ts-ignore
    return new ActivityInstance<P>(this.type, map(this), this.parameters, this.directive_id);
  }
}
