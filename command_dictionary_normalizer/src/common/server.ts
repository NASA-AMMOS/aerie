import bodyParser from "body-parser";
import express from "express";
import { Application } from "express";
import http from "http";
import path from "path";

import installValidator from "./swagger";

import l from "./logger";

const app = express();

export default class ExpressServer {
  constructor() {
    const root = path.normalize(__dirname + "/../..");
    app.set("appPath", root + "client");
    app.use(bodyParser.json({ limit: process.env.REQUEST_LIMIT || "100kb" }));
    app.use(
      bodyParser.urlencoded({
        extended: true,
        limit: process.env.REQUEST_LIMIT || "100kb"
      })
    );
    app.use(express.static(`${root}/public`));
  }

  router(routes: (app: Application) => void): ExpressServer {
    installValidator(app, routes);
    return this;
  }

  listen(p: string | number = process.env.PORT || 3000): Application {
    const welcome = (port: string | number) => () =>
      l.info(
        `up and running in ${process.env.NODE_ENV ||
          "development"} at: http://localhost:${port}`
      );
    http.createServer(app).listen(p, welcome(p));
    return app;
  }
}
