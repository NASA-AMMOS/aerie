import {Interval} from "../src/interval.js";
import {Temporal} from "@js-temporal/polyfill";
import Duration = Temporal.Duration;
import {Segment} from "../src/segment.js";
import {bound} from "../src/timeline.js";

const dur = (m: number) => Duration.from({ minutes: m });
const between = Interval.Between;
const at = Interval.At;

describe("bound",  () => {
  const bounds = between(dur(0), dur(10));
  test("bound array", async () => {
    const input = [new Segment(true, between(dur(4), dur(5)))];
    const result = await bound(input)(bounds);

    expect(result).toEqual(input);
  });
})
