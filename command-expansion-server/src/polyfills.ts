import { Temporal, toTemporalInstant } from '@js-temporal/polyfill';

globalThis.Temporal = Temporal as typeof globalThis.Temporal;

Date.prototype.toTemporalInstant = toTemporalInstant as typeof Date.prototype.toTemporalInstant;
