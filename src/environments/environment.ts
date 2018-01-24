// The file contents for the current environment will overwrite these during build.
// The build system defaults to the dev environment which uses `environment.ts`, but if you do
// `ng build --env=prod` then `environment.prod.ts` will be used instead.
// The list of which env maps to which file can be found in `.angular-cli.json`.

export const environment = {
  production: false,
  baseSourcesUrl: 'mpsserver/api/v2/fs',
  baseUrl: 'https://leucadia.jpl.nasa.gov:8443',
  itarMessage: 'This document was reviewed and approved for export, see ATS000. The technical data in this document is controlled under the U.S. Export Regulations; Release to foreign persons may require an export authorization.',
};
