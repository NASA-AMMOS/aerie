import pino from 'pino';

const l = pino({
  level: process.env.LOG_LEVEL,
  name: process.env.APP_ID,
});

export default l;
