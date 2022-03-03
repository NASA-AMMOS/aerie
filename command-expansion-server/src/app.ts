import fs from 'fs';

import express, {Application, Request, Response} from 'express';
import bodyParser from 'body-parser';
import {GraphQLClient} from 'graphql-request';
import multer from 'multer';
import * as ampcs from '@gov.nasa.jpl.aerie/ampcs';

import {getEnv} from './env.js';
import {DbExpansion} from './packages/db/db.js';
import {processDictionary} from './packages/lib/CommandTypeCodegen.js';
import {getCommandTypes} from './getCommandTypes.js';
import {getActivityTypes} from './getActivityTypes.js';
const app: Application = express();

app.use(bodyParser.json({ limit: "25mb" }));
app.use(bodyParser.urlencoded({ limit: "25mb", extended: true }));
app.use(express.json());

const PORT: number = parseInt(getEnv().PORT, 10) ?? 3000;

DbExpansion.init();
const db = DbExpansion.getDb();
const graphqlClient = new GraphQLClient(getEnv().MERLIN_GRAPHQL_URL);

const upload = multer({ dest: 'uploads/' });

app.get('/', (req: Request, res: Response) => {
  res.send('Aerie Command Service');
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
    const commandTypesPath = await processDictionary(parsedDictionary);
    console.log(`command-lib generated - path: ${commandTypesPath}`);

    const sqlExpression = `
      INSERT INTO command_dictionary (command_types, mission, version)
      VALUES ($1, $2, $3)
      ON CONFLICT (mission, version) DO UPDATE
        SET command_types = $1
      RETURNING id;
    `;

    const {rows} = await db.query(sqlExpression, [
      commandTypesPath,
      parsedDictionary.header.mission_name,
      parsedDictionary.header.version,
    ]);

    if (rows.length < 0) {
      throw new Error(`POST /dictionary: No command dictionary was updated in the database`);
    }
    const id = rows[0];
    res.status(200).json({id});
    return;
});

app.put('/expansion/:activityTypeName', upload.any(), async (req, res) => {
  // Pull existing expanded commands for an activity instance of an expansion run
  res.status(501).send('PUT /expansion: Not implemented');
  return;
});

app.put('/expansion-set', async (req, res) => {
  // Insert an expansion set into the db
  res.status(501).send('PUT /expansion-set: Not implemented');
  return;
});

app.get('/command-types/:dictionaryId', async (req, res) => {
  const commandTypes = await getCommandTypes(db, req.params.dictionaryId);
  res.contentType('text').status(200).send(commandTypes);
  return;
});

app.get('/activity-types/:missionModelId/:activityTypeName', async (req, res) => {
  const activityTypes = await getActivityTypes(graphqlClient, req.params.missionModelId, req.params.activityTypeName);
  res.contentType('text').status(200).send(activityTypes);
  return;
});

app.get('/commands/:expansionRunId/:activityInstanceId', async (req, res) => {
  // Pull existing expanded commands for an activity instance of an expansion run
  res.status(501).send('GET /commands: Not implemented');
  return;
});

app.post('/expand-activity-instance/:simulationId/:expansionConfigId', async (req, res) => {
  // Parallel([
    // Get activity instances from simulation
    // Get expansion config by id
  // ])
  // Get command dictionary id from expansion config
  // Get expansion ids from expansion config
  // Get mission model id from simulation
  // Parallel([
    // Get command types by command dictionary id
    // Get expansions by ids
  // ])
  // For each activity instance in the plan
  //   If the activity instance has an expansion in the expansion config
  //     Get the activity types by mission model id and activity type name
  //     Execute expansion using the command types, activity types, and activity instance
  //     Store resulting commands in db
  res.status(501).send('POST /expand-activity-instance: Not implemented');
  return;
});

app.listen(PORT, () => {
  console.log(`connected to port ${PORT}`);
});
