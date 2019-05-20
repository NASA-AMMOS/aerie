/**
 * Copyright 2019, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import winston from 'winston';
const { format, transports } = winston;

const env = process.env.NODE_ENV;
const level = env === 'prod' ? 'error' : 'debug';
const silent = env === 'test';
const logger = winston.createLogger({
  format: format.combine(
    format.timestamp({ format: 'YYYY-MM-DD hh:mm:ss.SSS A' }),
    format.colorize(),
    format.simple(),
    format.printf(info => `${info.timestamp} - ${info.level}: ${info.message}`),
  ),
  silent,
  transports: [
    new transports.Console({
      level,
    }),
  ],
});

// Use this to output Morgan logs via Winston.
export class WinstonStream {
  write(message: string): void {
    logger.log({ message, level: 'info' });
  }
}

export default logger;
