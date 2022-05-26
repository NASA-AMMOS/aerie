import { config } from 'dotenv';
config();
import fs from 'fs';
import os from 'os';
import path from 'path';
import fetch from 'node-fetch';
import { Writable } from 'stream';
import readline from 'node:readline';

process.env.MERLIN_GATEWAY_URL = 'http://localhost:9000';
process.env.MERLIN_GRAPHQL_URL = 'http://localhost:8080/v1/graphql';

export default async () => {
  const ssoFilePath = `${os.tmpdir()}/.aerie/sso.txt`;
  try {
    const ssoToken = await fs.promises.readFile(ssoFilePath, 'utf8');
    const res = await fetch(process.env.MERLIN_GATEWAY_URL, {
      headers: { 'x-auth-sso-token': ssoToken },
    });
    if (!res.ok) {
      throw new Error(`${res.status} ${res.statusText}`);
    }
    process.env.SSO_TOKEN = ssoToken;
  } catch (e) {
    await fs.promises.rm(ssoFilePath, { force: true });
    const mutableStdout = new Writable({
      write(chunk, encoding, callback) {
        if (!this.muted) process.stdout.write(chunk, encoding);
        callback();
      },
    });
    const readlineInterface = readline.createInterface({
      input: process.stdin,
      output: mutableStdout,
      terminal: true,
    });

    const username = await new Promise(resolve => readlineInterface.question('Enter Username: ', resolve));
    mutableStdout.muted = true;
    process.stdout.write('Enter Password: ');
    const password = await new Promise(resolve => readlineInterface.question('Enter Password: ', resolve));
    readlineInterface.close();
    const response = await fetch(`${process.env.MERLIN_GATEWAY_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username,
        password,
      }),
    });
    if (!response.ok) {
      throw new Error('Login failed');
    }
    const ssoToken = (await response.json())['ssoToken'];
    const res = await fetch(process.env.MERLIN_GATEWAY_URL, {
      headers: { 'x-auth-sso-token': ssoToken },
    });
    if (!res.ok) {
      throw new Error(`${res.status} ${res.statusText}`);
    }
    await fs.promises.mkdir(path.dirname(ssoFilePath), { recursive: true });
    await fs.promises.writeFile(ssoFilePath, ssoToken);
    process.env.SSO_TOKEN = ssoToken;
  }
};
