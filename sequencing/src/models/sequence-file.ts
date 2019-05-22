/**
 * Copyright 2019, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import * as d from 'decoders';

export const dSequenceFile = d.exact({
  childIds: d.array(d.string),
  content: d.string,
  id: d.string,
  name: d.string,
  timeCreated: d.number,
  timeLastUpdated: d.number,
  type: d.string,
});

export const dSequenceFileCreateBody = d.exact({
  childIds: d.array(d.string),
  content: d.string,
  id: d.optional(d.string),
  name: d.string,
  type: d.string,
});

export const dSequenceFileUpdateBody = d.exact({
  childIds: d.array(d.string),
  content: d.string,
  id: d.string,
  name: d.string,
  timeCreated: d.number,
  timeLastUpdated: d.number,
  type: d.string,
});

export const gSequenceFile = d.guard(dSequenceFile);
export const gSequenceFileCreateBody = d.guard(dSequenceFileCreateBody);
export const gSequenceFileUpdateBody = d.guard(dSequenceFileUpdateBody);

export type SequenceFile = d.$DecoderType<typeof dSequenceFile>;
export type SequenceFileCreateBody = d.$DecoderType<
  typeof dSequenceFileCreateBody
>;
export type SequenceFileUpdateBody = d.$DecoderType<
  typeof dSequenceFileUpdateBody
>;
