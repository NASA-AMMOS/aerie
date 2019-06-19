import { Request, Response } from 'express';
import logger from '../../../../../sequencing/src/util/logger';
import EuropaService from '../../services/europa.service';

export class Controller {
  convert(req: Request, res: Response): void {
    EuropaService.convert(req.body).then(r => res.status(200).json(r));

    logger.info('Europa Command Dictionary Normalized Successfully');
  }
}
export default new Controller();
