function isTemporalDuration<T>(thing: T | Temporal.Duration): thing is Temporal.Duration {
  return thing instanceof Temporal.Duration;
}

function isTemporalInstant<T>(thing: T | Temporal.Instant): thing is Temporal.Instant {
  return thing instanceof Temporal.Instant;
}

function isTemporalZonedDateTime<T>(thing: T | Temporal.ZonedDateTime): thing is Temporal.ZonedDateTime {
  return thing instanceof Temporal.ZonedDateTime;
}

function isTemporalCalendar<T>(thing: T | Temporal.Calendar): thing is Temporal.Calendar {
  return thing instanceof Temporal.Calendar;
}

function isTemporalPlainDate<T>(thing: T | Temporal.PlainDate): thing is Temporal.PlainDate {
  return thing instanceof Temporal.PlainDate;
}

function isTemporalPlainTime<T>(thing: T | Temporal.PlainTime): thing is Temporal.PlainTime {
  return thing instanceof Temporal.PlainTime;
}

function isTemporalPlainDateTime<T>(thing: T | Temporal.PlainDateTime): thing is Temporal.PlainDateTime {
  return thing instanceof Temporal.PlainDateTime;
}

function isTemporalPlainMonthDay<T>(thing: T | Temporal.PlainMonthDay): thing is Temporal.PlainMonthDay {
  return thing instanceof Temporal.PlainMonthDay;
}

function isTemporalPlainYearMonth<T>(thing: T | Temporal.PlainYearMonth): thing is Temporal.PlainYearMonth {
  return thing instanceof Temporal.PlainYearMonth;
}

function isTemporalTimeZone<T>(thing: T | Temporal.TimeZone): thing is Temporal.TimeZone {
  return thing instanceof Temporal.TimeZone;
}

function isRecord<T>(thing: T | Record<string, any>): thing is Record<string, any> {
  return typeof thing == 'object'
      && thing !== null
      && Object.getPrototypeOf(thing) === Object.prototype;
}

export function serializeWithTemporal<T>(object: T): SerializedTemporal<T> {
  if (isTemporalDuration(object)) {
    return {
      type: 'Temporal.Duration',
      value: object.toString(),
    } as SerializedTemporal<T>;
  }
  else if (isTemporalInstant(object)) {
    return {
      type: 'Temporal.Instant',
      value: object.toString(),
    } as SerializedTemporal<T>;
  }
  else if (isTemporalZonedDateTime(object)) {
    return {
      type: 'Temporal.ZonedDateTime',
      value: object.toString(),
    } as SerializedTemporal<T>;
  }
  else if (isTemporalCalendar(object)) {
    return {
      type: 'Temporal.Calendar',
      value: object.toString(),
    } as SerializedTemporal<T>;
  }
  else if (isTemporalPlainDate(object)) {
    return {
      type: 'Temporal.PlainDate',
      value: object.toString(),
    } as SerializedTemporal<T>;
  }
  else if (isTemporalPlainTime(object)) {
    return {
      type: 'Temporal.PlainTime',
      value: object.toString(),
    } as SerializedTemporal<T>;
  }
  else if (isTemporalPlainDateTime(object)) {
    return {
      type: 'Temporal.PlainDateTime',
      value: object.toString(),
    } as SerializedTemporal<T>;
  }
  else if (isTemporalPlainMonthDay(object)) {
    return {
      type: 'Temporal.PlainMonthDay',
      value: object.toString(),
    } as SerializedTemporal<T>;
  }
  else if (isTemporalPlainYearMonth(object)) {
    return {
      type: 'Temporal.PlainYearMonth',
      value: object.toString(),
    } as SerializedTemporal<T>;
  }
  else if (isTemporalTimeZone(object)) {
    return {
      type: 'Temporal.TimeZone',
      value: object.toString(),
    } as SerializedTemporal<T>;
  }
  else if (Array.isArray(object)) {
    return object.map(item => serializeWithTemporal(item)) as any as SerializedTemporal<T>;
  }
  else if (isRecord(object)) {
    return (Object.keys(object) as (keyof T)[]).reduce((acc, key) => {
      acc[key] = serializeWithTemporal(object[key]);
      return acc;
    }, {} as { [key in keyof T]: SerializedTemporal<T[key]> }) as SerializedTemporal<T>;
  }
  return object as SerializedTemporal<T>;
}

