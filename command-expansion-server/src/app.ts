import express, { Application, Request, Response, NextFunction } from "express";
import bodyParser from "body-parser";

import { getEnv } from "./env.js";
import { DbExpansion } from "./packages/db/db.js";
import * as ampcs from "@gov.nasa.jpl.aerie/ampcs";
import { processDictionary } from "./packages/lib/CommandTypeCodegen.js";

const app: Application = express();

app.use(bodyParser.json({ limit: "25mb" }));
app.use(bodyParser.urlencoded({ limit: "25mb", extended: true }));
app.use(express.json());

const PORT: number = parseInt(getEnv().PORT, 10) ?? 3000;

DbExpansion.init();
const db = DbExpansion.getDb();


app.get("/", (req: Request, res: Response) => {
  res.send("Aerie Command Service");
});

app.put("/dictionary", async (req, res) => {
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

  const { rows } = await db.query(sqlExpression, [
    path,
    parsedDictionary.header.mission_name,
    parsedDictionary.header.version,
  ]);



  if (rows.length < 0) {
    console.error(`POST /dictionary: No command dictionary was updated in the database`);
    res.status(500).send(`POST /dictionary: No command dictionary was updated in the database`);
    return;
  }
  const id = rows[0];
  res.status(200).json({ id });
  return;
});

app.put('/expansion/:activityTypeName', async (req, res) => {
  res.status(501).send('PUT /expansion: Not implemented');
  return;
});

app.get('/expansion/:expansionId(\\d+)', async (req, res) => {
  res.status(501).send('GET /expansion: Not implemented');
  return;
});

app.put('/expansion-set', async (req, res) => {
  res.status(501).send('PUT /expansion-set: Not implemented');
    return;
});

app.get('/command-types/:dictionaryId(\\d+)', async (req, res) => {
  res.status(501).send('GET /command-types: Not implemented');
  return;
});

app.get('/activity-types/:missionModelId(\\d+)/:activityTypeName', async (req, res) => {
  res.status(501).send('GET /activity-types: Not implemented');
  return;
});

app.get('/commands/:expansionRunId(\\d+)/:activityInstanceId(\\d+)', async (req, res) => {
  // Pull existing expanded commands for an activity instance of an expansion run
  res.status(501).send('GET /commands: Not implemented');
  return;
});

app.post('/expand-all-activity-instances/:simulationId(\\d+)/:expansionSetId(\\d+)', async (req, res) => {
    res.status(501).send('POST /expand-all-activity-instances: Not implemented');
    return;
});

app.use((err: any, req: Request, res: Response, _next: NextFunction) => {
  console.error(err)
  res.status(err.status ?? err.statusCode ?? 500).send(err.message)
});

app.listen(PORT, () => {
  console.log(`connected to port ${PORT}`);
});
