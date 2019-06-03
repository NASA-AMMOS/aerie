# How To Run Frontend E2E tests

> End-to-end (E2E) tests ensure that the user interactions are correct. They test solely from the point of view of the user, it doesn't test application state.

## Instructions

You need [Node 10.15.x](https://nodejs.org/en/) and [Docker](https://www.docker.com/) installed before running these commands.

1. Navigate to the root of the `aerie` repo: `cd aerie`
1. Make sure your have the Docker containers for the backend services running.
    - You can use the command: [./scripts/aerie.sh -br](../scripts/aerie.sh)
1. `cd nest`
1. Run `npm install` to install the dependencies for Nest.
1. Run `npm run e2e:build-and-run` to run the e2e tests. Browser windows should appear that show the tests running. Once the tests are done the browsers should close and a report should be displayed in the terminal. Another way to see the results is to look at `/aerie/nest/e2e/results/result.json`

## Uploading Test Results to TestRail

To upload your test results to TestRail you can follow [these instructions](../nest/scripts/testrail/README.md). You don't have to do this if you are just testing the Aerie services against the front-end.
