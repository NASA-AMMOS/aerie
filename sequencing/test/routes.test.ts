/**
 * Copyright 2019, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import assert from 'assert';
import { omit } from 'lodash';
import 'mocha';
import { MongoMemoryServer } from 'mongodb-memory-server';
import sinon from 'sinon';
import request from 'supertest';
import app from '../src/app';
import { SequenceFile, SequenceFileCreateBody } from '../src/models';
import * as db from '../src/util/db';
import * as mocks from './mocks';

let mongoServer: MongoMemoryServer;

describe('routes', () => {
  before(async () => {
    mongoServer = new MongoMemoryServer();
    const mongoUrl = await mongoServer.getConnectionString();
    await db.mongoConnect(mongoUrl);
  });

  after(async () => {
    mongoServer.stop();
    await db.mongoDisconnect();
  });

  describe('GET /random-url', () => {
    it('should return 404 for a random URL not defined in the routes', async () => {
      await request(app)
        .get('/random-url')
        .expect(404);
    });
  });

  describe('POST /sequencing/files', () => {
    after(async () => {
      // Delete all files after this test suite.
      const collection = db.getCollection('sequencing', 'files');
      await collection.deleteMany({});
    });

    it('should return 500 when getCollection throws', async () => {
      const newFile: SequenceFileCreateBody = {
        childIds: ['someChildId'],
        content: 'UNSET 0;',
        id: '123456789',
        name: 'seq_42',
        parentId: 'root',
        type: 'file',
      };

      const getCollection = sinon.stub(db, 'getCollection').throws();
      await request(app)
        .post('/sequencing/files')
        .send(newFile)
        .expect(500);
      getCollection.restore();
    });

    it('should return a 500 when the POST body has too few keys', async () => {
      const badPostBody = {
        content: 'stuff',
        name: 'Sequence_0',
      };

      await request(app)
        .post('/sequencing/files')
        .send(badPostBody)
        .expect(500);
    });

    it('should return a 500 when the POST body has too many keys', async () => {
      const badPostBody = {
        childIds: [],
        content: 'SCAN 0;',
        id: 'abc',
        name: 'seq_22',
        someBadExtraKey: 'abc123',
        type: 'file',
      };

      await request(app)
        .post('/sequencing/files')
        .send(badPostBody)
        .expect(500);
    });

    it('should return a 500 when the POST body just provides an id', async () => {
      const badPostBody = {
        id: 'foo',
      };

      await request(app)
        .post('/sequencing/files')
        .send(badPostBody)
        .expect(500);
    });

    it('should return 200 with the new sequence file that was added with a custom id', async () => {
      const newFile = {
        childIds: ['someChildId'],
        content: 'SET 0;',
        id: '42',
        name: 'seq_0',
        parentId: 'root',
        type: 'file',
      };

      const { body } = await request(app)
        .post('/sequencing/files')
        .send(newFile)
        .expect(200);

      assert.deepStrictEqual(body.childIds, newFile.childIds);
      assert.deepStrictEqual(body.content, newFile.content);
      assert.deepStrictEqual(body.id, newFile.id);
      assert.deepStrictEqual(body.name, newFile.name);
      assert.deepStrictEqual(body.parentId, newFile.parentId);
      assert.notStrictEqual(body.timeCreated, undefined);
      assert.notStrictEqual(body.timeLastUpdated, undefined);
      assert.deepStrictEqual(body.type, newFile.type);
    });

    it('should return 200 with the new sequence file that was added with a generated id', async () => {
      const newFile = {
        childIds: ['someChildId'],
        content: 'SET 1;',
        name: 'seq_1',
        parentId: 'root',
        type: 'file',
      };

      const { body } = await request(app)
        .post('/sequencing/files')
        .send(newFile)
        .expect(200);

      assert.deepStrictEqual(body.childIds, newFile.childIds);
      assert.deepStrictEqual(body.content, newFile.content);
      assert.notStrictEqual(body.id, undefined);
      assert.deepStrictEqual(body.name, newFile.name);
      assert.deepStrictEqual(body.parentId, newFile.parentId);
      assert.notStrictEqual(body.timeCreated, undefined);
      assert.notStrictEqual(body.timeLastUpdated, undefined);
      assert.deepStrictEqual(body.type, newFile.type);
    });
  });

  describe('GET /sequencing/files/:id', () => {
    it('should return 500 when getCollection throws', async () => {
      const getCollection = sinon.stub(db, 'getCollection').throws();
      await request(app)
        .get('/sequencing/files/1')
        .expect(500);
      getCollection.restore();
    });

    it('should return a 404 with a status message when the file with the given id is not found', async () => {
      const id = '42';
      await request(app)
        .get(`/sequencing/files/${id}`)
        .expect(404)
        .expect({ message: `File With ID ${id} Not Found` });
    });

    describe('get a file by id', () => {
      let sequenceFile: SequenceFile;

      before(async () => {
        // Setup so we have a file in the database.
        const collection = db.getCollection('sequencing', 'files');
        const { ops } = await collection.insertOne({ ...mocks.file0 });
        sequenceFile = omit(ops.pop(), '_id') as SequenceFile;
      });

      after(async () => {
        // Delete all files after this test suite.
        const collection = db.getCollection('sequencing', 'files');
        await collection.deleteMany({});
      });

      it('should return a 200 and the file when the file with the given id is found', async () => {
        const id = sequenceFile.id;
        await request(app)
          .get(`/sequencing/files/${id}`)
          .expect(200)
          .expect(sequenceFile);
      });
    });
  });

  describe('GET /sequencing/files/:id/children', () => {
    it('should return 500 when getCollection throws', async () => {
      const getCollection = sinon.stub(db, 'getCollection').throws();
      await request(app)
        .get('/sequencing/files/1/children')
        .expect(500);
      getCollection.restore();
    });

    describe('get a files children by id', () => {
      before(async () => {
        // Setup so we have a file in the database.
        const collection = db.getCollection('sequencing', 'files');
        const files = mocks.getFiles();
        await collection.insertMany(files);
      });

      after(async () => {
        // Delete all files after this test suite.
        const collection = db.getCollection('sequencing', 'files');
        await collection.deleteMany({});
      });

      it('should return a 200 and the files children when the given file id is found', async () => {
        const id = 'root';
        await request(app)
          .get(`/sequencing/files/${id}/children`)
          .expect(200)
          .expect([{ ...mocks.file0 }, { ...mocks.file1 }]);
      });
    });
  });

  describe('GET /sequencing/files', () => {
    it('should return 500 when getCollection throws', async () => {
      const getCollection = sinon.stub(db, 'getCollection').throws();
      await request(app)
        .get('/sequencing/files')
        .expect(500);
      getCollection.restore();
    });

    it('should return 500 when findToArray throws', async () => {
      const findToArray = sinon.stub(db, 'findToArray').throws();
      await request(app)
        .get('/sequencing/files')
        .expect(500);
      findToArray.restore();
    });

    it('should return a 200 as an empty array when the files collection is empty', async () => {
      await request(app)
        .get('/sequencing/files')
        .expect(200, []);
    });

    describe('get files when collection is non-empty', () => {
      before(async () => {
        // Setup so we have a file in the database.
        const collection = db.getCollection('sequencing', 'files');
        const files = mocks.getFiles();
        await collection.insertMany(files);
      });

      after(async () => {
        // Delete all files after this test suite.
        const collection = db.getCollection('sequencing', 'files');
        await collection.deleteMany({});
      });

      it('should return a 200 with an array of sequence files', async () => {
        await request(app)
          .get('/sequencing/files')
          .expect(200, mocks.files);
      });

      it('should return a 200 with an array of a single sequence file based on a query string', async () => {
        await request(app)
          .get(`/sequencing/files?id=${mocks.files[0].id}`)
          .expect(200, [mocks.files[0]]);
      });
    });
  });

  describe('PUT /sequencing/files/:id', () => {
    describe('update a single file by id', () => {
      let sequenceFile: SequenceFile;

      before(async () => {
        // Setup so we have a file in the database.
        const collection = db.getCollection('sequencing', 'files');
        const { ops } = await collection.insertOne({ ...mocks.file0 });
        sequenceFile = omit(ops.pop(), '_id') as SequenceFile;
      });

      after(async () => {
        // Delete all files after this test suite.
        const collection = db.getCollection('sequencing', 'files');
        await collection.deleteMany({});
      });

      it('should return 500 when getCollection throws when sending a valid body', async () => {
        const getCollection = sinon.stub(db, 'getCollection').throws();
        await request(app)
          .put('/sequencing/files/1')
          .send({ ...sequenceFile })
          .expect(500);
        getCollection.restore();
      });

      it('should return 500 when sending an empty body', async () => {
        await request(app)
          .put('/sequencing/files/1')
          .expect(500);
      });

      it('should return a 500 when the PUT body has too few keys', async () => {
        const id = sequenceFile.id;
        const newName = 'rocket';

        const badPutBody = {
          id,
          name: newName,
        };

        await request(app)
          .put(`/sequencing/files/${id}`)
          .send(badPutBody)
          .expect(500);
      });

      it('should return a 500 when the PUT body has too many keys', async () => {
        const id = sequenceFile.id;
        const badPutBody = {
          ...sequenceFile,
          someBadExtraKey: '123abc',
        };

        await request(app)
          .put(`/sequencing/files/${id}`)
          .send(badPutBody)
          .expect(500);
      });

      it('should return a 404 when a file with the given ID is not found', async () => {
        const id = '123456789';
        const newName = 'rocket';
        await request(app)
          .put(`/sequencing/files/${id}`)
          .send({
            ...sequenceFile,
            name: newName,
          })
          .expect(404, { message: `File With ID ${id} Not Found` });
      });

      it('should return a 204 when the file with the given id is updated', async () => {
        const id = sequenceFile.id;
        const newName = 'rocket';
        await request(app)
          .put(`/sequencing/files/${id}`)
          .send({
            ...sequenceFile,
            name: newName,
          })
          .expect(204);
      });
    });
  });

  describe('DELETE /sequencing/files/:id', () => {
    it('should return 500 when getCollection throws', async () => {
      const getCollection = sinon.stub(db, 'getCollection').throws();
      await request(app)
        .delete('/sequencing/files/1')
        .expect(500);
      getCollection.restore();
    });

    describe('delete a single file by id', () => {
      let sequenceFile: SequenceFile;

      before(async () => {
        // Setup so we have a file in the database.
        const collection = db.getCollection('sequencing', 'files');
        const { ops } = await collection.insertOne({ ...mocks.file0 });
        sequenceFile = omit(ops.pop(), '_id') as SequenceFile;
      });

      it('should return a 200 when the file with the given id is deleted', async () => {
        const id = sequenceFile.id;
        const res = await request(app)
          .delete(`/sequencing/files/${id}`)
          .expect(200);
        assert.strictEqual(1, res.body.deletedCount);
      });
    });
  });
});
