import fs from 'fs';
import util from 'util';

import Ajv from 'ajv';
import express, {Application, Request, Response} from 'express';
import {GraphQLClient} from 'graphql-request';
import multer from 'multer';
import bodyParser from 'body-parser';
import * as ampcs from '@gov.nasa.jpl.aerie/ampcs';

import {getEnv} from './env.js';
import {DbExpansion} from './packages/db/db.js';
import {processDictionary} from './packages/lib/CommandTypeCodegen.js';
import {getCommandTypes} from './getCommandTypes.js';
import {getActivityTypes} from './getActivityTypes.js';
import {ErrorWithStatusCode} from './utils/ErrorWithStatusCode.js';
import {expansionSetSchema} from './packages/schemas/expansion-set.js';
import {findDuplicates} from './utils/findDuplicates.js';

const PORT: number = parseInt(getEnv().PORT, 10) ?? 3000;

const app: Application = express();

app.use(bodyParser.json({ limit: "25mb" }));
app.use(bodyParser.urlencoded({ limit: "25mb", extended: true }));
app.use(express.json());

const ajv = new Ajv();

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

  const {rows} = await db.query<{id: string}>(sqlExpression, [
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
  const [file] = req.files as Express.Multer.File[];

  // TODO: Check that the activity type name is valid with GraphQL API
  console.log(`Expansion logic received`);

  const { rows } = await db.query(`
    INSERT INTO expansion_rules (activity_type, expansion_logic)
    VALUES ($1, $2)
    RETURNING id;
  `, [
    req.params.activityTypeName,
    file.path,
  ]);

  if (rows.length < 1) {
    throw new Error(`PUT /expansion: No expansion was updated in the database`);
  }

  const id = rows[0].id;
  console.log(`PUT /expansion: Updated expansion in the database: id=${id}`);
  res.contentType('json').status(200).send(JSON.stringify({ id }));
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
    throw new ErrorWithStatusCode(`GET /expansion: No expansion with id: ${expansionId}`, 404);
  }

  const expansionLogic = await fs.promises.readFile(rows[0].expansion_logic, 'utf-8');

  console.log(`GET /expansion: Retrieved expansion from database: id=${expansionId}`);
  res.contentType('text').status(200).send(expansionLogic);
  return;
});

const expansionConfigValidator = ajv.compile(expansionSetSchema);
app.put('/expansion-set', async (req, res) => {
  const { body } = req;
  if (!expansionConfigValidator(body)) {
    res.status(400).send(`PUT /expansion-set: Invalid request body: ${JSON.stringify(body)}\n${util.formatWithOptions({ depth: Infinity}, expansionConfigValidator.errors)}`);
    return;
  }
  const commandDictionaryId = body.commandDictionaryId;
  const expansionIds = body.expansionIds;
  const missionModelId = body.missionModelId;

  const { rows: expansionRulesRows } = await db.query(`
    SELECT activity_type, id
    FROM expansion_rules
    WHERE id = ANY($1);
  `, [
      expansionIds,
  ]);

  const duplicates = findDuplicates(expansionRulesRows, expansionRule => expansionRule.activity_type);

  if (duplicates.length > 0) {
    const duplicateStrings: string[] = [];
    const reportedDuplicates = new Set<string>();
    for (const duplicate of duplicates) {
      if (reportedDuplicates.has(duplicate.activity_type)) {
        continue;
      }
      const duplicateIds = duplicates.filter(dupe => dupe.activity_type === duplicate.activity_type).map(dupe => dupe.id)
      duplicateStrings.push(`Duplicate expansion rule for activity type: ${duplicate.activity_type} (expansion ids: ${duplicateIds.join(', ')})`);
      reportedDuplicates.add(duplicate.activity_type);
    }
    res.status(400).send(`PUT /expansion-set:\n\t${duplicateStrings.join('\n\t')}`);
    return;
  }

  const { rows } = await db.query(`
    WITH expansion_set_id AS (
      INSERT INTO expansion_set (command_dict_id, mission_model_id)
      VALUES (${commandDictionaryId}, ${missionModelId})
      RETURNING id
    )
    INSERT INTO expansion_set_to_rule (set_id, rule_id)
    VALUES
      ${expansionIds.map(expansionId => `((SELECT id FROM expansion_set_id), ${expansionId})`).join(',\n      ')}
    RETURNING (SELECT id FROM expansion_set_id);
  `);

  if (rows.length < 1) {
    throw new Error(`PUT /expansion-set: No expansion set was inserted in the database`);
  }
  const id = rows[0].id;
  console.log(`PUT /expansion-set: Updated expansion set in the database: id=${id}`);
  res.contentType('json').status(200).send(JSON.stringify({ id }));
  return;
});

app.get('/command-types/:dictionaryId(\\d+)', async (req, res) => {
  const commandTypes = await getCommandTypes(db, parseInt(req.params.dictionaryId, 10));
  res.contentType('text').status(200).send(commandTypes);
  return;
});

app.get('/activity-types/:missionModelId(\\d+)/:activityTypeName', async (req, res) => {
  const activityTypes = await getActivityTypes(graphqlClient, parseInt(req.params.missionModelId, 10), req.params.activityTypeName);
  res.contentType('text').status(200).send(activityTypes);
  return;
});

app.get('/commands/:expansionRunId(\\d+)/:activityInstanceId(\\d+)', async (req, res) => {
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
