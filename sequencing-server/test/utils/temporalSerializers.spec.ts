import { serializeWithTemporal, deserializeWithTemporal} from '../../src/utils/temporalSerializers.js';

const objectTree = {
  instant: Temporal.Instant.from('2020-01-01T01:01:01Z'),
  zonedDateTime: Temporal.ZonedDateTime.from('1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'),
  plainDate: Temporal.PlainDate.from('2020-01-01'),
  plainTime: Temporal.PlainTime.from('01:01:01'),
  plainDateTime: Temporal.PlainDateTime.from('2020-01-01T01:01:01'),
  plainYearMonth: Temporal.PlainYearMonth.from('2020-01'),
  plainMonthDay: Temporal.PlainMonthDay.from('01-01'),
  duration: Temporal.Duration.from('P1Y1M1DT1H1M1S'),
  calendar: Temporal.Calendar.from('iso8601'),
  timeZone: Temporal.TimeZone.from('Europe/London'),
  array: [
    Temporal.Instant.from('2020-01-01T01:01:01Z'),
    Temporal.ZonedDateTime.from('1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'),
    Temporal.PlainDate.from('2020-01-01'),
    Temporal.PlainTime.from('01:01:01'),
    Temporal.PlainDateTime.from('2020-01-01T01:01:01'),
    Temporal.PlainYearMonth.from('2020-01'),
    Temporal.PlainMonthDay.from('01-01'),
    Temporal.Duration.from('P1Y1M1DT1H1M1S'),
    Temporal.Calendar.from('iso8601'),
    Temporal.TimeZone.from('Europe/London'),
  ],
  record: {
    instant: Temporal.Instant.from('2020-01-01T01:01:01Z'),
    zonedDateTime: Temporal.ZonedDateTime.from('1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'),
    plainDate: Temporal.PlainDate.from('2020-01-01'),
    plainTime: Temporal.PlainTime.from('01:01:01'),
    plainDateTime: Temporal.PlainDateTime.from('2020-01-01T01:01:01'),
    plainYearMonth: Temporal.PlainYearMonth.from('2020-01'),
    plainMonthDay: Temporal.PlainMonthDay.from('01-01'),
    duration: Temporal.Duration.from('P1Y1M1DT1H1M1S'),
    calendar: Temporal.Calendar.from('iso8601'),
    timeZone: Temporal.TimeZone.from('Europe/London'),
  },
  deepRecord: {
    instant: Temporal.Instant.from('2020-01-01T01:01:01Z'),
    zonedDateTime: Temporal.ZonedDateTime.from('1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'),
    plainDate: Temporal.PlainDate.from('2020-01-01'),
    plainTime: Temporal.PlainTime.from('01:01:01'),
    plainDateTime: Temporal.PlainDateTime.from('2020-01-01T01:01:01'),
    plainYearMonth: Temporal.PlainYearMonth.from('2020-01'),
    plainMonthDay: Temporal.PlainMonthDay.from('01-01'),
    duration: Temporal.Duration.from('P1Y1M1DT1H1M1S'),
    calendar: Temporal.Calendar.from('iso8601'),
    timeZone: Temporal.TimeZone.from('Europe/London'),
    array: [
      Temporal.Instant.from('2020-01-01T01:01:01Z'),
      Temporal.ZonedDateTime.from('1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'),
      Temporal.PlainDate.from('2020-01-01'),
      Temporal.PlainTime.from('01:01:01'),
      Temporal.PlainDateTime.from('2020-01-01T01:01:01'),
      Temporal.PlainYearMonth.from('2020-01'),
      Temporal.PlainMonthDay.from('01-01'),
      Temporal.Duration.from('P1Y1M1DT1H1M1S'),
      Temporal.Calendar.from('iso8601'),
      Temporal.TimeZone.from('Europe/London'),
    ],
    record: {
      instant: Temporal.Instant.from('2020-01-01T01:01:01Z'),
      zonedDateTime: Temporal.ZonedDateTime.from('1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'),
      plainDate: Temporal.PlainDate.from('2020-01-01'),
      plainTime: Temporal.PlainTime.from('01:01:01'),
      plainDateTime: Temporal.PlainDateTime.from('2020-01-01T01:01:01'),
      plainYearMonth: Temporal.PlainYearMonth.from('2020-01'),
      plainMonthDay: Temporal.PlainMonthDay.from('01-01'),
      duration: Temporal.Duration.from('P1Y1M1DT1H1M1S'),
      calendar: Temporal.Calendar.from('iso8601'),
      timeZone: Temporal.TimeZone.from('Europe/London'),
    },
  }
};

