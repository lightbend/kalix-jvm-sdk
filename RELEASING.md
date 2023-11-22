# Releasing Kalix Java SDK

Create a release issue (using the [GitHub CLI](https://cli.github.com/))

```shell
gh issue create --title 'Release Kalix Java/Scala SDKs' --label kalix-runtime --body-file docs/release-issue-template.md -w
````

and follow the instructions.

## Publishing documentation hotfixes

Docs will be published automatically on release. Docs can also be published manually for hotfixes.

The version used in the docs will be the nearest tag. If all doc changes since the last release should be published, run (in the `docs` dir, or with `-C docs`):

```
make deploy
```

If only some doc changes are needed, branch from the last release tag, cherry-pick the needed doc changes, and then run `make deploy`.

This will publish the doc sources to the `docs/current` branch. They will be included automatically in the next build for the main docs. A build for the main docs can also be triggered by re-running the last docs build in CircleCI (on the `master` branch for dev docs, on the `current` branch for prod docs).
