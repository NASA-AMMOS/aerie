import {bound, Interval, IntervalLike, Segment, Spans, Windows} from "../internal.js";
import {ActivityTypeName, ActivityTypeParameterMap} from "../dynamic/activity-type.js";

export class ActivityInstance<A extends ActivityTypeName> implements IntervalLike {
  public constructor(
      public readonly type: A,
      public readonly interval: Interval,
      public readonly parameters: ActivityTypeParameterMap[A],
      public readonly directive_id: number
  ) {}

  /**
   * Produces a span for the duration of the activity.
   */
  public span(): Spans<ActivityInstance<A>> {
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
  public start(): Spans<ActivityInstance<A>> {
    return this.span().starts();
  }

  /**
   * Produces an instantaneous window at the end of the activity.
   */
  public end(): Spans<ActivityInstance<A>> {
    return this.span().ends();
  }

  public bound(bounds: Interval): this | undefined {
    const intersection = Interval.intersect(bounds, this.interval);
    if (intersection.isEmpty()) return undefined;
    else {
      // @ts-ignore
      return new ActivityInstance<A>(
          this.type,
          intersection,
          this.parameters,
          this.directive_id
      );
    }
  }

  public mapInterval(map: (i: this) => Interval): this {
    // @ts-ignore
    return new ActivityInstance<A>(
        this.type,
        map(this),
        this.parameters,
        this.directive_id
    );
  }
}
