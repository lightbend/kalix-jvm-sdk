# JVM SDKs docs


## Building docs

To build the docs, run `make` in the `docs` directory:

```
make
```

Dynamically-generated and managed sources will be created in `build/src/managed`.

For quick iteration of changes you can use a partial build:

```
make examples dev-html
```

## Deploying docs

Docs are automatically published on releases. To deploy the docs manually run:

```
git fetch --tags
make deploy
```
