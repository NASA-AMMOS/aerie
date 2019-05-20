/**
 * Copyright 2019, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Request, Response } from 'express';
import * as t from 'io-ts';
import { PathReporter } from 'io-ts/lib/PathReporter';
import logger from './logger';

type ExpressMiddleware = (
  req: Request,
  res: Response,
  next: () => void,
) => void;

interface Codecs {
  body?: t.Any;
  response?: t.Any;
}

export function validateWithCodecs(codecs: Codecs): ExpressMiddleware {
  return (req: Request, res: Response, next: () => void): void => {
    if (codecs.body) {
      const bodyCodec = codecs.body;
      const decoded = bodyCodec.decode(req.body);

      if (decoded.isRight()) {
        req.body = decoded.value;
        next();
      } else {
        const report = PathReporter.report(decoded);
        const reportMessage = report.length ? report[0] : 'UnknownError';
        const body = JSON.stringify(req.body);
        const message = `Request body validation failed for ${body}: ${reportMessage}`;
        logger.log({ message, level: 'error' });
        res.status(500).send(message);
      }
    }
  };
}
