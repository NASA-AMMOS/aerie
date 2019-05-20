/**
 * Copyright 2019, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import dotenv from 'dotenv';
import errorHandler from 'errorhandler';
import ip from 'ip';
import app from './app';
import { mongoConnect } from './util/db';
import logger from './util/logger';

dotenv.config({ path: `.env.${process.env.NODE_ENV}` });
const mongoUrl = process.env.MONGO_URL as string;

async function main() {
  const env = process.env.NODE_ENV;
  const port = app.get('port');

  if (env !== 'prod') {
    // Only use errorhandler in a non-prod environment since it shows full stack traces.
    app.use(errorHandler());
  }

  try {
    await mongoConnect(mongoUrl);
    app.listen(port, () => {
      logger.info(
        `sequencing service is running at http://${ip.address()}:${port} in ${env} mode`,
      );
      logger.info('Press CTRL-C to stop');
    });
  } catch (e) {
    logger.error(e);
  }
}

main();
