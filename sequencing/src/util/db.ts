/**
 * Copyright 2019, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Collection, FilterQuery, MongoClient, MongoError } from 'mongodb';
import logger from './logger';

let client: MongoClient | null = null;

type MongoFindQuery =
  | {
      [key: string]: any;
    }
  | {
      [x: string]: any;
    }
  | undefined;

export function findOne<T>(
  collection: Collection,
  filter: FilterQuery<any>,
): Promise<T | null> {
  return new Promise((resolve, reject) => {
    collection.findOne(filter, (error: MongoError, result: T | null) => {
      if (error) {
        logger.error(error);
        reject(error);
      }
      resolve(result);
    });
  });
}

export function findToArray<T>(
  collection: Collection,
  query?: MongoFindQuery,
): Promise<T[]> {
  return new Promise((resolve, reject) => {
    collection.find(query).toArray((error: MongoError, results: T[] = []) => {
      if (error) {
        logger.error(error);
        reject(error);
      }
      resolve(results);
    });
  });
}

export function getCollection<T>(
  dbName: string,
  collectionName: string,
): Collection<T> {
  return (client as MongoClient).db(dbName).collection(collectionName);
}

export function mongoConnect(
  mongoUrl: string,
): Promise<MongoClient | MongoError> {
  return new Promise((resolve, reject) => {
    if (client) {
      resolve(client);
    } else {
      MongoClient.connect(
        mongoUrl,
        { useNewUrlParser: true },
        (e: MongoError, c: MongoClient) => {
          if (e) {
            logger.error(
              `MongoDB connection error. Please make sure MongoDB is running on ${mongoUrl}.`,
            );
            reject(e);
          } else {
            logger.info(`MongoDB connection established on ${mongoUrl}.`);
            client = c;
            resolve(client);
          }
        },
      );
    }
  });
}

export async function mongoDisconnect(): Promise<void> {
  if (client) {
    await client.close();
    client = null;
    logger.info(`MongoDB connection closed.`);
  } else {
    logger.info(`MongoDB cannot disconnect. Client is null.`);
  }
}
