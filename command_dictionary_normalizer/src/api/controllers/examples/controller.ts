import { Request, Response } from "express";
import ExamplesService from "../../services/examples.service";

export class Controller {
  all(_req: Request, res: Response): void {
    ExamplesService.all().then(r => res.json(r));
  }

  byId(req: Request, res: Response): void {
    ExamplesService.byId(req.params.id).then(r => {
      if (r) res.json(r);
      else res.status(404).end();
    });
  }

  create(req: Request, res: Response): void {
    ExamplesService.create(req.body.name).then(r =>
      res
        .status(201)
        .location(`/api/examples/${r.id}`)
        .json(r)
    );
  }
}
export default new Controller();
