import type {Pool} from "pg";
import fs from "fs";
import { ErrorWithStatusCode } from "./packages/utils/ErrorWithStatusCode";

export async function getCommandTypes(db: Pool, dictionaryId: string): Promise<string> {
  const sqlExpression = `
    SELECT command_types
    FROM command_dictionary
    WHERE id = $1;
  `;

  const {rowCount, rows} = await db.query(sqlExpression, [
    dictionaryId
  ]);


  const [row] = rows;
  if (rowCount < 0) {
    throw new ErrorWithStatusCode(`No dictionary with id: ${dictionaryId}`, 404);
  }
  return fs.promises.readFile(row.command_types, 'utf8');
}
