import { Temporal as TemporalPolyfill, Intl as InltPolyfill, toTemporalInstant } from '@js-temporal/polyfill';

declare global {
  interface Date {
    toTemporalInstant: typeof toTemporalInstant;
  }

  var Temporal: typeof TemporalPolyfill;
  //@ts-ignore
  //var Intl: typeof InltPolyfill;
}

globalThis.Temporal = TemporalPolyfill;
//@ts-ignore
//globalThis.Intl = InltPolyfill;

Date.prototype.toTemporalInstant = toTemporalInstant;
