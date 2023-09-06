import { Interval } from '../interval.js';
import { Segment } from '../segment.js';
import database from '../database.js';
import { Profile } from '../internal.js';
import { ProfileType } from './profile-type.js';

export class Discrete {
  /**
   * @deprecated
   *
   * @param value
   * @param interval
   * @constructor
   */
  public static Value<V>(value: V, interval?: Interval): Profile<V> {
    return new Profile<V>(
      async bounds => [new Segment(value, interval === undefined ? bounds : Interval.intersect(bounds, interval))],
      ProfileType.Other
    );
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
