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

## Windows (Git Bash) Requirements

If building on a Windows machine using Git Bash you will need to first [install chocolatey](https://docs.chocolatey.org/en-us/choco/setup/). Once installed you will need to run:

```
cinst make
cinst rsync
```

After that you can revert to the normal directions.

## Deploying docs

Docs are automatically published on releases. To deploy the docs manually run:

```
git fetch --tags
make deploy
```
