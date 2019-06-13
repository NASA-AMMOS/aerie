import { Request, Response } from "express";
import EuropaService from "../../services/europa.service";

export class Controller {
  convert(req: Request, res: Response): void {
    EuropaService.convert(req.body.name).then(r => res.status(200).json(r));
  }
}
export default new Controller();