export function deserializeWithTemporal<T>(object: T): DeserializedTemporal<T> {
  if (Array.isArray(object)) {
    return object.map(item => deserializeWithTemporal(item)) as any as DeserializedTemporal<T>;
  }
  else if (isRecord(object)) {
    const objectAsRecord = object as Record<string, any>;
    if ('type' in objectAsRecord && 'value' in objectAsRecord) {
      if (objectAsRecord['type'] === 'Temporal.Duration') {
        return Temporal.Duration.from(objectAsRecord['value']) as DeserializedTemporal<T>;
      }
      else if (objectAsRecord['type'] === 'Temporal.Instant') {
        return Temporal.Instant.from(objectAsRecord['value']) as DeserializedTemporal<T>;
      }
      else if (objectAsRecord['type'] === 'Temporal.ZonedDateTime') {
        return Temporal.ZonedDateTime.from(objectAsRecord['value']) as DeserializedTemporal<T>;
      }
      else if (objectAsRecord['type'] === 'Temporal.Calendar') {
        return Temporal.Calendar.from(objectAsRecord['value']) as DeserializedTemporal<T>;
      }
      else if (objectAsRecord['type'] === 'Temporal.PlainDate') {
        return Temporal.PlainDate.from(objectAsRecord['value']) as DeserializedTemporal<T>;
      }
      else if (objectAsRecord['type'] === 'Temporal.PlainTime') {
        return Temporal.PlainTime.from(objectAsRecord['value']) as DeserializedTemporal<T>;
      }
      else if (objectAsRecord['type'] === 'Temporal.PlainDateTime') {
        return Temporal.PlainDateTime.from(objectAsRecord['value']) as DeserializedTemporal<T>;
      }
      else if (objectAsRecord['type'] === 'Temporal.PlainMonthDay') {
        return Temporal.PlainMonthDay.from(objectAsRecord['value']) as DeserializedTemporal<T>;
      }
      else if (objectAsRecord['type'] === 'Temporal.PlainYearMonth') {
        return Temporal.PlainYearMonth.from(objectAsRecord['value']) as DeserializedTemporal<T>;
      }
      else if (objectAsRecord['type'] === 'Temporal.TimeZone') {
        return Temporal.TimeZone.from(objectAsRecord['value']) as DeserializedTemporal<T>;
      }
    }
    else {
      return (Object.keys(object) as (keyof T)[]).reduce((acc, key) => {
        acc[key] = deserializeWithTemporal(object[key]);
        return acc;
      }, {} as {[key in keyof T]: DeserializedTemporal<T[key]> }) as DeserializedTemporal<T>;
    }
  }
  return object as DeserializedTemporal<T>;
}

type SerializedTemporal<T> =
    T extends Temporal.Instant ? { type: 'Temporal.Instant', value: ReturnType<Temporal.Instant['toString']> }
    : T extends Temporal.ZonedDateTime ? { type: 'Temporal.ZonedDateTime', value: ReturnType<Temporal.ZonedDateTime['toString']> }
    : T extends Temporal.PlainDate ? { type: 'Temporal.PlainDate', value: ReturnType<Temporal.PlainDate['toString']> }
    : T extends Temporal.PlainTime ? { type: 'Temporal.PlainTime', value: ReturnType<Temporal.PlainTime['toString']> }
    : T extends Temporal.PlainDateTime ? { type: 'Temporal.PlainDateTime', value: ReturnType<Temporal.PlainDateTime['toString']> }
    : T extends Temporal.PlainYearMonth ? { type: 'Temporal.PlainYearMonth', value: ReturnType<Temporal.PlainYearMonth['toString']> }
    : T extends Temporal.PlainMonthDay ? { type: 'Temporal.PlainMonthDay', value: ReturnType<Temporal.PlainMonthDay['toString']> }
    : T extends Temporal.Duration ? { type: 'Temporal.Duration', value: ReturnType<Temporal.Duration['toString']> }
    : T extends Temporal.TimeZone ? { type: 'Temporal.TimeZone', value: ReturnType<Temporal.TimeZone['toString']> }
    : T extends Temporal.Calendar ? { type: 'Temporal.Calendar', value: ReturnType<Temporal.Calendar['toString']> }
    : T extends Record<string, any> ? { [Property in keyof T]: SerializedTemporal<T[Property]> }
    : T;

type DeserializedTemporal<T extends Record<string, any>> =
    T extends  { type: 'Temporal.Duration', value: ReturnType<Temporal.Duration['toString']> } ? Temporal.Duration
    : T extends { type: 'Temporal.Instant', value: ReturnType<Temporal.Instant['toString']> } ? Temporal.Instant
    : T extends { type: 'Temporal.ZonedDateTime', value: ReturnType<Temporal.ZonedDateTime['toString']> } ? Temporal.ZonedDateTime
    : T extends { type: 'Temporal.Calendar', value: ReturnType<Temporal.Calendar['toString']> } ? Temporal.Calendar
    : T extends { type: 'Temporal.PlainDate', value: ReturnType<Temporal.PlainDate['toString']> } ? Temporal.PlainDate
    : T extends { type: 'Temporal.PlainTime', value: ReturnType<Temporal.PlainTime['toString']> } ? Temporal.PlainTime
    : T extends { type: 'Temporal.PlainDateTime', value: ReturnType<Temporal.PlainDateTime['toString']> } ? Temporal.PlainDateTime
    : T extends { type: 'Temporal.PlainMonthDay', value: ReturnType<Temporal.PlainMonthDay['toString']> } ? Temporal.PlainMonthDay
    : T extends { type: 'Temporal.PlainYearMonth', value: ReturnType<Temporal.PlainYearMonth['toString']> } ? Temporal.PlainYearMonth
    : T extends { type: 'Temporal.TimeZone', value: ReturnType<Temporal.TimeZone['toString']> } ? Temporal.TimeZone
    : T extends Record<string, any> ? { [Property in keyof T]: DeserializedTemporal<T[Property]> }
    : T;
