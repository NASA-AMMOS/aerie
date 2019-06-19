import { Application } from 'express';
import path from 'path';
import middleware from 'swagger-express-middleware';
import errorHandler from '../api/middlewares/error.handler';

export default function(app: Application, routes: (app: Application) => void) {
  middleware(path.join(__dirname, 'api.yml'), app, function(_err, mw) {
    // Enable Express' case-sensitive and strict options
    // (so "/entities", "/Entities", and "/Entities/" are all different)
    app.enable('case sensitive routing');
    app.enable('strict routing');

    app.use(mw.metadata());
    app.use(
      mw.files(app, {
        apiPath: process.env.SWAGGER_API_SPEC,
      }),
    );

    // These two middleware don't have any options (yet)
    app.use(mw.CORS(), mw.validateRequest());

    routes(app);

    // eslint-disable-next-line no-unused-vars, no-shadow
    app.use(errorHandler);
  });
}
