import { NextFunction, Request, Response } from "express";

export default function errorHandler(
  err: any,
  _req: Request,
  res: Response,
  _next: NextFunction
) {
  res.status(err.status || 500);
  res.send(`${err.status || 500} Error: ${err.message}`);
}
