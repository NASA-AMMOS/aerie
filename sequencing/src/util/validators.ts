/**
 * Copyright 2019, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import * as d from 'decoders';
import { Request, Response } from 'express';
import logger from './logger';

type ExpressMiddleware = (
  req: Request,
  res: Response,
  next: () => void,
) => void;

export function validateRequestBody<T>(
  bodyGuard: d.Guard<T>,
): ExpressMiddleware {
  return (req: Request, res: Response, next: () => void): void => {
    try {
      const decoded = bodyGuard(req.body);
      req.body = decoded;
      next();
    } catch (e) {
      const message = `Request body validation failed: ${e.message}`;
      logger.log({ message, level: 'error' });
      res.status(500).send(message);
    }
  };
}
