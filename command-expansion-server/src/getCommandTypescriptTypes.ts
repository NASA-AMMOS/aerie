import type {Pool} from 'pg';
import fs from 'fs';
import { ErrorWithStatusCode } from './utils/ErrorWithStatusCode.js';

export async function getCommandTypescriptTypes(db: Pool, dictionaryId: number): Promise<string> {

  const {rowCount, rows} = await db.query(`
    SELECT command_types_typescript_path
    FROM command_dictionary
    WHERE id = $1;
  `, [
    dictionaryId
  ]);


  const [row] = rows;
  if (rowCount < 1) {
    throw new ErrorWithStatusCode(`No dictionary with id: ${dictionaryId}`, 404);
  }
  return fs.promises.readFile(row.command_types_typescript_path, 'utf8');
}
