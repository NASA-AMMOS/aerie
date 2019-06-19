import bodyParser from 'body-parser';
import express from 'express';
import { Application } from 'express';
import xmlparser from 'express-xml-bodyparser';
import http from 'http';
import path from 'path';
import l from './logger';
import installValidator from './swagger';

const app = express();

export default class ExpressServer {
  constructor() {
    const root = path.normalize(__dirname + '/../..');
    app.set('appPath', root + 'client');
    app.use(bodyParser.json());
    app.use(
      bodyParser.urlencoded({
        extended: true,
      }),
    );
    app.use(xmlparser());
    app.use(express.static(`${root}/public/api-explorer`));
  }

  router(routes: (app: Application) => void): ExpressServer {
    installValidator(app, routes);

    return this;
  }

  listen(p: string | number = process.env.PORT || 3000): Application {
    const welcome = (port: string | number) => () =>
      l.info(
        `Running in ${process.env.NODE_ENV ||
          'development'} at: http://localhost:${port}`,
      );
    http.createServer(app).listen(p, welcome(p));

    return app;
  }
}
