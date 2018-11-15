# Fetching and building NEST

For now the NEST codebase is sourced from the raven2 repository. It can be
manually updated with the `sync_repo.sh` script in the scripts directory:

```bash
./scripts/sync_repo.sh -r git@github.jpl.nasa.gov:MPS/raven2.git -b develop -d ./nest/
```

This is a **ONE-WAY SYNCRONIZATION**, so it is very important that the files in
the nest directory are not edited by hand. Any changes that are made will be
completely removed the next time the `sync_repo.sh` script is run.

After this procedure is complete, follow the instructions in the fetched README
to build the project.
