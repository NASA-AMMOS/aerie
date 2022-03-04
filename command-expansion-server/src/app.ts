import express, { Application, Request, Response, NextFunction } from "express";
import bodyParser from "body-parser";

import { getEnv } from "./env.js";
import { DbExpansion } from "./packages/db/db.js";
import * as ampcs from "@nasa-jpl/aerie-ampcs";
import { processDictionary } from "./packages/lib/CommandTypeCodegen.js";
import { ErrorWithStatusCode } from "./utils/ErrorWithStatusCode.js";

const PORT: number = parseInt(getEnv().PORT, 10) ?? 3000;

const app: Application = express();
app.use(bodyParser.json({ limit: '25mb' }));

DbExpansion.init();
const db = DbExpansion.getDb();

app.get("/", (req: Request, res: Response) => {
  res.send("Aerie Command Service");
});

app.post("/dictionary", async (req, res) => {
  const base64Dictionary: string = req.body.input.dictionary;
  const dictionary = Buffer.from(base64Dictionary, "base64").toString("utf8");
  console.log(`Dictionary received`);

  const parsedDictionary = ampcs.parse(dictionary);
  console.log(
    `Dictionary parsed - version: ${parsedDictionary.header.version}, mission: ${parsedDictionary.header.mission_name}`
  );

  const commandDictionaryPath = await processDictionary(parsedDictionary);
  console.log(`command-lib generated - path: ${commandDictionaryPath}`);

  const sqlExpression = `
    insert into command_dictionary (command_types, mission, version)
    values ($1, $2, $3)
    on conflict (mission, version) do update
    set command_types = $1
    returning id;
  `;

  const { rows } = await db.query(sqlExpression, [
    commandDictionaryPath,
    parsedDictionary.header.mission_name,
    parsedDictionary.header.version,
  ]);

  if (rows.length < 0) {
    console.error(`POST /dictionary: No command dictionary was updated in the database`);
    res.status(500).send(`POST /dictionary: No command dictionary was updated in the database`);
    return;
  }
  const id = rows[0].id;
  res.status(200).json({ id });
  return;
});

app.put('/expansion/:activityTypeName', async (req, res) => {
  const expansionLogic = Buffer.from(req.body.input.expansion, 'base64').toString();

  const { rows } = await db.query(`
    INSERT INTO expansion_rules (activity_type, expansion_logic)
    VALUES ($1, $2)
    RETURNING id;
  `, [
    req.params.activityTypeName,
    expansionLogic,
  ]);

  if (rows.length < 1) {
    throw new Error(`PUT /expansion: No expansion was updated in the database`);
  }

  const id = rows[0].id;
  console.log(`PUT /expansion: Updated expansion in the database: id=${id}`);
  res.status(200).json({ id });
  return;
});

app.get('/expansion/:expansionId(\\d+)', async (req, res) => {
  const expansionId = req.params.expansionId;

  const { rows } = await db.query(`
    SELECT expansion_logic
    FROM expansion_rules
    WHERE id = $1;
  `, [
    expansionId,
  ]);

  if (rows.length < 1) {
    throw new ErrorWithStatusCode(`GET /expansion: No expansion with id=${expansionId}`, 404);
  }

  console.log(`GET /expansion: Retrieved expansion from database: id=${expansionId}`);
  res.contentType('text').status(200).send(rows[0].expansion_logic);
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
  console.error(err);
  res.status(err.status ?? err.statusCode ?? 500).send(err.message);
});

app.listen(PORT, () => {
  console.log(`connected to port ${PORT}`);
});
