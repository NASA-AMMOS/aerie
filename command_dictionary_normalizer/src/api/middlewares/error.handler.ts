import { NextFunction, Request, Response } from "express";
import l from "../../common/logger";

export default function errorHandler(
  err: any,
  _req: Request,
  res: Response,
  _next: NextFunction
) {
  const errorMessage = `${err.status || 500} Error: ${err.message}`;

  l.error(errorMessage);
  res.status(err.status || 500);
  res.send(errorMessage);
}
