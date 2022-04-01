import { expect, test } from '@playwright/test';
import req from '../utilities/requests';

test.describe('Mission Models', () => {
  let jar_id: number;
  let mission_model_id: number;

  test('Upload mission model .jar file to the gateway', async ({ request }) => {
    jar_id = await req.uploadJarFile(request);
    expect(jar_id).not.toBeNull();
    expect(jar_id).toBeDefined();
    expect(typeof jar_id).toEqual('number');
  });

  test('Create mission model using the jar_id returned from the gateway', async ({ request }) => {
    const model: MissionModelInsertInput = {
      jar_id,
      mission: 'aerie_e2e_tests',
      name: 'Banananation (e2e tests)',
      version: '0.0.0',
    };
    mission_model_id = await req.createMissionModel(request, model);
    expect(mission_model_id).not.toBeNull();
    expect(mission_model_id).toBeDefined();
    expect(typeof mission_model_id).toEqual('number');
  });

  test('Delete mission model', async ({ request }) => {
    const deleted_mission_model_id = await req.deleteMissionModel(request, mission_model_id);
    expect(deleted_mission_model_id).toEqual(mission_model_id);
    expect(deleted_mission_model_id).not.toBeNull();
    expect(deleted_mission_model_id).toBeDefined();
    expect(typeof deleted_mission_model_id).toEqual('number');
  });
});
