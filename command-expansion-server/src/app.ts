import express, { Application, Request, Response } from "express";

import { getEnv } from "./env.js";
import { DbExpansion } from "./packages/db/db.js";

const app: Application = express();
const PORT: number = parseInt(getEnv().PORT, 10) ?? 3000;

DbExpansion.init();
const db = DbExpansion.getDb();

app.get("/", (req: Request, res: Response) => {
  res.send("Aerie Command Service");
});

app.listen(PORT, () => {
  console.log(`connected to port ${PORT}`);
});
