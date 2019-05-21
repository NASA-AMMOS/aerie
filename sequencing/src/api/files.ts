/**
 * Copyright 2019, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Request, Response } from 'express';
import { omit } from 'lodash';
import v4 from 'uuid/v4';
import { SequenceFile } from '../models';
import { findOne, findToArray, getCollection } from '../util/db';
import logger from '../util/logger';

export async function create(req: Request, res: Response): Promise<void> {
  try {
    const currentUnixTime = Date.now();
    const id = v4();
    const newFile: SequenceFile = {
      ...req.body,
      id,
      timeCreated: currentUnixTime,
      timeLastUpdated: currentUnixTime,
    };

    const collection = getCollection<SequenceFile>('sequencing', 'files');
    const { ops } = await collection.insertOne(newFile);
    const file = omit(ops.pop(), '_id') as SequenceFile;
    logger.info(JSON.stringify(file));
    res.send(file);
  } catch (e) {
    logger.error(e.message);
    res.status(500).send(e);
  }
}

export async function readAll(_: Request, res: Response): Promise<void> {
  try {
    const collection = getCollection<SequenceFile>('sequencing', 'files');
    const result = await findToArray<SequenceFile>(collection);
    const files = result.map(file => omit(file, '_id')) as SequenceFile[];
    logger.info(JSON.stringify(files));
    res.send(files);
  } catch (e) {
    logger.error(e.message);
    res.status(500).send(e);
  }
}

export async function read(req: Request, res: Response): Promise<void> {
  try {
    const { id } = req.params;
    const collection = getCollection<SequenceFile>('sequencing', 'files');
    const result = await findOne<SequenceFile>(collection, { id });

    if (result) {
      const file = omit(result, '_id') as SequenceFile;
      logger.info(JSON.stringify(file));
      res.send(file);
    } else {
      const message = `File With ID ${id} Not Found`;
      logger.info(message);
      res.status(404).send({ message });
    }
  } catch (e) {
    logger.error(e.message);
    res.status(500).send(e);
  }
}

export async function remove(req: Request, res: Response): Promise<void> {
  try {
    const { id } = req.params;
    const collection = getCollection<SequenceFile>('sequencing', 'files');
    const result = await collection.deleteOne({ id });

    logger.info(JSON.stringify(result));
    res.send({ deletedCount: result.deletedCount });
  } catch (e) {
    logger.error(e.message);
    res.status(500).send(e);
  }
}

export async function update(req: Request, res: Response): Promise<void> {
  try {
    const { id } = req.params;
    const currentUnixTime = Date.now();
    const updatedFile: SequenceFile = {
      ...req.body,
      timeLastUpdated: currentUnixTime,
    };

    const collection = getCollection<SequenceFile>('sequencing', 'files');
    const { result } = await collection.updateOne(
      { id },
      { $set: updatedFile },
    );
    const { nModified } = result;

    if (nModified > 0) {
      logger.info(JSON.stringify(result));
      res.status(204).send();
    } else {
      const message = `File With ID ${id} Not Found`;
      logger.info(message);
      res.status(404).send({ message });
    }
  } catch (e) {
    logger.error(e.message);
    res.status(500).send(e);
  }
}
