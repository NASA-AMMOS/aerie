import {BoundsMap} from "./timeline.js";
import {Temporal} from "@js-temporal/polyfill";
import {Interval} from "./interval.js";
import Duration = Temporal.Duration;

export const shiftEdgesBoundsMap = (shiftRising: Duration, shiftFalling: Duration) => ({
  eager: (bounds: Interval) => {
    let start: Temporal.Duration;
    let end: Temporal.Duration;
    if (Temporal.Duration.compare(shiftRising, shiftFalling!) === 1) {
      start = bounds.start.add(shiftRising);
      end = bounds.end.add(shiftFalling!);
    } else {
      start = bounds.start.add(shiftFalling!);
      end = bounds.end.add(shiftRising);
    }
    return Interval.Between(start, end, bounds.startInclusivity, bounds.endInclusivity);
  },
  lazy: (bounds: Interval) => {
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
  }
});
