import express, { Application, Request, Response } from "express";
import bodyParser from "body-parser";

import { getEnv } from "./env.js";
import { DbExpansion } from "./packages/db/db.js";
import multer from "multer";
import fs from "fs";
import * as ampcs from "@gov.nasa.jpl.aerie/ampcs";
import { processDictionary } from "./packages/lib/CommandTypeCodegen.js";

const app: Application = express();

app.use(bodyParser.json({ limit: "25mb" }));
app.use(bodyParser.urlencoded({ limit: "25mb", extended: true }));
app.use(express.json());

const PORT: number = parseInt(getEnv().PORT, 10) ?? 3000;

DbExpansion.init();
const db = DbExpansion.getDb();

const upload = multer({ dest: "uploads/" });

app.get("/", (req: Request, res: Response) => {
  res.send("Aerie Command Service");
});

app.put("/dictionary", async (req, res) => {
  try {
    let dictionary: string = req.body.input.dictionary;

    // un-stringify the xml
    dictionary = dictionary.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&quot;", '"');
    console.log(`Dictionary received`);
    const parsedDictionary = ampcs.parse(dictionary);
    console.log(
      `Dictionary parsed - version: ${parsedDictionary.header.version}, mission: ${parsedDictionary.header.mission_name}`
    );
    const path = await Promise.resolve(processDictionary(parsedDictionary));
    console.log(`command-lib generated - path: ${path}`);

    const sqlExpression = `
      insert into command_dictionary (command_types, mission, version)
      values ($1, $2, $3)
      on conflict (mission, version) do update
      set command_types = $1
      returning id;
    `;

    const { rowCount, rows } = await db.query(sqlExpression, [
      path,
      parsedDictionary.header.mission_name,
      parsedDictionary.header.version,
    ]);

    const [row] = rows;
    const id = row ? row.id : null;

    if (rowCount > 0) {
      res.status(200).json({
        id: id,
      });
    } else {
      res.status(500).send(`POST /dictionary: No command dictionary was updated in the database`);
    }
  } catch (err) {
    console.log(err);
    res.status(500).send(`POST /dictionary Command Dictionary upload failed: \n${err}`);
  }
});

app.listen(PORT, () => {
  console.log(`connected to port ${PORT}`);
  //main();
});
