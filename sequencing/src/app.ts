/**
 * Copyright 2019, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import bodyParser from 'body-parser';
import cors from 'cors';
import dotenv from 'dotenv';
import express from 'express';
import morgan from 'morgan';
import * as files from './api/files';
import { gSequenceFileCreateBody, gSequenceFileUpdateBody } from './models';
import { WinstonStream } from './util/logger';
import { validateRequestBody } from './util/validators';

dotenv.config({ path: `.env.${process.env.NODE_ENV}` });
const morganFormat = (process.env.MORGAN_FORMAT as string) || 'tiny';

// Create Express App.
const app = express();

// Configuration.
app.set('port', process.env.PORT || 27186);
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(morgan(morganFormat, { stream: new WinstonStream() }));
app.use(cors({ origin: true }));

// Routes.
app.post(
  '/sequencing/files',
  validateRequestBody(gSequenceFileCreateBody),
  files.create,
);
app.get('/sequencing/files/:id', files.read);
app.get('/sequencing/files/:id/children', files.readChildren);
app.get('/sequencing/files', files.readAll);
app.put(
  '/sequencing/files/:id',
  validateRequestBody(gSequenceFileUpdateBody),
  files.update,
);
app.delete('/sequencing/files/:id', files.remove);

export default app;
