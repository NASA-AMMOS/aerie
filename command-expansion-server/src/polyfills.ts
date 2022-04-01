import { Temporal as TemporalPolyfill, toTemporalInstant } from '@js-temporal/polyfill';

globalThis.Temporal = TemporalPolyfill;

Date.prototype.toTemporalInstant = toTemporalInstant;
