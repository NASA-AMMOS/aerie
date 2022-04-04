import { expect, test } from '@playwright/test';
import req from '../utilities/requests';

test.describe('Health', () => {
  test('Gateway is healthy', async ({ request }) => {
    const healthy = await req.healthGateway(request);
    expect(healthy).toBeTruthy();
  });

  test('Hasura is healthy', async ({ request }) => {
    const healthy = await req.healthHasura(request);
    expect(healthy).toBeTruthy();
  });

  test('UI is healthy', async ({ request }) => {
    const healthy = await req.healthUI(request);
    expect(healthy).toBeTruthy();
  });
});
