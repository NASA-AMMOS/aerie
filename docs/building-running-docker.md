# Building and running NEST Docker container

This assumes that NEST has been build by running `npm run build-prod`.
Eventually this should be integrated into the build pipeline.

To build the NEST Docker container run:

```bash
docker build -t nest .
```

To start the NEST Docker container run:

```bash
docker run --name nest -v /tmp/nginx-logs:/var/log/nginx -p 8080:80 nest
```
