import {Interval} from "../interval";
import {Segment} from "../segment";
import database from "../database";
import {Profile} from "./profile";
import {ProfileType} from "./profile-type";

export class Discrete {
  /**
   * @deprecated
   *
   * @param value
   * @param interval
   * @constructor
   */
  public static Value<V>(value: V, interval?: Interval): Profile<V> {
    return new Profile<V>(bounds => [new Segment(
        value,
        interval === undefined ? bounds : Interval.intersect(bounds, interval)
    )], ProfileType.Other);
  }

  /**
   * @deprecated
   *
   * @param name
   * @constructor
   */
  public static Resource<V>(name: string): Profile<V> {
    return new Profile<V>(database.getResource(name), ProfileType.Other);
  }
}
