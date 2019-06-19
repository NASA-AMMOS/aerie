import { NextFunction, Request, Response } from 'express';
import logger from '../../../../sequencing/src/util/logger';

export default function errorHandler(
  err: any,
  _req: Request,
  res: Response,
  _next: NextFunction,
) {
  const errorMessage = `${err.status || 500} Error: ${err.message}`;

  logger.error(errorMessage);
  res.status(err.status || 500);
  res.send(errorMessage);
}
