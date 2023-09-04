import { Temporal } from '@js-temporal/polyfill';

globalThis.Temporal = Temporal as unknown as typeof globalThis.Temporal;
