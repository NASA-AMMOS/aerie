import {Temporal} from '@js-temporal/polyfill';

globalThis.Temporal = Temporal as typeof globalThis.Temporal;
