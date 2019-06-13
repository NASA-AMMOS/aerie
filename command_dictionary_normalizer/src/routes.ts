import { Application } from "express";
import europaRouter from "./api/controllers/europa-to-mps/router";

export default function routes(app: Application): void {
  app.use("/api/europa", europaRouter);
}