const serializedObjectTree = {
  instant: {
    type: 'Temporal.Instant',
    value: '2020-01-01T01:01:01Z'
  },
  zonedDateTime: {
    type: 'Temporal.ZonedDateTime',
    value: '1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'
  },
  plainDate: {
    type: 'Temporal.PlainDate',
    value: '2020-01-01'
  },
  plainTime: {
    type: 'Temporal.PlainTime',
    value: '01:01:01'
  },
  plainDateTime: {
    type: 'Temporal.PlainDateTime',
    value: '2020-01-01T01:01:01'
  },
  plainYearMonth: {
    type: 'Temporal.PlainYearMonth',
    value: '2020-01'
  },
  plainMonthDay: {
    type: 'Temporal.PlainMonthDay',
    value: '01-01'
  },
  duration: {
    type: 'Temporal.Duration',
    value: 'P1Y1M1DT1H1M1S'
  },
  calendar: {
    type: 'Temporal.Calendar',
    value: 'iso8601'
  },
  timeZone: {
    type: 'Temporal.TimeZone',
    value: 'Europe/London'
  },
  array: [
    {
      type: 'Temporal.Instant',
      value: '2020-01-01T01:01:01Z'
    },
    {
      type: 'Temporal.ZonedDateTime',
      value: '1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'
    },
    {
      type: 'Temporal.PlainDate',
      value: '2020-01-01'
    },
    {
      type: 'Temporal.PlainTime',
      value: '01:01:01'
    },
    {
      type: 'Temporal.PlainDateTime',
      value: '2020-01-01T01:01:01'
    },
    {
      type: 'Temporal.PlainYearMonth',
      value: '2020-01'
    },
    {
    type: 'Temporal.PlainMonthDay',
    value: '01-01'
  },
    {
      type: 'Temporal.Duration',
      value: 'P1Y1M1DT1H1M1S'
    },
    {
      type: 'Temporal.Calendar',
      value: 'iso8601'
    },
    {
      type: 'Temporal.TimeZone',
      value: 'Europe/London'
    }
  ],
  record: {
    instant: {
      type: 'Temporal.Instant',
      value: '2020-01-01T01:01:01Z'
    },
    zonedDateTime: {
      type: 'Temporal.ZonedDateTime',
      value: '1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'
    },
    plainDate: {
      type: 'Temporal.PlainDate',
      value: '2020-01-01'
    },
    plainTime: {
      type: 'Temporal.PlainTime',
      value: '01:01:01'
    },
    plainDateTime: {
      type: 'Temporal.PlainDateTime',
      value: '2020-01-01T01:01:01'
    },
    plainYearMonth: {
      type: 'Temporal.PlainYearMonth',
      value: '2020-01'
    },
    plainMonthDay: {
      type: 'Temporal.PlainMonthDay',
      value: '01-01'
    },
    duration: {
      type: 'Temporal.Duration',
      value: 'P1Y1M1DT1H1M1S'
    },
    calendar: {
      type: 'Temporal.Calendar',
      value: 'iso8601'
    },
    timeZone: {
      type: 'Temporal.TimeZone',
      value: 'Europe/London'
    }
  },
  deepRecord: {
    instant: {
      type: 'Temporal.Instant',
      value: '2020-01-01T01:01:01Z'
    },
    zonedDateTime: {
      type: 'Temporal.ZonedDateTime',
      value: '1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'
    },
    plainDate: {
      type: 'Temporal.PlainDate',
      value: '2020-01-01'
    },
    plainTime: {
      type: 'Temporal.PlainTime',
      value: '01:01:01'
    },
    plainDateTime: {
      type: 'Temporal.PlainDateTime',
      value: '2020-01-01T01:01:01'
    },
    plainYearMonth: {
      type: 'Temporal.PlainYearMonth',
      value: '2020-01'
    },
    plainMonthDay: {
      type: 'Temporal.PlainMonthDay',
      value: '01-01'
    },
    duration: {
      type: 'Temporal.Duration',
      value: 'P1Y1M1DT1H1M1S'
    },
    calendar: {
      type: 'Temporal.Calendar',
      value: 'iso8601'
    },
    timeZone: {
      type: 'Temporal.TimeZone',
      value: 'Europe/London'
    },
    array: [
      {
        type: 'Temporal.Instant',
        value: '2020-01-01T01:01:01Z'
      },
      {
        type: 'Temporal.ZonedDateTime',
        value: '1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'
      },
      {
        type: 'Temporal.PlainDate',
        value: '2020-01-01'
      },
      {
        type: 'Temporal.PlainTime',
        value: '01:01:01'
      },
      {
        type: 'Temporal.PlainDateTime',
        value: '2020-01-01T01:01:01'
      },
      {
        type: 'Temporal.PlainYearMonth',
        value: '2020-01'
      },
      {
        type: 'Temporal.PlainMonthDay',
        value: '01-01'
      },
      {
        type: 'Temporal.Duration',
        value: 'P1Y1M1DT1H1M1S'
      },
      {
        type: 'Temporal.Calendar',
        value: 'iso8601'
      },
      {
        type: 'Temporal.TimeZone',
        value: 'Europe/London'
      }
    ],
    record: {
      instant: {
        type: 'Temporal.Instant',
        value: '2020-01-01T01:01:01Z'
      },
      zonedDateTime: {
        type: 'Temporal.ZonedDateTime',
        value: '1995-12-07T03:24:30.0000035-08:00[America/Los_Angeles]'
      },
      plainDate: {
        type: 'Temporal.PlainDate',
        value: '2020-01-01'
      },
      plainTime: {
        type: 'Temporal.PlainTime',
        value: '01:01:01'
      },
      plainDateTime: {
        type: 'Temporal.PlainDateTime',
        value: '2020-01-01T01:01:01'
      },
      plainYearMonth: {
        type: 'Temporal.PlainYearMonth',
        value: '2020-01'
      },
      plainMonthDay: {
        type: 'Temporal.PlainMonthDay',
        value: '01-01'
      },
      duration: {
        type: 'Temporal.Duration',
        value: 'P1Y1M1DT1H1M1S'
      },
      calendar: {
        type: 'Temporal.Calendar',
        value: 'iso8601'
      },
      timeZone: {
        type: 'Temporal.TimeZone',
        value: 'Europe/London'
      }
    },
  }
};

it('should serialize temporal objects', () => {
  const serialized = serializeWithTemporal(objectTree);

  expect(serialized).toEqual(serializedObjectTree);
})

it('should deserialize temporal objects', () => {
  const deserialized = deserializeWithTemporal(serializedObjectTree);

  expect(deserialized).toEqual(objectTree);
})
