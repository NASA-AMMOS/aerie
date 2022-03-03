import fs from 'fs';

import {Pool} from 'pg';
import {ErrorWithStatusCode} from './utils/ErrorWithStatusCode.js';

export async function getExpansionSet(db: Pool, expansionSetId: number): Promise<{
  commandDictionaryId: number;
  missionModelId: number;
  expansionMap: {[activityTypeName: string]: string},
}> {
  const {rows} = await db.query<{
    command_dict_id: number,
    mission_model_id: number,
    activity_type: string,
    expansion_logic: string,
  }>(`
    SELECT
      expansion_set.command_dict_id,
      expansion_set.mission_model_id,
      expansion_rules.activity_type,
      expansion_rules.expansion_logic
    FROM expansion_set
    INNER JOIN expansion_set_to_rule
      ON expansion_set.id = expansion_set_to_rule.set_id
    INNER JOIN expansion_rules ON expansion_rules.id = expansion_set_to_rule.rule_id
    WHERE expansion_set.id = $1;
  `, [
    expansionSetId,
  ]);

  if (rows.length < 1) {
    throw new ErrorWithStatusCode(`POST /expand-activity-instance: No expansion set with id: ${expansionSetId}`, 404);
  }

  const rowsWithActualExpansionLogic = await Promise.all(rows.map(async (row) => ({
    ...row,
    expansion_logic: await fs.promises.readFile(row.expansion_logic, 'utf-8'),
  })));

  const commandDictionaryId = rowsWithActualExpansionLogic[0].command_dict_id;
  const missionModelId = rowsWithActualExpansionLogic[0].mission_model_id;

  const expansionMap: {[activityTypeName: string]: string} = {};
  for (const row of rowsWithActualExpansionLogic) {
    expansionMap[row.activity_type] = row.expansion_logic;
  }

  return {
    commandDictionaryId,
    missionModelId,
    expansionMap,
  };
}
