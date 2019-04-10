/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

const { exec } = require('child_process');

const cwd = process.cwd();

const script = process.argv[2];

const files = process.argv
  .slice(3)
  .map(f => {
    // ng lint's --files argument only works with relative paths
    // strip cwd and leading path delimiter
    return `--files="${f.replace(cwd, '').slice(1)}"`;
  })
  .join(' ');

exec(
  `npm run ${script} -- --tsConfig=tsconfig.json ${files}`,
  (error, stdout) => {
    if (error) {
      console.log(stdout);
      process.exit(1);
    }
  },
);
