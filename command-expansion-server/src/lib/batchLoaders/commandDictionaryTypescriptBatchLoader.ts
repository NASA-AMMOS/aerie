import type {Pool} from 'pg';
import fs from 'fs';
import {ErrorWithStatusCode} from '../../utils/ErrorWithStatusCode.js';
import type {BatchLoader} from "./index.js";


export const commandDictionaryTypescriptBatchLoader: BatchLoader<
  { dictionaryId: number },
  string,
  { db: Pool }
> = opts => async keys => {
  const {rows} = await opts.db.query(`
    SELECT id, command_types
    FROM command_dictionary
    WHERE id IN $1;
  `, [
    keys.map(key => key.dictionaryId)
  ]);

  return Promise.all(keys.map(async ({ dictionaryId }) => {
    const row = rows.find(row => row.id === dictionaryId);
    if (row === undefined) {
      return new ErrorWithStatusCode(`No dictionary with id: ${dictionaryId}`, 404);
    }
    return fs.promises.readFile(row.command_types, 'utf8');
  }));
}
